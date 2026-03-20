package com.yuan.monitor.collector;

import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.vo.SystemMetricVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class SystemMetricSecondBucket {
    private final LocalDateTime ts;
    private final ConcurrentHashMap<Long, RunBinding> runBindings = new ConcurrentHashMap<>();

    private BigDecimal sumCpu = BigDecimal.ZERO;
    private BigDecimal sumMemory = BigDecimal.ZERO;
    private BigDecimal sumDisk = BigDecimal.ZERO;
    private BigDecimal sumNetworkIn = BigDecimal.ZERO;
    private BigDecimal sumNetworkOut = BigDecimal.ZERO;

    private BigDecimal maxCpu;
    private BigDecimal maxMemory;

    private int sampleCount;

    public SystemMetricSecondBucket(LocalDateTime ts) {
        this.ts = ts;
    }

    public void registerRuns(Collection<TestStatusRecord> activeRuns) {
        if (activeRuns == null) {
            return;
        }
        for (TestStatusRecord activeRun : activeRuns) {
            if (activeRun == null || activeRun.getRunId() == null || activeRun.getTaskId() == null) {
                continue;
            }
            runBindings.put(activeRun.getRunId(), new RunBinding(activeRun.getUserId(), activeRun.getTaskId(), activeRun.getRunId()));
        }
    }

    public synchronized void addSample(SystemMetricVO metric) {
        if (metric == null) {
            return;
        }

        sumCpu = sumCpu.add(zero(metric.getCpu()));
        sumMemory = sumMemory.add(zero(metric.getMemory()));
        sumDisk = sumDisk.add(zero(metric.getDisk()));
        sumNetworkIn = sumNetworkIn.add(zero(metric.getNetworkIn()));
        sumNetworkOut = sumNetworkOut.add(zero(metric.getNetworkOut()));

        maxCpu = max(maxCpu, metric.getCpu());
        maxMemory = max(maxMemory, metric.getMemory());
        sampleCount++;
    }

    public synchronized BucketSnapshot snapshot() {
        if (sampleCount <= 0) {
            return new BucketSnapshot(null, null, null, null, null, null, null, 0, true);
        }

        BigDecimal divisor = BigDecimal.valueOf(sampleCount);
        return new BucketSnapshot(
                sumCpu.divide(divisor, 2, RoundingMode.HALF_UP),
                sumMemory.divide(divisor, 2, RoundingMode.HALF_UP),
                sumDisk.divide(divisor, 2, RoundingMode.HALF_UP),
                sumNetworkIn.divide(divisor, 2, RoundingMode.HALF_UP),
                sumNetworkOut.divide(divisor, 2, RoundingMode.HALF_UP),
                maxCpu,
                maxMemory,
                sampleCount,
                false
        );
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal max(BigDecimal currentMax, BigDecimal candidate) {
        if (candidate == null) {
            return currentMax;
        }
        if (currentMax == null || candidate.compareTo(currentMax) > 0) {
            return candidate;
        }
        return currentMax;
    }

    @Getter
    @AllArgsConstructor
    public static class RunBinding {
        private Long userId;
        private Long taskId;
        private Long runId;
    }

    @Getter
    @AllArgsConstructor
    public static class BucketSnapshot {
        private BigDecimal avgCpu;
        private BigDecimal avgMemory;
        private BigDecimal avgDisk;
        private BigDecimal avgNetworkIn;
        private BigDecimal avgNetworkOut;
        private BigDecimal maxCpu;
        private BigDecimal maxMemory;
        private Integer sampleCount;
        private Boolean missing;

        public SystemMetricVO toSystemMetricVO() {
            SystemMetricVO metric = new SystemMetricVO();
            metric.setCpu(avgCpu == null ? BigDecimal.ZERO : avgCpu);
            metric.setMemory(avgMemory == null ? BigDecimal.ZERO : avgMemory);
            metric.setDisk(avgDisk == null ? BigDecimal.ZERO : avgDisk);
            metric.setNetworkIn(avgNetworkIn == null ? BigDecimal.ZERO : avgNetworkIn);
            metric.setNetworkOut(avgNetworkOut == null ? BigDecimal.ZERO : avgNetworkOut);
            return metric;
        }
    }
}
