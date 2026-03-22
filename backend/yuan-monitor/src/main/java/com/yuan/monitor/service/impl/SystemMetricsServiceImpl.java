package com.yuan.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import com.yuan.monitor.collector.SystemMetricsCollector;
import com.yuan.monitor.entity.SystemMetricsRecord;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.mapper.SystemMetricsMapper;
import com.yuan.monitor.mapper.TestStatusRecordMapper;
import com.yuan.monitor.service.SystemMetricsService;
import com.yuan.monitor.vo.RunSystemMetricPointVO;
import com.yuan.monitor.vo.RunSystemMetricSummaryVO;
import com.yuan.monitor.vo.SystemMetricVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class SystemMetricsServiceImpl implements SystemMetricsService {
    private static final BigDecimal HIGH_CPU_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal HIGH_MEMORY_THRESHOLD = BigDecimal.valueOf(80);

    @Autowired
    SystemMetricsCollector systemMetricsCollector;

    @Autowired
    SystemMetricsMapper systemMetricsMapper;

    @Autowired
    TestStatusRecordMapper testStatusRecordMapper;

    @Override
    public R<SystemMetricVO> getSystemMetrics() {
        return R.success(systemMetricsCollector.collectCurrentSystemMetricsForDisplay());
    }

    @Override
    public R<RunSystemMetricSummaryVO> getRunSystemMetricSummary(Long userId, Long runId) {
        if (!existsOwnedRun(userId, runId)) {
            return R.fail(HttpStatus.NOT_FOUND, "运行记录不存在");
        }

        List<SystemMetricsRecord> records = listRunMetricRecords(runId);
        RunSystemMetricSummaryVO summary = new RunSystemMetricSummaryVO();
        summary.setRunId(runId);
        if (records == null || records.isEmpty()) {
            fillEmptySummary(summary);
            return R.success(summary);
        }

        List<SystemMetricsRecord> sampledRecords = records.stream()
                .filter(this::isSampledRecord)
                .toList();
        summary.setSampledSeconds(sampledRecords.size());
        summary.setMissingSeconds(records.size() - sampledRecords.size());
        if (sampledRecords.isEmpty()) {
            fillEmptySummary(summary);
            summary.setMissingSeconds(records.size());
            return R.success(summary);
        }

        BigDecimal size = BigDecimal.valueOf(sampledRecords.size());
        BigDecimal sumCpu = sampledRecords.stream()
                .map(SystemMetricsRecord::getCpu)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumMemory = sampledRecords.stream()
                .map(SystemMetricsRecord::getMemory)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summary.setAvgCpu(sumCpu.divide(size, 2, RoundingMode.HALF_UP));
        summary.setAvgMemory(sumMemory.divide(size, 2, RoundingMode.HALF_UP));
        summary.setPeakCpu(sampledRecords.stream()
                .map(this::resolvePeakCpu)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO));
        summary.setPeakMemory(sampledRecords.stream()
                .map(this::resolvePeakMemory)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO));
        summary.setHighCpuSeconds((int) sampledRecords.stream()
                .map(SystemMetricsRecord::getCpu)
                .filter(value -> value != null && value.compareTo(HIGH_CPU_THRESHOLD) >= 0)
                .count());
        summary.setHighMemorySeconds((int) sampledRecords.stream()
                .map(SystemMetricsRecord::getMemory)
                .filter(value -> value != null && value.compareTo(HIGH_MEMORY_THRESHOLD) >= 0)
                .count());
        return R.success(summary);
    }

    @Override
    public R<List<RunSystemMetricPointVO>> listRunSystemMetricPoints(Long userId, Long runId) {
        if (!existsOwnedRun(userId, runId)) {
            return R.fail(HttpStatus.NOT_FOUND, "运行记录不存在");
        }

        List<RunSystemMetricPointVO> points = listRunMetricRecords(runId).stream()
                .map(this::toRunSystemMetricPoint)
                .toList();
        return R.success(points);
    }

    private boolean existsOwnedRun(Long userId, Long runId) {
        LambdaQueryWrapper<TestStatusRecord> ownershipWrapper = new LambdaQueryWrapper<>();
        ownershipWrapper.eq(TestStatusRecord::getUserId, userId)
                .eq(TestStatusRecord::getRunId, runId)
                .last("limit 1");
        return testStatusRecordMapper.selectOne(ownershipWrapper) != null;
    }

    private List<SystemMetricsRecord> listRunMetricRecords(Long runId) {
        LambdaQueryWrapper<SystemMetricsRecord> metricsWrapper = new LambdaQueryWrapper<>();
        metricsWrapper.eq(SystemMetricsRecord::getRunId, runId)
                .orderByAsc(SystemMetricsRecord::getTs);
        return systemMetricsMapper.selectList(metricsWrapper);
    }

    // 这里保留秒桶平均值和秒内峰值，analysis 才能同时看到“持续压力”和“短时尖峰”。
    private RunSystemMetricPointVO toRunSystemMetricPoint(SystemMetricsRecord record) {
        RunSystemMetricPointVO point = new RunSystemMetricPointVO();
        point.setTs(record.getTs());
        point.setCpu(record.getCpu());
        point.setCpuMax(record.getCpuMax());
        point.setMemory(record.getMemory());
        point.setMemoryMax(record.getMemoryMax());
        point.setSampleCount(record.getSampleCount());
        point.setMissing(record.getMissing());
        return point;
    }

    private boolean isSampledRecord(SystemMetricsRecord record) {
        return record != null
                && !Boolean.TRUE.equals(record.getMissing())
                && record.getSampleCount() != null
                && record.getSampleCount() > 0;
    }

    private BigDecimal resolvePeakCpu(SystemMetricsRecord record) {
        return record.getCpuMax() != null ? record.getCpuMax() : record.getCpu();
    }

    private BigDecimal resolvePeakMemory(SystemMetricsRecord record) {
        return record.getMemoryMax() != null ? record.getMemoryMax() : record.getMemory();
    }

    private void fillEmptySummary(RunSystemMetricSummaryVO summary) {
        summary.setAvgCpu(BigDecimal.ZERO);
        summary.setAvgMemory(BigDecimal.ZERO);
        summary.setPeakCpu(BigDecimal.ZERO);
        summary.setPeakMemory(BigDecimal.ZERO);
        summary.setHighCpuSeconds(0);
        summary.setHighMemorySeconds(0);
        if (summary.getSampledSeconds() == null) {
            summary.setSampledSeconds(0);
        }
        if (summary.getMissingSeconds() == null) {
            summary.setMissingSeconds(0);
        }
    }
}
