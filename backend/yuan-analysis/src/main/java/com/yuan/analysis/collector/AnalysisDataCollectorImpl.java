package com.yuan.analysis.collector;

import com.yuan.analysis.model.*;
import com.yuan.api.monitor.dto.RunSystemMetricPointDTO;
import com.yuan.api.monitor.dto.RunSystemMetricSummaryDTO;
import com.yuan.api.monitor.feign.MonitorFeignClient;
import com.yuan.api.test.dto.TestMetricDTO;
import com.yuan.api.test.dto.TestRunBaselineDTO;
import com.yuan.api.test.dto.TestRunContextDTO;
import com.yuan.api.test.feign.TestFeignClient;
import com.yuan.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
// 负责把一次 run 的上下文与秒级指标收集成统一快照，供规则引擎和后续 AI 分析复用。
public class AnalysisDataCollectorImpl implements AnalysisDataCollector {
    private static final long RESOURCE_ALIGNMENT_WINDOW_MILLIS = 1000;

    @Autowired
    TestFeignClient testFeignClient;

    @Autowired
    MonitorFeignClient monitorFeignClient;

    @Override
    public AnalysisSnapshot collectSnapshot(Long userId, Long taskId, Long runId, String finalStatus) {
        AnalysisSnapshot snapshot = new AnalysisSnapshot();
        snapshot.setUserId(userId);
        snapshot.setTaskId(taskId);
        snapshot.setRunId(runId);
        snapshot.setFinalStatus(finalStatus);

        TestRunContextDTO testRunContextDTO;
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

        List<TestMetricDTO> metrics;
        try {
            metrics = testFeignClient.listRunMetrics(userId, runId).getData();
        } catch (Exception e) {
            throw new RuntimeException("服务调用出错");
        }

        List<SecondMetricPoint> businessMetricPoints = toMetricPoints(metrics);
        List<RunSystemMetricPointDTO> systemMetricPoints = loadRunSystemMetricPoints(userId, runId);

        snapshot.setSummary(buildSummary(businessMetricPoints));
        snapshot.setFeature(buildFeature(businessMetricPoints));
        // 统一时序以业务秒点为主轴，资源点只做贴合，避免两个时间轴并集后把 run 人为拉长。
        snapshot.setSeries(buildSeries(mergeMetricSeries(businessMetricPoints, systemMetricPoints)));
        applySystemMetricSummary(snapshot.getSummary(), snapshot.getFeature(), loadRunSystemMetricSummary(userId, runId));
        snapshot.setBaseline(loadBaseline(userId, runId, snapshot.getSummary()));
        return snapshot;
    }

    // 把 test 服务返回的 DTO 转成 analysis 内部统一使用的秒级点模型。
    private List<SecondMetricPoint> toMetricPoints(List<TestMetricDTO> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            metrics = new ArrayList<>();
        }
        return metrics
                .stream()
                .map(m -> {
                    SecondMetricPoint point = new SecondMetricPoint();
                    point.setQps(defaultValue(m.getQps()));
                    point.setP50(defaultValue(m.getP50()));
                    point.setP90(defaultValue(m.getP90()));
                    point.setP99(defaultValue(m.getP99()));
                    point.setErrorRate(defaultValue(m.getErrorRate()));
                    point.setTs(m.getTime());
                    return point;
                }).toList();
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

    private RunSystemMetricSummaryDTO loadRunSystemMetricSummary(Long userId, Long runId) {
        try {
            R<RunSystemMetricSummaryDTO> response = monitorFeignClient.getRunSystemMetricSummary(userId, runId);
            return response == null ? null : response.getData();
        } catch (Exception e) {
            log.warn("Failed to load run system metric summary, runId={}", runId, e);
            return null;
        }
    }

    private List<RunSystemMetricPointDTO> loadRunSystemMetricPoints(Long userId, Long runId) {
        try {
            R<List<RunSystemMetricPointDTO>> response = monitorFeignClient.listRunSystemMetricPoints(userId, runId);
            if (response == null || response.getData() == null) {
                return List.of();
            }
            return response.getData();
        } catch (Exception e) {
            log.warn("Failed to load run system metric points, runId={}", runId, e);
            return List.of();
        }
    }

    // 统一时序优先保留业务主轴，资源点按最近秒贴合，避免业务/资源并集后把时长放大成稀疏序列。
    private List<SecondMetricPoint> mergeMetricSeries(List<SecondMetricPoint> businessPoints,
                                                      List<RunSystemMetricPointDTO> systemPoints) {
        if (businessPoints == null || businessPoints.isEmpty()) {
            return toSystemOnlyPoints(systemPoints);
        }

        List<SecondMetricPoint> alignedPoints = businessPoints.stream()
                .map(this::copyPoint)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        List<RunSystemMetricPointDTO> sortedSystemPoints = sortSystemPoints(systemPoints);
        int nextSystemIndex = 0;

        for (SecondMetricPoint businessPoint : alignedPoints) {
            if (businessPoint.getTs() == null) {
                continue;
            }

            while (nextSystemIndex < sortedSystemPoints.size()
                    && isEarlierThanAlignmentWindow(sortedSystemPoints.get(nextSystemIndex).getTs(), businessPoint.getTs())) {
                nextSystemIndex++;
            }

            int matchedIndex = findClosestSystemPointIndex(sortedSystemPoints, nextSystemIndex, businessPoint.getTs());
            if (matchedIndex < 0) {
                continue;
            }

            applySystemPoint(businessPoint, sortedSystemPoints.get(matchedIndex));
            nextSystemIndex = matchedIndex + 1;
        }

        return alignedPoints;
    }

    private List<RunSystemMetricPointDTO> sortSystemPoints(List<RunSystemMetricPointDTO> systemPoints) {
        if (systemPoints == null || systemPoints.isEmpty()) {
            return List.of();
        }

        return systemPoints.stream()
                .filter(point -> point != null && point.getTs() != null)
                .sorted(Comparator.comparing(RunSystemMetricPointDTO::getTs))
                .toList();
    }

    private boolean isEarlierThanAlignmentWindow(LocalDateTime systemTs, LocalDateTime businessTs) {
        return systemTs != null
                && businessTs != null
                && systemTs.isBefore(businessTs.minusSeconds(1));
    }

    private int findClosestSystemPointIndex(List<RunSystemMetricPointDTO> systemPoints,
                                            int startIndex,
                                            LocalDateTime businessTs) {
        int bestIndex = -1;
        long bestDiff = Long.MAX_VALUE;

        for (int i = startIndex; i < systemPoints.size(); i++) {
            RunSystemMetricPointDTO candidate = systemPoints.get(i);
            LocalDateTime systemTs = candidate.getTs();
            if (systemTs == null) {
                continue;
            }

            long diffMillis = Math.abs(Duration.between(systemTs, businessTs).toMillis());
            if (systemTs.isAfter(businessTs.plusSeconds(1)) && diffMillis > RESOURCE_ALIGNMENT_WINDOW_MILLIS) {
                break;
            }
            if (diffMillis > RESOURCE_ALIGNMENT_WINDOW_MILLIS) {
                continue;
            }

            if (diffMillis < bestDiff) {
                bestDiff = diffMillis;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    // 没有业务秒点时，退回资源主轴，至少保留资源时序给规则和 AI 解释层使用。
    private List<SecondMetricPoint> toSystemOnlyPoints(List<RunSystemMetricPointDTO> systemPoints) {
        if (systemPoints == null || systemPoints.isEmpty()) {
            return List.of();
        }

        return sortSystemPoints(systemPoints).stream()
                .map(systemPoint -> {
                    SecondMetricPoint point = new SecondMetricPoint();
                    point.setTs(systemPoint.getTs());
                    applySystemPoint(point, systemPoint);
                    return point;
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private void applySystemPoint(SecondMetricPoint target, RunSystemMetricPointDTO source) {
        target.setCpu(source.getCpu());
        target.setCpuMax(source.getCpuMax());
        target.setMemory(source.getMemory());
        target.setMemoryMax(source.getMemoryMax());
        target.setSampleCount(source.getSampleCount());
        target.setMissing(source.getMissing());
    }

    private SecondMetricPoint copyPoint(SecondMetricPoint source) {
        SecondMetricPoint copy = new SecondMetricPoint();
        copy.setTs(source.getTs());
        copy.setQps(source.getQps());
        copy.setP50(source.getP50());
        copy.setP90(source.getP90());
        copy.setP99(source.getP99());
        copy.setErrorRate(source.getErrorRate());
        copy.setCpu(source.getCpu());
        copy.setCpuMax(source.getCpuMax());
        copy.setMemory(source.getMemory());
        copy.setMemoryMax(source.getMemoryMax());
        copy.setSampleCount(source.getSampleCount());
        copy.setMissing(source.getMissing());
        return copy;
    }

    // 把 monitor 提供的 CPU / Memory 摘要并入快照，让规则引擎和后续 AI 能拿到资源侧证据。
    private void applySystemMetricSummary(MetricSummary summary, MetricFeature feature, RunSystemMetricSummaryDTO dto) {
        if (dto == null) {
            summary.setAvgCpu(BigDecimal.ZERO);
            summary.setAvgMemory(BigDecimal.ZERO);
            feature.setPeakCpu(BigDecimal.ZERO);
            feature.setPeakMemory(BigDecimal.ZERO);
            feature.setHighCpuSeconds(0);
            feature.setHighMemorySeconds(0);
            return;
        }

        summary.setAvgCpu(defaultValue(dto.getAvgCpu()));
        summary.setAvgMemory(defaultValue(dto.getAvgMemory()));
        feature.setPeakCpu(defaultValue(dto.getPeakCpu()));
        feature.setPeakMemory(defaultValue(dto.getPeakMemory()));
        feature.setHighCpuSeconds(dto.getHighCpuSeconds() == null ? 0 : dto.getHighCpuSeconds());
        feature.setHighMemorySeconds(dto.getHighMemorySeconds() == null ? 0 : dto.getHighMemorySeconds());
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
