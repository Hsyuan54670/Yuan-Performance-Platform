package com.yuan.analysis.llm;

import com.yuan.analysis.ai.condition.FallbackProviderCondition;
import com.yuan.analysis.ai.config.AnalysisAiProperties;
import com.yuan.analysis.ai.model.AiAnalysisInput;
import com.yuan.analysis.ai.model.AiAnalysisResult;
import com.yuan.analysis.ai.model.AiBottleneckInsight;
import com.yuan.analysis.ai.model.AiRuleInstruction;
import com.yuan.analysis.ai.model.AiSuggestionInsight;
import com.yuan.analysis.model.BaselineSnapshot;
import com.yuan.analysis.model.MetricFeature;
import com.yuan.analysis.model.MetricSummary;
import com.yuan.analysis.model.RuleHit;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Conditional(FallbackProviderCondition.class)
@Service
public class FallbackLlmClient implements LlmClient {

    private final AnalysisAiProperties properties;

    public FallbackLlmClient(AnalysisAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public AiAnalysisResult analyze(String prompt, AiAnalysisInput input) {
        AiAnalysisResult result = new AiAnalysisResult();
        result.setFallback(true);
        result.setModel("fallback-heuristic-v1");
        result.setScoreAdjustment(resolveScoreAdjustment(input));
        result.setSummary(buildSummary(input));
        result.setRationale("Prompt prepared with fallback client, length=" + (prompt == null ? 0 : prompt.length()));
        buildCorrelationBottlenecks(input, result);
        buildSuggestions(input, result);
        return result;
    }

    private Integer resolveScoreAdjustment(AiAnalysisInput input) {
        if ("FAILED".equalsIgnoreCase(input.getFinalStatus())) {
            return 0;
        }

        long criticalCount = input.getRuleHits().stream()
                .filter(hit -> "CRITICAL".equalsIgnoreCase(hit.getSeverity()))
                .count();
        long highCount = input.getRuleHits().stream()
                .filter(hit -> "HIGH".equalsIgnoreCase(hit.getSeverity()))
                .count();

        int adjustment = 0;
        if (criticalCount >= 2) {
            adjustment -= 6;
        } else if (criticalCount == 1) {
            adjustment -= 4;
        } else if (highCount >= 2) {
            adjustment -= 3;
        } else if (highCount == 1) {
            adjustment -= 1;
        }

        if (input.getRuleHits().isEmpty() && isStableComparedWithBaseline(input.getSnapshot().getBaseline(), input.getSnapshot().getSummary())) {
            adjustment += 2;
        }

        int maxAdjustment = Math.max(properties.getMaxScoreAdjustment(), 0);
        return Math.max(-maxAdjustment, Math.min(maxAdjustment, adjustment));
    }

    private boolean isStableComparedWithBaseline(BaselineSnapshot baseline, MetricSummary summary) {
        if (baseline == null || summary == null) {
            return false;
        }
        BigDecimal qpsChange = safe(baseline.getQpsChangeRate());
        BigDecimal p99Change = safe(baseline.getP99ChangeRate());
        BigDecimal errorChange = safe(baseline.getErrorRateChangeRate());
        return qpsChange.compareTo(BigDecimal.ZERO) >= 0
                && p99Change.compareTo(BigDecimal.ZERO) <= 0
                && errorChange.compareTo(BigDecimal.ZERO) <= 0
                && safe(summary.getErrorRate()).compareTo(BigDecimal.valueOf(1)) <= 0;
    }

    private String buildSummary(AiAnalysisInput input) {
        if ("FAILED".equalsIgnoreCase(input.getFinalStatus())) {
            return "AI 解释认为本次结果首先体现为执行失败，建议优先确认 JMeter 执行状态、依赖可用性和环境资源，再继续讨论性能瓶颈。";
        }

        List<RuleHit> hits = input.getRuleHits();
        MetricSummary summary = input.getSnapshot().getSummary();
        MetricFeature feature = input.getSnapshot().getFeature();
        BaselineSnapshot baseline = input.getSnapshot().getBaseline();

        if (hits.isEmpty()) {
            if (baseline != null && baseline.getBaselineRunId() != null) {
                return String.format(
                        "AI 解释认为本次压测整体稳定，和历史基线相比没有看到明显回归；当前 avgQps=%s、p99=%sms、errorRate=%s%%，更像是一次可以沉淀为参考样本的健康运行。",
                        text(summary.getAvgQps()),
                        text(summary.getP99()),
                        text(summary.getErrorRate())
                );
            }
            return String.format(
                    "AI 解释认为本次压测整体平稳，当前 avgQps=%s、p99=%sms、errorRate=%s%%，资源侧 avgCpu=%s%%、avgMemory=%s%% 也没有出现明显异常，可以先将这次运行作为后续 AI 对比的参考样本。",
                    text(summary.getAvgQps()),
                    text(summary.getP99()),
                    text(summary.getErrorRate()),
                    text(summary.getAvgCpu()),
                    text(summary.getAvgMemory())
            );
        }

        Set<String> codes = hits.stream().map(RuleHit::getCode).collect(Collectors.toSet());
        if (codes.contains("HIGH_LATENCY") && codes.contains("CPU_PRESSURE")) {
            return String.format(
                    "AI 解释认为本次性能问题更偏向应用内部处理压力：高延迟与 CPU 压力同时出现，说明线程竞争、热点计算或 GC 抖动正在放大尾延迟；当前 p99=%sms、avgCpu=%s%%、peakCpu=%s%%。",
                    text(summary.getP99()),
                    text(summary.getAvgCpu()),
                    text(feature.getPeakCpu())
            );
        }

        if (codes.contains("HIGH_LATENCY") && codes.contains("ERROR_SPIKE")) {
            return String.format(
                    "AI 解释认为本次问题更像是慢依赖与稳定性相互放大：高延迟伴随错误率上升，通常意味着下游超时、连接池耗尽或线程池排队开始影响成功率；当前 p99=%sms、errorRate=%s%%。",
                    text(summary.getP99()),
                    text(summary.getErrorRate())
            );
        }

        if (codes.contains("THROUGHPUT_REGRESSION")) {
            return String.format(
                    "AI 解释认为本次运行的核心变化是承载能力下降：avgQps=%s，基线 avgQps=%s，回归通常需要结合延迟、错误率和资源利用率一起判断真正的限制点。",
                    text(summary.getAvgQps()),
                    text(baseline == null ? BigDecimal.ZERO : baseline.getBaselineQps())
            );
        }

        String hitTitles = hits.stream().map(RuleHit::getTitle).distinct().collect(Collectors.joining("、"));
        return "AI 解释认为本次压测存在多维度风险，当前最值得关注的现象包括：" + hitTitles + "。建议按优先级先处理稳定性和高负载问题，再回看吞吐与基线偏差。";
    }

    private void buildCorrelationBottlenecks(AiAnalysisInput input, AiAnalysisResult result) {
        Set<String> codes = input.getRuleHits().stream().map(RuleHit::getCode).collect(Collectors.toSet());
        MetricSummary summary = input.getSnapshot().getSummary();
        MetricFeature feature = input.getSnapshot().getFeature();

        if (codes.contains("HIGH_LATENCY") && codes.contains("CPU_PRESSURE")) {
            AiBottleneckInsight insight = new AiBottleneckInsight();
            insight.setTime("AI 归因");
            insight.setType("AI_CORRELATION");
            insight.setSeverity("HIGH");
            insight.setReason("高延迟与 CPU 压力同时出现，更像是应用内部计算、锁竞争或线程池拥塞导致的尾延迟放大。");
            insight.setEvidence("p99=" + text(summary.getP99()) + "ms, avgCpu=" + text(summary.getAvgCpu()) + "%, peakCpu=" + text(feature.getPeakCpu()) + "%");
            result.getBottlenecks().add(insight);
        }

        if (codes.contains("HIGH_LATENCY") && codes.contains("ERROR_SPIKE")) {
            AiBottleneckInsight insight = new AiBottleneckInsight();
            insight.setTime("AI 归因");
            insight.setType("AI_CORRELATION");
            insight.setSeverity("CRITICAL");
            insight.setReason("高延迟叠加错误率升高，说明超时、熔断或依赖不稳定已经从性能问题演化为可用性问题。");
            insight.setEvidence("p99=" + text(summary.getP99()) + "ms, errorRate=" + text(summary.getErrorRate()) + "%");
            result.getBottlenecks().add(insight);
        }

        if (codes.contains("MEMORY_PRESSURE") && codes.contains("ERROR_SPIKE")) {
            AiBottleneckInsight insight = new AiBottleneckInsight();
            insight.setTime("AI 归因");
            insight.setType("AI_CORRELATION");
            insight.setSeverity("HIGH");
            insight.setReason("内存压力与错误率同时升高，建议重点关注堆膨胀、缓存失控或 GC 停顿对请求成功率的影响。");
            insight.setEvidence("avgMemory=" + text(summary.getAvgMemory()) + "%, peakMemory=" + text(feature.getPeakMemory()) + "%");
            result.getBottlenecks().add(insight);
        }
    }

    private void buildSuggestions(AiAnalysisInput input, AiAnalysisResult result) {
        Set<String> codes = input.getRuleHits().stream().map(RuleHit::getCode).collect(Collectors.toSet());

        if (codes.contains("HIGH_LATENCY") && codes.contains("CPU_PRESSURE")) {
            AiSuggestionInsight suggestion = new AiSuggestionInsight();
            suggestion.setPriority("P1");
            suggestion.setTitle("补充 CPU profile 与线程栈证据");
            suggestion.setDetail("建议在下一轮压测同步采集 CPU profile、热点线程栈和 GC 指标，确认尾延迟究竟来自热点逻辑、锁竞争还是线程池排队。");
            result.getSuggestions().add(suggestion);
        }

        if (codes.contains("HIGH_LATENCY") && codes.contains("ERROR_SPIKE")) {
            AiSuggestionInsight suggestion = new AiSuggestionInsight();
            suggestion.setPriority("P0");
            suggestion.setTitle("对齐超时、重试与连接池配置");
            suggestion.setDetail("建议优先核对应用超时、重试、连接池上限和下游 SLA，避免慢请求继续放大为错误率问题。");
            result.getSuggestions().add(suggestion);
        }

        if (codes.contains("MEMORY_PRESSURE")) {
            AiSuggestionInsight suggestion = new AiSuggestionInsight();
            suggestion.setPriority("P1");
            suggestion.setTitle("补充 GC 与堆快照分析");
            suggestion.setDetail("建议下一轮运行同步留存 GC 日志与堆快照，验证是否存在缓存增长、对象滞留或批量加载导致的内存堆积。");
            result.getSuggestions().add(suggestion);
        }

        if (input.getRuleHits().isEmpty()) {
            AiSuggestionInsight suggestion = new AiSuggestionInsight();
            suggestion.setPriority("P2");
            suggestion.setTitle("将本次结果沉淀为健康基线");
            suggestion.setDetail("本次运行没有发现明显风险，建议将关键摘要、并发配置和环境版本一并记录，作为后续 AI 对比和回归判断的参考样本。");
            result.getSuggestions().add(suggestion);
        }

        if (input.getAiRules() != null && !input.getAiRules().isEmpty()) {
            String aiFocus = input.getAiRules().stream()
                    .map(AiRuleInstruction::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .collect(Collectors.joining("、"));
            AiSuggestionInsight suggestion = new AiSuggestionInsight();
            suggestion.setPriority("P2");
            suggestion.setTitle("延续 AI 关注点进行下一轮验证");
            suggestion.setDetail("当前用户启用了这些 AI 规则：" + aiFocus + "。下一轮压测建议围绕这些关注点补充证据，提升解释与归因的稳定性。");
            result.getSuggestions().add(suggestion);
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String text(BigDecimal value) {
        return safe(value).stripTrailingZeros().toPlainString();
    }
}
