package com.yuan.analysis.rule;

import com.yuan.analysis.entity.AnalysisSuggestion;
import com.yuan.analysis.entity.BottleneckRecord;
import com.yuan.analysis.model.AnalysisComputationResult;
import com.yuan.analysis.model.AnalysisSnapshot;
import com.yuan.analysis.model.BaselineSnapshot;
import com.yuan.analysis.model.MetricFeature;
import com.yuan.analysis.model.MetricSummary;
import com.yuan.analysis.model.RuleHit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class RuleEngine {

    // 绝对阈值负责兜底判定，避免没有基线时完全失去判断能力。
    private static final BigDecimal HIGH_LATENCY_THRESHOLD = BigDecimal.valueOf(2000);
    private static final BigDecimal ERROR_RATE_THRESHOLD = BigDecimal.valueOf(5);
    private static final BigDecimal AVG_CPU_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal PEAK_CPU_THRESHOLD = BigDecimal.valueOf(90);
    private static final BigDecimal AVG_MEMORY_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal PEAK_MEMORY_THRESHOLD = BigDecimal.valueOf(90);

    // 基线阈值负责识别“相对退化”，例如同一任务虽然没触发硬阈值，但已经明显变差。
    private static final BigDecimal THROUGHPUT_REGRESSION_RATIO = BigDecimal.valueOf(0.85);
    private static final BigDecimal P99_REGRESSION_RATIO = BigDecimal.valueOf(0.30);
    private static final BigDecimal ERROR_RATE_REGRESSION_RATIO = BigDecimal.ONE;

    // 持续时间阈值用于区分瞬时抖动和持续性风险。
    private static final int HIGH_LATENCY_SECONDS_THRESHOLD = 3;
    private static final int ERROR_SPIKE_SECONDS_THRESHOLD = 3;
    private static final int HIGH_CPU_SECONDS_THRESHOLD = 5;
    private static final int HIGH_MEMORY_SECONDS_THRESHOLD = 5;
    private static final int MIN_SCORE = 55;

    // 规则引擎只做确定性判断，产出规则命中、基准分和基准摘要，后续 AI 只在此基础上做增强。
    public AnalysisComputationResult analyze(AnalysisSnapshot snapshot) {
        AnalysisComputationResult result = new AnalysisComputationResult();
        List<RuleHit> hits = new ArrayList<>();

        if (isFailed(snapshot)) {
            hits.add(buildFailedHit());
        }
        if (isHighLatency(snapshot)) {
            hits.add(buildHighLatencyHit(snapshot));
        }
        if (isErrorSpike(snapshot)) {
            hits.add(buildErrorSpikeHit(snapshot));
        }
        if (isCpuPressure(snapshot)) {
            hits.add(buildCpuPressureHit(snapshot));
        }
        if (isMemoryPressure(snapshot)) {
            hits.add(buildMemoryPressureHit(snapshot));
        }
        if (isThroughputRegression(snapshot)) {
            hits.add(buildThroughputRegressionHit(snapshot));
        }

        result.setRuleHits(hits);
        snapshot.setRuleHits(hits);

        fillScoreAndGrade(result, hits, snapshot);
        fillSummary(result, hits, snapshot);
        fillLegacyItems(result, hits, snapshot);
        return result;
    }

    private boolean isFailed(AnalysisSnapshot snapshot) {
        return "FAILED".equalsIgnoreCase(snapshot.getFinalStatus());
    }

    // 高延迟同时看绝对风险和基线退化，避免只盯当前值而漏掉“虽然没爆表但明显变慢”的情况。
    private boolean isHighLatency(AnalysisSnapshot snapshot) {
        MetricSummary summary = snapshot.getSummary();
        MetricFeature feature = snapshot.getFeature();
        BaselineSnapshot baseline = snapshot.getBaseline();
        BigDecimal p99 = safe(summary.getP99());
        int highLatencySeconds = safe(feature.getHighLatencySeconds());
        BigDecimal p99ChangeRate = safe(baseline.getP99ChangeRate());
        boolean absoluteRisk = p99.compareTo(HIGH_LATENCY_THRESHOLD) >= 0
                || highLatencySeconds >= HIGH_LATENCY_SECONDS_THRESHOLD;
        boolean baselineRisk = safe(baseline.getBaselineP99()).compareTo(BigDecimal.ZERO) > 0
                && p99ChangeRate.compareTo(P99_REGRESSION_RATIO) >= 0;
        return absoluteRisk || baselineRisk;
    }

    // 错误率同样采用“当前值 + 持续秒数 + 基线恶化”三路合并判断，避免被偶发错误误伤。
    private boolean isErrorSpike(AnalysisSnapshot snapshot) {
        MetricSummary summary = snapshot.getSummary();
        MetricFeature feature = snapshot.getFeature();
        BaselineSnapshot baseline = snapshot.getBaseline();
        BigDecimal errorRate = safe(summary.getErrorRate());
        int errorSpikeSeconds = safe(feature.getErrorSpikeSeconds());
        BigDecimal errorRateChangeRate = safe(baseline.getErrorRateChangeRate());
        boolean absoluteRisk = errorRate.compareTo(ERROR_RATE_THRESHOLD) >= 0
                || errorSpikeSeconds >= ERROR_SPIKE_SECONDS_THRESHOLD;
        boolean baselineRisk = safe(baseline.getBaselineErrorRate()).compareTo(BigDecimal.ZERO) > 0
                && errorRateChangeRate.compareTo(ERROR_RATE_REGRESSION_RATIO) >= 0;
        return absoluteRisk || baselineRisk;
    }

    private boolean isCpuPressure(AnalysisSnapshot snapshot) {
        MetricSummary summary = snapshot.getSummary();
        MetricFeature feature = snapshot.getFeature();
        BigDecimal avgCpu = safe(summary.getAvgCpu());
        BigDecimal peakCpu = safe(feature.getPeakCpu());
        int highCpuSeconds = safe(feature.getHighCpuSeconds());
        return avgCpu.compareTo(AVG_CPU_THRESHOLD) >= 0
                || peakCpu.compareTo(PEAK_CPU_THRESHOLD) >= 0
                || highCpuSeconds >= HIGH_CPU_SECONDS_THRESHOLD;
    }

    private boolean isMemoryPressure(AnalysisSnapshot snapshot) {
        MetricSummary summary = snapshot.getSummary();
        MetricFeature feature = snapshot.getFeature();
        BigDecimal avgMemory = safe(summary.getAvgMemory());
        BigDecimal peakMemory = safe(feature.getPeakMemory());
        int highMemorySeconds = safe(feature.getHighMemorySeconds());
        return avgMemory.compareTo(AVG_MEMORY_THRESHOLD) >= 0
                || peakMemory.compareTo(PEAK_MEMORY_THRESHOLD) >= 0
                || highMemorySeconds >= HIGH_MEMORY_SECONDS_THRESHOLD;
    }

    private boolean isThroughputRegression(AnalysisSnapshot snapshot) {
        MetricSummary summary = snapshot.getSummary();
        BaselineSnapshot baseline = snapshot.getBaseline();
        BigDecimal baselineQps = safe(baseline.getBaselineQps());
        if (baselineQps.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal currentQps = safe(summary.getAvgQps());
        BigDecimal threshold = baselineQps.multiply(THROUGHPUT_REGRESSION_RATIO);
        return currentQps.compareTo(threshold) < 0;
    }

    private RuleHit buildFailedHit() {
        RuleHit hit = new RuleHit();
        hit.setCode("FAILED_RUN");
        hit.setType("STABILITY");
        hit.setSeverity("CRITICAL");
        hit.setTitle("压测运行失败");
        hit.setReason("本次压测未正常完成，当前结果优先反映运行稳定性问题。");
        hit.setEvidence("finalStatus=FAILED");
        hit.setPriority("P0");
        hit.setSuggestionTitle("优先排查运行失败原因");
        hit.setSuggestionDetail("先检查错误日志、JMeter 执行异常、下游依赖状态和线程池资源，再进行性能分析。");
        return hit;
    }

    private RuleHit buildHighLatencyHit(AnalysisSnapshot snapshot) {
        BigDecimal currentP99 = safe(snapshot.getSummary().getP99());
        BigDecimal baselineP99 = safe(snapshot.getBaseline().getBaselineP99());
        BigDecimal p99ChangeRate = safe(snapshot.getBaseline().getP99ChangeRate());

        RuleHit hit = new RuleHit();
        hit.setCode("HIGH_LATENCY");
        hit.setType("HIGH_LATENCY");
        hit.setSeverity("HIGH");
        hit.setTitle("高延迟风险");
        hit.setReason("P99 响应时间过高，或相较历史基线出现明显退化，说明尾延迟存在抖动、阻塞或慢依赖风险。");
        hit.setEvidence(String.format(
                "p99=%sms, baselineP99=%sms, p99Change=%s%%, highLatencySeconds=%s",
                currentP99.stripTrailingZeros().toPlainString(),
                baselineP99.stripTrailingZeros().toPlainString(),
                p99ChangeRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(),
                safe(snapshot.getFeature().getHighLatencySeconds())
        ));
        hit.setPriority("P1");
        hit.setSuggestionTitle("优先排查慢接口与下游耗时");
        hit.setSuggestionDetail("建议优先检查慢 SQL、远程调用耗时、线程池阻塞以及接口预热情况，并对比历史基线定位退化来源。");
        return hit;
    }

    private RuleHit buildErrorSpikeHit(AnalysisSnapshot snapshot) {
        BigDecimal currentErrorRate = safe(snapshot.getSummary().getErrorRate());
        BigDecimal baselineErrorRate = safe(snapshot.getBaseline().getBaselineErrorRate());
        BigDecimal errorRateChangeRate = safe(snapshot.getBaseline().getErrorRateChangeRate());

        RuleHit hit = new RuleHit();
        hit.setCode("ERROR_SPIKE");
        hit.setType("ERROR_RATE");
        hit.setSeverity("CRITICAL");
        hit.setTitle("错误率升高");
        hit.setReason("运行过程中出现较明显的错误率上升，或相较历史基线明显恶化，说明服务稳定性存在风险。");
        hit.setEvidence(String.format(
                "errorRate=%s%%, baselineErrorRate=%s%%, errorChange=%s%%, errorSpikeSeconds=%s",
                currentErrorRate.stripTrailingZeros().toPlainString(),
                baselineErrorRate.stripTrailingZeros().toPlainString(),
                errorRateChangeRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(),
                safe(snapshot.getFeature().getErrorSpikeSeconds())
        ));
        hit.setPriority("P0");
        hit.setSuggestionTitle("优先排查异常响应与下游依赖");
        hit.setSuggestionDetail("建议先检查接口错误响应、应用异常栈、数据库连接状态以及外部依赖可用性，并结合基线确认恶化范围。");
        return hit;
    }

    private RuleHit buildCpuPressureHit(AnalysisSnapshot snapshot) {
        BigDecimal avgCpu = safe(snapshot.getSummary().getAvgCpu());
        BigDecimal peakCpu = safe(snapshot.getFeature().getPeakCpu());
        int highCpuSeconds = safe(snapshot.getFeature().getHighCpuSeconds());

        RuleHit hit = new RuleHit();
        hit.setCode("CPU_PRESSURE");
        hit.setType("CPU_PRESSURE");
        hit.setSeverity("HIGH");
        hit.setTitle("CPU 压力偏高");
        hit.setReason("运行期间 CPU 长时间处于高位，说明应用计算开销、线程竞争或热点逻辑可能已成为性能瓶颈。");
        hit.setEvidence(String.format(
                "avgCpu=%s%%, peakCpu=%s%%, highCpuSeconds=%s",
                avgCpu.stripTrailingZeros().toPlainString(),
                peakCpu.stripTrailingZeros().toPlainString(),
                highCpuSeconds
        ));
        hit.setPriority("P1");
        hit.setSuggestionTitle("优先排查热点计算与线程竞争");
        hit.setSuggestionDetail("建议结合压测窗口检查 CPU profile、GC、线程池队列和热点接口逻辑，确认是否存在计算密集型瓶颈。");
        return hit;
    }

    private RuleHit buildMemoryPressureHit(AnalysisSnapshot snapshot) {
        BigDecimal avgMemory = safe(snapshot.getSummary().getAvgMemory());
        BigDecimal peakMemory = safe(snapshot.getFeature().getPeakMemory());
        int highMemorySeconds = safe(snapshot.getFeature().getHighMemorySeconds());

        RuleHit hit = new RuleHit();
        hit.setCode("MEMORY_PRESSURE");
        hit.setType("MEMORY_PRESSURE");
        hit.setSeverity("HIGH");
        hit.setTitle("内存压力偏高");
        hit.setReason("运行期间内存使用率持续偏高，说明缓存膨胀、对象堆积或资源释放不及时的风险正在累积。");
        hit.setEvidence(String.format(
                "avgMemory=%s%%, peakMemory=%s%%, highMemorySeconds=%s",
                avgMemory.stripTrailingZeros().toPlainString(),
                peakMemory.stripTrailingZeros().toPlainString(),
                highMemorySeconds
        ));
        hit.setPriority("P1");
        hit.setSuggestionTitle("优先排查堆占用与缓存增长");
        hit.setSuggestionDetail("建议结合 GC 日志、堆快照和缓存命中情况检查对象滞留、批量加载或缓存未命中导致的内存压力。");
        return hit;
    }

    private RuleHit buildThroughputRegressionHit(AnalysisSnapshot snapshot) {
        BigDecimal currentQps = safe(snapshot.getSummary().getAvgQps());
        BigDecimal baselineQps = safe(snapshot.getBaseline().getBaselineQps());
        BigDecimal qpsChangeRate = safe(snapshot.getBaseline().getQpsChangeRate());

        RuleHit hit = new RuleHit();
        hit.setCode("THROUGHPUT_REGRESSION");
        hit.setType("THROUGHPUT");
        hit.setSeverity("MEDIUM");
        hit.setTitle("吞吐回归");
        hit.setReason("当前平均吞吐低于历史基线，说明相同任务在本次运行中的承载能力有所下降。");
        hit.setEvidence(String.format(
                "avgQps=%s, baselineQps=%s, qpsChange=%s%%",
                currentQps.stripTrailingZeros().toPlainString(),
                baselineQps.stripTrailingZeros().toPlainString(),
                qpsChangeRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        ));
        hit.setPriority("P2");
        hit.setSuggestionTitle("对比历史基线定位吞吐退化原因");
        hit.setSuggestionDetail("建议结合本次与基线运行的响应时间、错误率和资源使用差异，定位吞吐下降的关键瓶颈。");
        return hit;
    }

    // 评分是规则层的基准分，不追求非常细腻，但要保证可解释、可重复。
    private void fillScoreAndGrade(AnalysisComputationResult result, List<RuleHit> hits, AnalysisSnapshot snapshot) {
        if (isFailed(snapshot)) {
            result.setScore(45);
            result.setGrade("D");
            return;
        }

        int score = 92;
        for (RuleHit hit : hits) {
            switch (hit.getCode()) {
                case "HIGH_LATENCY" -> score -= 12;
                case "ERROR_SPIKE" -> score -= 18;
                case "CPU_PRESSURE" -> score -= 8;
                case "MEMORY_PRESSURE" -> score -= 8;
                case "THROUGHPUT_REGRESSION" -> score -= 10;
                default -> {
                }
            }
        }
        score = Math.max(score, MIN_SCORE);
        result.setScore(score);
        result.setGrade(resolveGrade(score));
    }

    private void fillSummary(AnalysisComputationResult result, List<RuleHit> hits, AnalysisSnapshot snapshot) {
        if (isFailed(snapshot)) {
            result.setSummary("本次压测执行失败，建议先排查运行异常与依赖状态，再继续分析性能表现。");
            return;
        }

        if (hits.isEmpty()) {
            result.setSummary("本次压测整体表现稳定，未发现明显的高延迟、错误率突增、资源压力或吞吐退化风险。");
            return;
        }

        String titles = hits.stream()
                .map(RuleHit::getTitle)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("、"));
        result.setSummary("本次压测识别到以下主要风险：" + titles + "。建议优先处理高优先级问题，再继续观察整体性能趋势。");
    }

    // 当前持久化和展示层还在消费旧的 bottleneck/suggestion 实体，这里负责做一层兼容映射。
    private void fillLegacyItems(AnalysisComputationResult result, List<RuleHit> hits, AnalysisSnapshot snapshot) {
        for (RuleHit hit : hits) {
            BottleneckRecord bottleneck = new BottleneckRecord();
            bottleneck.setType(hit.getType());
            bottleneck.setReason(hit.getReason());
            bottleneck.setEvidence(hit.getEvidence());
            bottleneck.setSeverity(hit.getSeverity());
            bottleneck.setTimePoint(resolveTimePoint(hit, snapshot));
            result.getBottlenecks().add(bottleneck);

            AnalysisSuggestion suggestion = new AnalysisSuggestion();
            suggestion.setPriority(hit.getPriority());
            suggestion.setTitle(hit.getSuggestionTitle());
            suggestion.setDetail(hit.getSuggestionDetail());
            result.getSuggestions().add(suggestion);
        }
    }

    // 时间点目前更偏报告展示标签，而不是真正精确定位到秒级时序点。
    private String resolveTimePoint(RuleHit hit, AnalysisSnapshot snapshot) {
        return switch (hit.getCode()) {
            case "FAILED_RUN" -> "运行结束";
            case "THROUGHPUT_REGRESSION" -> "基线对比";
            case "HIGH_LATENCY", "ERROR_SPIKE", "CPU_PRESSURE", "MEMORY_PRESSURE" -> "运行摘要";
            default -> snapshot.getContext() != null && snapshot.getContext().getStartTime() != null
                    ? snapshot.getContext().getStartTime().toLocalTime().toString()
                    : "分析结果";
        };
    }

    private String resolveGrade(int score) {
        if (score >= 90) {
            return "A";
        }
        if (score >= 80) {
            return "B";
        }
        if (score >= 70) {
            return "C";
        }
        return "D";
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
