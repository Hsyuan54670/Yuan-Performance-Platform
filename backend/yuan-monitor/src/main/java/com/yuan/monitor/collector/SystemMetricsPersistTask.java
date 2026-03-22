package com.yuan.monitor.collector;

import com.yuan.monitor.entity.SystemMetricsRecord;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.mapper.SystemMetricsMapper;
import com.yuan.monitor.runtime.ActiveRunRegistry;
import com.yuan.monitor.service.AlertEvaluateService;
import com.yuan.monitor.vo.SystemMetricVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class SystemMetricsPersistTask {
    @Autowired
    private SystemMetricsCollector systemMetricsCollector;

    @Autowired
    private SystemMetricsMapper systemMetricsMapper;

    @Autowired
    private AlertEvaluateService alertEvaluateService;

    @Autowired
    private ActiveRunRegistry activeRunRegistry;

    // 秒桶按自然秒聚合高频样本，避免 fixedDelay 直接落库导致秒点漂移和缺失。
    private final ConcurrentNavigableMap<LocalDateTime, SystemMetricSecondBucket> buckets = new ConcurrentSkipListMap<>();
    private final AtomicReference<LocalDateTime> lastHeartbeatSecond = new AtomicReference<>();

    // 每秒打一次“心跳桶”，哪怕这一秒没有采到资源样本，也会留下 missing 桶而不是直接丢秒。
    @Scheduled(fixedRateString = "${yuan.monitor.system-metrics.heartbeat-interval-ms:1000}")
    public void registerCurrentSecondBucket() {
        LocalDateTime currentSecond = LocalDateTime.now().withNano(0);
        LocalDateTime previousSecond = lastHeartbeatSecond.getAndSet(currentSecond);

        List<TestStatusRecord> trackableRuns = activeRunRegistry.listTrackableRuns();
        if (trackableRuns.isEmpty()) {
            return;
        }

        if (previousSecond == null || !previousSecond.isBefore(currentSecond)) {
            ensureBucket(currentSecond, trackableRuns);
            return;
        }

        LocalDateTime cursor = previousSecond.plusSeconds(1);
        while (!cursor.isAfter(currentSecond)) {
            ensureBucket(cursor, trackableRuns);
            cursor = cursor.plusSeconds(1);
        }
    }

    // 高频采样负责把瞬时资源数据打进当前秒桶，CPU 语义会变成“采样窗口平均”，而不是调度间隔平均。
    @Scheduled(fixedRateString = "${yuan.monitor.system-metrics.sample-interval-ms:200}")
    public void sampleCurrentSystemMetrics() {
        List<TestStatusRecord> trackableRuns = activeRunRegistry.listTrackableRuns();
        if (trackableRuns.isEmpty()) {
            return;
        }

        SystemMetricVO metric = systemMetricsCollector.collectCurrentSystemMetricsForSampling();
        LocalDateTime currentSecond = LocalDateTime.now().withNano(0);
        SystemMetricSecondBucket bucket = buckets.computeIfAbsent(currentSecond, SystemMetricSecondBucket::new);
        bucket.registerRuns(trackableRuns);
        bucket.addSample(metric);
    }

    // 状态变更时立刻把当前秒挂上 run 绑定，减少短压测首尾边界秒被调度错过的概率。
    public void trackRunAtCurrentSecond(TestStatusRecord record) {
        if (record == null || record.getTaskId() == null || record.getRunId() == null) {
            return;
        }
        ensureBucket(LocalDateTime.now().withNano(0), List.of(record));
    }

    // 已完成的秒桶统一落库，每条记录都显式带上 sampleCount / missing / cpuMax / memoryMax。
    @Scheduled(fixedRateString = "${yuan.monitor.system-metrics.flush-interval-ms:500}")
    public void flushCompletedBuckets() {
        LocalDateTime currentSecond = LocalDateTime.now().withNano(0);
        List<LocalDateTime> completedSeconds = new ArrayList<>(buckets.headMap(currentSecond, false).keySet());
        for (LocalDateTime bucketTime : completedSeconds) {
            SystemMetricSecondBucket bucket = buckets.remove(bucketTime);
            if (bucket == null || bucket.getRunBindings().isEmpty()) {
                continue;
            }

            SystemMetricSecondBucket.BucketSnapshot snapshot = bucket.snapshot();
            for (SystemMetricSecondBucket.RunBinding runBinding : bucket.getRunBindings().values()) {
                if (runBinding.getTaskId() == null || runBinding.getRunId() == null) {
                    continue;
                }

                SystemMetricsRecord record = new SystemMetricsRecord();
                record.setTaskId(runBinding.getTaskId());
                record.setRunId(runBinding.getRunId());
                record.setTs(bucketTime);
                record.setCpu(snapshot.getAvgCpu());
                record.setCpuMax(snapshot.getMaxCpu());
                record.setMemory(snapshot.getAvgMemory());
                record.setMemoryMax(snapshot.getMaxMemory());
                record.setDisk(snapshot.getAvgDisk());
                record.setNetworkIn(snapshot.getAvgNetworkIn());
                record.setNetworkOut(snapshot.getAvgNetworkOut());
                record.setSampleCount(snapshot.getSampleCount());
                record.setMissing(snapshot.getMissing());
                systemMetricsMapper.insert(record);

                if (Boolean.TRUE.equals(snapshot.getMissing()) || runBinding.getUserId() == null) {
                    continue;
                }

                TestStatusRecord runningContext = new TestStatusRecord();
                runningContext.setUserId(runBinding.getUserId());
                runningContext.setTaskId(runBinding.getTaskId());
                runningContext.setRunId(runBinding.getRunId());
                runningContext.setStatus("RUNNING");
                runningContext.setCreatedAt(bucketTime);
                try {
                    alertEvaluateService.evaluateSystemMetric(runningContext, snapshot.toSystemMetricVO());
                } catch (Exception e) {
                    log.error("Failed to evaluate aggregated system metric alerts, runId={}, taskId={}, ts={}",
                            runBinding.getRunId(), runBinding.getTaskId(), bucketTime, e);
                }
            }
        }
    }

    private void ensureBucket(LocalDateTime bucketTime, List<TestStatusRecord> activeRuns) {
        SystemMetricSecondBucket bucket = buckets.computeIfAbsent(bucketTime, SystemMetricSecondBucket::new);
        bucket.registerRuns(activeRuns);
    }
}
