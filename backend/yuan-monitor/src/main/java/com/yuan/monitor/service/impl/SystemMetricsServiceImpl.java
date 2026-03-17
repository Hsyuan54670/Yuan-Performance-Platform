package com.yuan.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import com.yuan.monitor.collector.SystemMetricsCollector;
import com.yuan.monitor.entity.SystemMetricsRecord;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.mapper.SystemMetricsMapper;
import com.yuan.monitor.mapper.TestStatusRecordMapper;
import com.yuan.monitor.service.AlertEvaluateService;
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

    @Autowired
    AlertEvaluateService alertEvaluateService;

    @Override
    public R<SystemMetricVO> getSystemMetrics() {
        SystemMetricVO systemMetricVO = systemMetricsCollector.collectCurrentSystemMetrics();
        try {
            alertEvaluateService.evaluateSystemMetric(systemMetricVO);
        } catch (Exception e) {
            log.error("Failed to evaluate system metric alerts", e);
        }
        return R.success(systemMetricVO);
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

        BigDecimal sumCpu = BigDecimal.ZERO;
        BigDecimal sumMemory = BigDecimal.ZERO;
        int highCpuSeconds = 0;
        int highMemorySeconds = 0;
        for (SystemMetricsRecord record : records) {
            BigDecimal cpu = defaultValue(record.getCpu());
            BigDecimal memory = defaultValue(record.getMemory());
            sumCpu = sumCpu.add(cpu);
            sumMemory = sumMemory.add(memory);

            if (cpu.compareTo(HIGH_CPU_THRESHOLD) >= 0) {
                highCpuSeconds++;
            }
            if (memory.compareTo(HIGH_MEMORY_THRESHOLD) >= 0) {
                highMemorySeconds++;
            }
        }

        BigDecimal size = BigDecimal.valueOf(records.size());
        summary.setAvgCpu(sumCpu.divide(size, 2, RoundingMode.HALF_UP));
        summary.setAvgMemory(sumMemory.divide(size, 2, RoundingMode.HALF_UP));
        summary.setPeakCpu(records.stream()
                .map(SystemMetricsRecord::getCpu)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO));
        summary.setPeakMemory(records.stream()
                .map(SystemMetricsRecord::getMemory)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO));
        summary.setHighCpuSeconds(highCpuSeconds);
        summary.setHighMemorySeconds(highMemorySeconds);
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

    // 这里保留原始秒级时间戳，analysis 才能把资源侧波动和业务侧延迟/错误做真正的时序关联。
    private RunSystemMetricPointVO toRunSystemMetricPoint(SystemMetricsRecord record) {
        RunSystemMetricPointVO point = new RunSystemMetricPointVO();
        point.setTs(record.getTs());
        point.setCpu(defaultValue(record.getCpu()));
        point.setMemory(defaultValue(record.getMemory()));
        return point;
    }

    private void fillEmptySummary(RunSystemMetricSummaryVO summary) {
        summary.setAvgCpu(BigDecimal.ZERO);
        summary.setAvgMemory(BigDecimal.ZERO);
        summary.setPeakCpu(BigDecimal.ZERO);
        summary.setPeakMemory(BigDecimal.ZERO);
        summary.setHighCpuSeconds(0);
        summary.setHighMemorySeconds(0);
    }

    private BigDecimal defaultValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
