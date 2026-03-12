package com.yuan.monitor.collector;

import com.yuan.monitor.vo.SystemMetricVO;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    // CPU 快照：保存上一次的 tick 数据
    private final AtomicReference<long[]> prevCpuTicks = new AtomicReference<>(PROCESSOR.getSystemCpuLoadTicks());
    // 网络快照：保存上一次的流量数据和时间戳
    private final AtomicReference<NetworkSnapshot> prevNetworkSnapshot = new AtomicReference<>();

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

        // 1. CPU 使用率（OSHI 实现，更稳定）
        metric.setCpu(getCpuUsage());

        // 2. 内存使用率（OSHI 实现）
        metric.setMemory(getMemoryUsage());

        // 3. 磁盘使用率（JDK FileStore 实现，保持不变）
        metric.setDisk(getDiskUsage(diskPath));

        // 4. 网络出入流量（OSHI 实现，实时速率 MB/s）
        NetworkRate networkRate = getNetworkRate();
        metric.setNetworkIn(networkRate.inRate);
        metric.setNetworkOut(networkRate.outRate);

        return metric;
    }

    // ====================== 1. CPU 使用率（OSHI 实现） ======================
    private BigDecimal getCpuUsage() {
        long[] prevTicks = prevCpuTicks.get();
        long[] newTicks = PROCESSOR.getSystemCpuLoadTicks();

        // 计算两次采集之间的 CPU 使用率
        double cpuLoad = PROCESSOR.getSystemCpuLoadBetweenTicks(prevTicks, newTicks) * 100;

        // 更新快照
        prevCpuTicks.set(newTicks);

        // 保留 1 位小数
        return BigDecimal.valueOf(cpuLoad).setScale(1, RoundingMode.HALF_UP);
    }

    // ====================== 2. 内存使用率（OSHI 实现） ======================
    private BigDecimal getMemoryUsage() {
        long total = MEMORY.getTotal();
        long available = MEMORY.getAvailable(); // 实际可用内存（包含缓存）

        // 计算公式：(总内存 - 可用内存) / 总内存 * 100
        BigDecimal usage = BigDecimal.valueOf(total - available)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);

        return usage;
    }

    // ====================== 3. 磁盘使用率（JDK FileStore 实现） ======================
    private BigDecimal getDiskUsage(String path) {
        try {
            Path targetPath = Paths.get(path);
            FileStore fileStore = Files.getFileStore(targetPath);

            long total = fileStore.getTotalSpace();
            long usable = fileStore.getUsableSpace(); // 实际可用空间（考虑权限）
            long used = total - usable;

            return BigDecimal.valueOf(used)
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    // ====================== 4. 网络出入流量（OSHI 实现） ======================
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

        // 获取所有网卡的累计流量（过滤掉虚拟网卡）
        List<NetworkIF> networkIFs = SYSTEM_INFO.getHardware().getNetworkIFs();
        for (NetworkIF networkIF : networkIFs) {
            networkIF.updateAttributes();
            // 简单过滤：只监控有流量的物理网卡（可选）
            if (networkIF.getBytesRecv() > 0 || networkIF.getBytesSent() > 0) {
                currentBytesRecv += networkIF.getBytesRecv();
                currentBytesSent += networkIF.getBytesSent();
            }
        }

        // 初始化快照（第一次调用）
        NetworkSnapshot prev = prevNetworkSnapshot.get();
        if (prev == null) {
            prevNetworkSnapshot.set(new NetworkSnapshot(currentTime, currentBytesRecv, currentBytesSent));
            return new NetworkRate(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // 计算时间差和流量差
        long timeDiffMs = currentTime - prev.timestamp;
        if (timeDiffMs <= 0) {
            return new NetworkRate(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long bytesRecvDiff = currentBytesRecv - prev.bytesRecv;
        long bytesSentDiff = currentBytesSent - prev.bytesSent;

        // 计算实时速率（MB/s）
        double timeDiffSec = timeDiffMs / 1000.0;
        BigDecimal inRate = bytesToMBPerSec(bytesRecvDiff, timeDiffSec);
        BigDecimal outRate = bytesToMBPerSec(bytesSentDiff, timeDiffSec);

        // 更新快照
        prevNetworkSnapshot.set(new NetworkSnapshot(currentTime, currentBytesRecv, currentBytesSent));

        return new NetworkRate(inRate, outRate);
    }

    // 辅助方法：字节转 MB/s
    private BigDecimal bytesToMBPerSec(long bytes, double seconds) {
        if (seconds <= 0 || bytes <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(bytes)
                .divide(BigDecimal.valueOf(seconds), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(1024 * 1024), 2, RoundingMode.HALF_UP);
    }
}