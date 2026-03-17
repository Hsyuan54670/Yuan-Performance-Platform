package com.yuan.monitor.collector;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.monitor.entity.SystemMetricsRecord;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.mapper.SystemMetricsMapper;
import com.yuan.monitor.mapper.TestStatusRecordMapper;
import com.yuan.monitor.service.AlertEvaluateService;
import com.yuan.monitor.vo.SystemMetricVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SystemMetricsPersistTask {
    @Autowired
    private SystemMetricsCollector systemMetricsCollector;

    @Autowired
    private SystemMetricsMapper systemMetricsMapper;

    @Autowired
    private TestStatusRecordMapper testStatusRecordMapper;

    @Autowired
    private AlertEvaluateService alertEvaluateService;

    // 每秒采一份宿主机指标，并按 runId 复制写入当前所有运行中的任务，便于 analysis 按运行维度聚合。
    @Scheduled(fixedDelay = 1000)
    public void persistCurrentSystemMetrics() {
        List<TestStatusRecord> activeRuns = listActiveRuns();
        if (activeRuns.isEmpty()) {
            return;
        }

        SystemMetricVO metric = systemMetricsCollector.collectCurrentSystemMetrics();
        LocalDateTime sampleTime = LocalDateTime.now().withNano(0);

        for (TestStatusRecord activeRun : activeRuns) {
            if (activeRun.getTaskId() == null || activeRun.getRunId() == null) {
                continue;
            }
            SystemMetricsRecord record = new SystemMetricsRecord();
            record.setTaskId(activeRun.getTaskId());
            record.setRunId(activeRun.getRunId());
            record.setTs(sampleTime);
            record.setCpu(metric.getCpu());
            record.setMemory(metric.getMemory());
            record.setDisk(metric.getDisk());
            record.setNetworkIn(metric.getNetworkIn());
            record.setNetworkOut(metric.getNetworkOut());
            systemMetricsMapper.insert(record);
        }

        try {
            alertEvaluateService.evaluateSystemMetric(metric);
        } catch (Exception e) {
            log.error("Failed to evaluate scheduled system metric alerts", e);
        }
    }

    private List<TestStatusRecord> listActiveRuns() {
        LambdaQueryWrapper<TestStatusRecord> runningWrapper = new LambdaQueryWrapper<>();
        runningWrapper.eq(TestStatusRecord::getStatus, "RUNNING")
                .orderByDesc(TestStatusRecord::getCreatedAt);

        List<TestStatusRecord> runningCandidates = testStatusRecordMapper.selectList(runningWrapper);
        Map<Long, TestStatusRecord> uniqueRuns = new LinkedHashMap<>();
        for (TestStatusRecord candidate : runningCandidates) {
            if (candidate.getRunId() == null || uniqueRuns.containsKey(candidate.getRunId())) {
                continue;
            }
            uniqueRuns.put(candidate.getRunId(), candidate);
        }

        List<TestStatusRecord> activeRuns = new ArrayList<>();
        for (Long runId : uniqueRuns.keySet()) {
            LambdaQueryWrapper<TestStatusRecord> latestWrapper = new LambdaQueryWrapper<>();
            latestWrapper.eq(TestStatusRecord::getRunId, runId)
                    .orderByDesc(TestStatusRecord::getCreatedAt)
                    .last("limit 1");

            TestStatusRecord latestStatus = testStatusRecordMapper.selectOne(latestWrapper);
            if (latestStatus != null && "RUNNING".equals(latestStatus.getStatus())) {
                activeRuns.add(latestStatus);
            }
        }
        return activeRuns;
    }
}
