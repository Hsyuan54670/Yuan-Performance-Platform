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

    // 前端展示轮询和后台高频采样如果共用同一组快照，会互相覆盖 CPU / 网络 / 磁盘的“上一帧”。
    // 这里显式拆成两套上下文，避免 HTTP 查询刚好踩在采样任务后面时把瞬时区间算成 0。
    private final MetricProbe displayProbe = new MetricProbe();
    private final MetricProbe samplingProbe = new MetricProbe();

    // ====================== 状态快照（保证线程安全） ======================
    private static class MetricProbe {
        final AtomicReference<long[]> prevCpuTicks = new AtomicReference<>(PROCESSOR.getSystemCpuLoadTicks());
        final AtomicReference<NetworkSnapshot> prevNetworkSnapshot = new AtomicReference<>();
        final AtomicReference<DiskSnapshot> prevDiskSnapshot = new AtomicReference<>();
    }

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

    private static class DiskSnapshot {
        final long timestamp;
        final long transferTime;

        DiskSnapshot(long timestamp, long transferTime) {
            this.timestamp = timestamp;
            this.transferTime = transferTime;
        }
    }

    private static class NetworkRate {
        final BigDecimal inRate;
        final BigDecimal outRate;

        NetworkRate(BigDecimal inRate, BigDecimal outRate) {
            this.inRate = inRate;
            this.outRate = outRate;
        }
    }

    // ====================== 核心采集入口 ======================
    public SystemMetricVO collectCurrentSystemMetrics() {
        return collectCurrentSystemMetricsForDisplay();
    }

    public SystemMetricVO collectCurrentSystemMetricsForDisplay() {
        return collectCurrentSystemMetrics(displayProbe, defaultDiskPath());
    }

    public SystemMetricVO collectCurrentSystemMetricsForSampling() {
        return collectCurrentSystemMetrics(samplingProbe, defaultDiskPath());
    }

    /**
     * 重载方法：指定监控的磁盘路径
     * @param diskPath 磁盘路径（如 "E:/jmeter/results"）
     */
    public SystemMetricVO collectCurrentSystemMetrics(String diskPath) {
        return collectCurrentSystemMetrics(displayProbe, diskPath);
    }

    public SystemMetricVO collectCurrentSystemMetricsForSampling(String diskPath) {
        return collectCurrentSystemMetrics(samplingProbe, diskPath);
    }

    private String defaultDiskPath() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? "C:/" : "/";
    }

    private SystemMetricVO collectCurrentSystemMetrics(MetricProbe probe, String diskPath) {
        SystemMetricVO metric = new SystemMetricVO();
        metric.setCpu(getCpuUsage(probe));
        metric.setMemory(getMemoryUsage());
        metric.setDisk(getDiskUsage(probe, diskPath));

        NetworkRate networkRate = getNetworkRate(probe);
        metric.setNetworkIn(networkRate.inRate);
        metric.setNetworkOut(networkRate.outRate);
        return metric;
    }

    // ====================== 1. CPU 使用率 ======================
    private BigDecimal getCpuUsage(MetricProbe probe) {
        long[] prevTicks = probe.prevCpuTicks.get();
        long[] newTicks = PROCESSOR.getSystemCpuLoadTicks();

        double cpuLoad = PROCESSOR.getSystemCpuLoadBetweenTicks(prevTicks, newTicks) * 100;
        probe.prevCpuTicks.set(newTicks);

        return BigDecimal.valueOf(cpuLoad).setScale(1, RoundingMode.HALF_UP);
    }

    // ====================== 2. 内存使用率 ======================
    private BigDecimal getMemoryUsage() {
        long total = MEMORY.getTotal();
        long available = MEMORY.getAvailable();

        return BigDecimal.valueOf(total - available)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    // ====================== 3. 磁盘 IO 使用率  ======================
    private BigDecimal getDiskUsage(MetricProbe probe, String path) {
        long currentTime = System.currentTimeMillis();
        long currentTransferTime = 0;

        List<HWDiskStore> diskStores = SYSTEM_INFO.getHardware().getDiskStores();
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes();
            currentTransferTime += disk.getTransferTime();
        }

        DiskSnapshot prev = probe.prevDiskSnapshot.get();
        if (prev == null) {
            probe.prevDiskSnapshot.set(new DiskSnapshot(currentTime, currentTransferTime));
            return BigDecimal.ZERO;
        }

        long timeDelta = currentTime - prev.timestamp;
        long ioTimeDelta = currentTransferTime - prev.transferTime;
        probe.prevDiskSnapshot.set(new DiskSnapshot(currentTime, currentTransferTime));

        if (timeDelta <= 0 || ioTimeDelta <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(ioTimeDelta)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(timeDelta), 1, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
    }

    // ====================== 4. 网络出入流量 ======================
    private NetworkRate getNetworkRate(MetricProbe probe) {
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

        NetworkSnapshot prev = probe.prevNetworkSnapshot.get();
        if (prev == null) {
            probe.prevNetworkSnapshot.set(new NetworkSnapshot(currentTime, currentBytesRecv, currentBytesSent));
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

        probe.prevNetworkSnapshot.set(new NetworkSnapshot(currentTime, currentBytesRecv, currentBytesSent));
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