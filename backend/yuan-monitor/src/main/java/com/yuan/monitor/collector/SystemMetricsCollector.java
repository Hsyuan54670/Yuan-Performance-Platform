package com.yuan.monitor.collector;

import com.yuan.monitor.vo.SystemMetricVO;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/*
 * 系统指标采集器：使用 OSHI 库获取 CPU、内存、网络等系统级指标
 * auth: dou_bao
 * */
@Component
public class SystemMetricsCollector {

    // ====================== OSHI 核心对象初始化 ======================
    private static final SystemInfo SYSTEM_INFO = new SystemInfo();
    private static final CentralProcessor PROCESSOR = SYSTEM_INFO.getHardware().getProcessor();
    private static final GlobalMemory MEMORY = SYSTEM_INFO.getHardware().getMemory();

    // ====================== 状态快照（保证线程安全） ======================
    // CPU 快照
    private final AtomicReference<long[]> prevCpuTicks = new AtomicReference<>(PROCESSOR.getSystemCpuLoadTicks());
    // 网络快照
    private final AtomicReference<NetworkSnapshot> prevNetworkSnapshot = new AtomicReference<>();
    // 磁盘 IO 快照 (新增)
    private final AtomicReference<DiskSnapshot> prevDiskSnapshot = new AtomicReference<>();

    // 内部类：网络快照
    private static class NetworkSnapshot {
        final long timestamp;
        final long bytesRecv;
        final long bytesSent;

        NetworkSnapshot(long timestamp, long bytesRecv, long bytesSent) {
            this.timestamp = timestamp;
            this.bytesRecv = bytesRecv;
            this.bytesSent = bytesSent;
        }
    }

    // 内部类：磁盘 IO 快照 (新增)
    private static class DiskSnapshot {
        final long timestamp;
        final long transferTime; // 磁盘 IO 繁忙的累计时间 (ms)

        DiskSnapshot(long timestamp, long transferTime) {
            this.timestamp = timestamp;
            this.transferTime = transferTime;
        }
    }

    // ====================== 核心采集入口 ======================
    public SystemMetricVO collectCurrentSystemMetrics() {
        // 默认监控系统盘（Windows C:，Linux /），可改为你的压测结果目录
        return collectCurrentSystemMetrics(System.getProperty("os.name").toLowerCase().contains("win") ? "C:/" : "/");
    }

    /**
     * 重载方法：指定监控的磁盘路径
     * @param diskPath 磁盘路径（如 "E:/jmeter/results"）
     */
    public SystemMetricVO collectCurrentSystemMetrics(String diskPath) {
        SystemMetricVO metric = new SystemMetricVO();

        // 1. CPU 使用率
        metric.setCpu(getCpuUsage());

        // 2. 内存使用率
        metric.setMemory(getMemoryUsage());

        // 3. 磁盘 IO 使用率 (注意：这里传 path 目前仅用于兼容接口，逻辑是统计所有物理磁盘的总 IO)
        metric.setDisk(getDiskUsage(diskPath));

        // 4. 网络出入流量
        NetworkRate networkRate = getNetworkRate();
        metric.setNetworkIn(networkRate.inRate);
        metric.setNetworkOut(networkRate.outRate);

        return metric;
    }

    // ====================== 1. CPU 使用率 ======================
    private BigDecimal getCpuUsage() {
        long[] prevTicks = prevCpuTicks.get();
        long[] newTicks = PROCESSOR.getSystemCpuLoadTicks();

        double cpuLoad = PROCESSOR.getSystemCpuLoadBetweenTicks(prevTicks, newTicks) * 100;
        prevCpuTicks.set(newTicks);

        return BigDecimal.valueOf(cpuLoad).setScale(1, RoundingMode.HALF_UP);
    }

    // ====================== 2. 内存使用率 ======================
    private BigDecimal getMemoryUsage() {
        long total = MEMORY.getTotal();
        long available = MEMORY.getAvailable();

        BigDecimal usage = BigDecimal.valueOf(total - available)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);

        return usage;
    }

    // ====================== 3. 磁盘 IO 使用率  ======================

    /**
     * 获取磁盘 IO 使用率
     * 注意：这里统计的是所有物理磁盘的总体 IO 繁忙程度 (Utilization)
     * 范围：0 - 100
     */
    private BigDecimal getDiskUsage(String path) {
        long currentTime = System.currentTimeMillis();
        long currentTransferTime = 0;

        // 1. 获取所有物理磁盘并累加它们的 IO 繁忙时间
        List<HWDiskStore> diskStores = SYSTEM_INFO.getHardware().getDiskStores();
        for (HWDiskStore disk : diskStores) {
            // 更新磁盘属性以获取最新数据
            disk.updateAttributes();
            // getTransferTime(): 操作系统记录的磁盘忙于读写的总毫秒数
            currentTransferTime += disk.getTransferTime();
        }

        // 2. 获取上一次快照
        DiskSnapshot prev = prevDiskSnapshot.get();
        if (prev == null) {
            // 第一次调用，初始化快照并返回 0
            prevDiskSnapshot.set(new DiskSnapshot(currentTime, currentTransferTime));
            return BigDecimal.ZERO;
        }

        // 3. 计算差值
        long timeDelta = currentTime - prev.timestamp;
        long ioTimeDelta = currentTransferTime - prev.transferTime;

        // 4. 更新快照
        prevDiskSnapshot.set(new DiskSnapshot(currentTime, currentTransferTime));

        // 5. 边界检查
        if (timeDelta <= 0 || ioTimeDelta <= 0) {
            return BigDecimal.ZERO;
        }

        // 6. 计算使用率：(IO 繁忙时间差 / 经过的时间差) * 100
        // 因为 ioTimeDelta 是所有磁盘的总和，如果有多个磁盘，使用率可能超过 100%，
        // 通常性能测试中我们看单盘或者取最大值，这里为了简单演示，取 min(100) 或者展示实际值
        return BigDecimal.valueOf(ioTimeDelta)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(timeDelta), 1, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100)); // 限制最大显示 100%
    }

    // ====================== 4. 网络出入流量 ======================
    private static class NetworkRate {
        final BigDecimal inRate;
        final BigDecimal outRate;

        NetworkRate(BigDecimal inRate, BigDecimal outRate) {
            this.inRate = inRate;
            this.outRate = outRate;
        }
    }

    private NetworkRate getNetworkRate() {
        long currentTime = System.currentTimeMillis();
        long currentBytesRecv = 0;
        long currentBytesSent = 0;

        List<NetworkIF> networkIFs = SYSTEM_INFO.getHardware().getNetworkIFs();
        for (NetworkIF networkIF : networkIFs) {
            networkIF.updateAttributes();
            if (networkIF.getBytesRecv() > 0 || networkIF.getBytesSent() > 0) {
                currentBytesRecv += networkIF.getBytesRecv();
                currentBytesSent += networkIF.getBytesSent();
            }
        }

        NetworkSnapshot prev = prevNetworkSnapshot.get();
        if (prev == null) {
            prevNetworkSnapshot.set(new NetworkSnapshot(currentTime, currentBytesRecv, currentBytesSent));
            return new NetworkRate(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long timeDiffMs = currentTime - prev.timestamp;
        if (timeDiffMs <= 0) {
            return new NetworkRate(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long bytesRecvDiff = currentBytesRecv - prev.bytesRecv;
        long bytesSentDiff = currentBytesSent - prev.bytesSent;

        double timeDiffSec = timeDiffMs / 1000.0;
        BigDecimal inRate = bytesToMBPerSec(bytesRecvDiff, timeDiffSec);
        BigDecimal outRate = bytesToMBPerSec(bytesSentDiff, timeDiffSec);

        prevNetworkSnapshot.set(new NetworkSnapshot(currentTime, currentBytesRecv, currentBytesSent));

        return new NetworkRate(inRate, outRate);
    }

    private BigDecimal bytesToMBPerSec(long bytes, double seconds) {
        if (seconds <= 0 || bytes <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(bytes)
                .divide(BigDecimal.valueOf(seconds), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(1024 * 1024), 2, RoundingMode.HALF_UP);
    }
}