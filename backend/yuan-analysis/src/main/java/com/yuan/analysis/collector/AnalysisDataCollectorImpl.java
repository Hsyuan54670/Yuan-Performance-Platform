package com.yuan.analysis.collector;

import com.yuan.analysis.model.*;
import com.yuan.api.test.dto.TestMetricDTO;
import com.yuan.api.test.dto.TestRunBaselineDTO;
import com.yuan.api.test.dto.TestRunContextDTO;
import com.yuan.api.test.feign.TestFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
// 负责把一次 run 的上下文与秒级指标收集成统一快照，供规则引擎和后续 AI 分析复用。
public class AnalysisDataCollectorImpl implements AnalysisDataCollector {
    @Autowired
    TestFeignClient testFeignClient;

    @Override
    public AnalysisSnapshot collectSnapshot(Long userId, Long taskId, Long runId, String finalStatus) {
        AnalysisSnapshot snapshot = new AnalysisSnapshot();
        snapshot.setUserId(userId);
        snapshot.setTaskId(taskId);
        snapshot.setRunId(runId);
        snapshot.setFinalStatus(finalStatus);

        TestRunContextDTO testRunContextDTO = null;
        try {
            testRunContextDTO = testFeignClient.getRunContext(userId, runId).getData();
        } catch (Exception e) {
            throw new RuntimeException("服务调用出错");
        }
        if (testRunContextDTO == null) {
            throw new RuntimeException("无法获取压测运行上下文信息");
        }

        // run 上下文描述这次执行发生在什么场景下，是后续报告解释的重要背景信息。
        RunContext runContext = new RunContext();
        runContext.setConcurrency(testRunContextDTO.getConcurrency());
        runContext.setStartTime(testRunContextDTO.getStartTime());
        runContext.setEndTime(testRunContextDTO.getEndTime());
        runContext.setPlanName(testRunContextDTO.getPlanName());
        runContext.setSceneName(testRunContextDTO.getSceneName());
        runContext.setDurationSeconds(testRunContextDTO.getDurationSeconds());
        snapshot.setContext(runContext);

        List<TestMetricDTO> metrics = null;
        try {
            metrics = testFeignClient.listRunMetrics(userId, runId).getData();
        } catch (Exception e) {
            throw new RuntimeException("服务调用出错");
        }

        List<SecondMetricPoint> metricPoints = toMetricPoints(metrics);

        // 先保存原始时序点，再计算摘要与特征，后续接秒级分析时不用推翻现有模型。
        snapshot.setSeries(buildSeries(metricPoints));
        snapshot.setSummary(buildSummary(metricPoints));
        snapshot.setFeature(buildFeature(metricPoints));
        snapshot.setBaseline(loadBaseline(userId, runId, snapshot.getSummary()));
        return snapshot;
    }

    // 把 test 服务返回的 DTO 转成 analysis 内部统一使用的秒级点模型。
    private List<SecondMetricPoint> toMetricPoints(List<TestMetricDTO> metrics) {
        List<SecondMetricPoint> secondMetricPoints = null;
        if (metrics == null || metrics.isEmpty()) {
            metrics = new ArrayList<>();
        }
        secondMetricPoints = metrics
                .stream()
                .map(m -> {
                    SecondMetricPoint point = new SecondMetricPoint();
                    // point.setCpu(m.getCpu());
                    // point.setMemory(m.getMemory());
                    point.setQps(m.getQps());
                    point.setP50(m.getP50());
                    point.setP90(m.getP90());
                    point.setP99(m.getP99());
                    point.setErrorRate(m.getErrorRate());
                    point.setTs(m.getTime());
                    return point;
                }).toList();
        return secondMetricPoints;
    }

    // 时序容器只做轻量封装，后续可以在这里补采样间隔、窗口信息等元数据。
    private MetricSeriesBundle buildSeries(List<SecondMetricPoint> points) {
        MetricSeriesBundle series = new MetricSeriesBundle();
        series.setSamplingIntervalSeconds(1);
        series.setPoints(points);
        return series;
    }

    // 摘要层优先提供规则引擎最需要的几个核心指标，避免第一版过度设计。
    private MetricSummary buildSummary(List<SecondMetricPoint> points) {
        MetricSummary summary = new MetricSummary();
        if (points == null || points.isEmpty()) {
            summary.setAvgMemory(BigDecimal.ZERO);
            summary.setAvgCpu(BigDecimal.ZERO);
            summary.setP50(BigDecimal.ZERO);
            summary.setP90(BigDecimal.ZERO);
            summary.setP99(BigDecimal.ZERO);
            summary.setErrorRate(BigDecimal.ZERO);
            summary.setAvgQps(BigDecimal.ZERO);
            return summary;
        }
        BigDecimal sumQps = points.stream()
                .map(SecondMetricPoint::getQps)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgQps = sumQps.divide(BigDecimal.valueOf(points.size()),
                2, RoundingMode.HALF_UP);

        summary.setP50(points.getLast().getP50());
        summary.setP90(points.getLast().getP90());
        summary.setP99(points.getLast().getP99());
        summary.setErrorRate(points.getLast().getErrorRate());
        // summary.setAvgCpu();
        // summary.setAvgMemory();
        summary.setAvgQps(avgQps);
        return summary;
    }

    // 特征层从秒级数据里提取峰值和异常持续时间，方便规则引擎直接消费。
    private MetricFeature buildFeature(List<SecondMetricPoint> points) {
        MetricFeature feature = new MetricFeature();
        BigDecimal peakP99 = BigDecimal.ZERO;
        BigDecimal peakQps = BigDecimal.ZERO;
        BigDecimal peakErrorRate = BigDecimal.ZERO;
        Integer highLatencySeconds = 0;
        Integer errorSpikeSeconds = 0;
        for (SecondMetricPoint point : points) {
            if (point.getQps().compareTo(peakQps) > 0) {
                peakQps = point.getQps();
            }
            if (point.getP99().compareTo(peakP99) > 0) {
                peakP99 = point.getP99();
            }
            if (point.getErrorRate().compareTo(peakErrorRate) > 0) {
                peakErrorRate = point.getErrorRate();
            }

            if (point.getP99().compareTo(BigDecimal.valueOf(2000)) > 0) {
                highLatencySeconds++;
            }
            if (point.getErrorRate().compareTo(BigDecimal.valueOf(5)) > 0) {
                errorSpikeSeconds++;
            }
        }
        feature.setPeakP99(peakP99);
        feature.setPeakQps(peakQps);
        feature.setPeakErrorRate(peakErrorRate);
        feature.setErrorSpikeSeconds(errorSpikeSeconds);
        feature.setHighLatencySeconds(highLatencySeconds);
        return feature;
    }

    // 基线来自当前 run 之前最近一次成功运行的摘要，用于吞吐回归等横向对比规则。
    private BaselineSnapshot loadBaseline(Long userId, Long runId, MetricSummary currentSummary) {
        TestRunBaselineDTO baselineDTO;
        try {
            baselineDTO = testFeignClient.getRunBaseline(userId, runId).getData();
        } catch (Exception e) {
            throw new RuntimeException("服务调用出错");
        }

        BaselineSnapshot baseline = new BaselineSnapshot();
        if (baselineDTO == null) {
            baseline.setBaselineRunId(null);
            baseline.setBaselineQps(BigDecimal.ZERO);
            baseline.setBaselineP99(BigDecimal.ZERO);
            baseline.setBaselineErrorRate(BigDecimal.ZERO);
            baseline.setQpsChangeRate(BigDecimal.ZERO);
            baseline.setP99ChangeRate(BigDecimal.ZERO);
            baseline.setErrorRateChangeRate(BigDecimal.ZERO);
            return baseline;
        }

        baseline.setBaselineRunId(baselineDTO.getBaselineRunId());
        baseline.setBaselineQps(defaultValue(baselineDTO.getAvgQps()));
        baseline.setBaselineP99(defaultValue(baselineDTO.getP99()));
        baseline.setBaselineErrorRate(defaultValue(baselineDTO.getErrorRate()));
        baseline.setQpsChangeRate(calculateChangeRate(currentSummary.getAvgQps(), baseline.getBaselineQps()));
        baseline.setP99ChangeRate(calculateChangeRate(currentSummary.getP99(), baseline.getBaselineP99()));
        baseline.setErrorRateChangeRate(calculateChangeRate(currentSummary.getErrorRate(), baseline.getBaselineErrorRate()));
        return baseline;
    }

    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal baseline) {
        BigDecimal safeBaseline = defaultValue(baseline);
        if (safeBaseline.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return defaultValue(current)
                .subtract(safeBaseline)
                .divide(safeBaseline, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
