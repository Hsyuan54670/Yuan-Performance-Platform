package com.yuan.analysis.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.analysis.ai.model.AiAnalysisInput;
import com.yuan.analysis.ai.model.AiRuleInstruction;
import com.yuan.analysis.model.MetricSeriesBundle;
import com.yuan.analysis.model.RuleHit;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class AnalysisPromptBuilder {

    private static final String TEMPLATE_PATH = "prompts/performance-analysis.prompt.md";
    private final ObjectMapper objectMapper;

    public AnalysisPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String build(AiAnalysisInput input) {
        String template = loadTemplate();
        return template
                .replace("{{AI_RULES}}", renderAiRules(input))
                .replace("{{ANALYSIS_INPUT_JSON}}", renderInputJson(input));
    }

    private String loadTemplate() {
        Resource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream stream = resource.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load analysis prompt template", e);
        }
    }

    private String renderAiRules(AiAnalysisInput input) {
        if (input.getAiRules() == null || input.getAiRules().isEmpty()) {
            return "- 当前用户没有启用额外 AI 规则。";
        }
        return input.getAiRules().stream()
                .map(this::renderAiRule)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String renderAiRule(AiRuleInstruction rule) {
        return String.format(
                "- %s | priority=%s | severity=%s | focus=%s | instruction=%s",
                safe(rule.getName()),
                safe(rule.getPriority()),
                safe(rule.getSeverity()),
                safe(rule.getBottleneckType()),
                safe(rule.getInstruction())
        );
    }

    private String renderInputJson(AiAnalysisInput input) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toPromptPayload(input));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AI analysis input", e);
        }
    }

    // 给模型的是“事实包”，不是整个内部对象，避免把规则建议原文直接喂给模型后被复述出来。
    private Map<String, Object> toPromptPayload(AiAnalysisInput input) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("run", Map.of(
                "userId", input.getUserId(),
                "taskId", input.getTaskId(),
                "runId", input.getRunId(),
                "finalStatus", input.getFinalStatus()
        ));
        payload.put("ruleBaseline", Map.of(
                "score", input.getBaseScore(),
                "grade", input.getBaseGrade(),
                "summary", input.getBaseSummary()
        ));

        if (input.getSnapshot() != null) {
            payload.put("context", input.getSnapshot().getContext());
            payload.put("summary", input.getSnapshot().getSummary());
            payload.put("feature", input.getSnapshot().getFeature());
            payload.put("baseline", input.getSnapshot().getBaseline());
            payload.put("alertEvidenceCount", input.getSnapshot().getAlertEvidenceCount());
            payload.put("alertEvidence", input.getSnapshot().getAlertEvidence());
            payload.put("series", toPromptSeries(input.getSnapshot().getSeries()));
        }

        List<Map<String, Object>> ruleFindings = input.getRuleHits().stream()
                .map(this::toPromptRuleFinding)
                .toList();
        List<Map<String, Object>> ruleSuggestionsToAvoid = input.getRuleHits().stream()
                .map(this::toPromptRuleSuggestion)
                .filter(Objects::nonNull)
                .toList();

        payload.put("ruleFindings", ruleFindings);
        payload.put("ruleSuggestionsToAvoid", ruleSuggestionsToAvoid);
        return payload;
    }

    // 当前直接把完整秒级时序点给模型，方便识别抖动、阶段性恶化和恢复拐点；后续若时长变大，再评估压缩策略。
    private Map<String, Object> toPromptSeries(MetricSeriesBundle series) {
        if (series == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("samplingIntervalSeconds", series.getSamplingIntervalSeconds());
        payload.put("pointCount", series.getPoints() == null ? 0 : series.getPoints().size());
        payload.put("points", series.getPoints());
        return payload;
    }

    private Map<String, Object> toPromptRuleFinding(RuleHit hit) {
        Map<String, Object> finding = new LinkedHashMap<>();
        finding.put("code", hit.getCode());
        finding.put("type", hit.getType());
        finding.put("severity", hit.getSeverity());
        finding.put("title", hit.getTitle());
        finding.put("reason", hit.getReason());
        finding.put("evidence", hit.getEvidence());
        finding.put("priority", hit.getPriority());
        return finding;
    }

    private Map<String, Object> toPromptRuleSuggestion(RuleHit hit) {
        if (!StringUtils.hasText(hit.getSuggestionTitle()) && !StringUtils.hasText(hit.getSuggestionDetail())) {
            return null;
        }
        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("priority", hit.getPriority());
        suggestion.put("title", hit.getSuggestionTitle());
        suggestion.put("detail", hit.getSuggestionDetail());
        return suggestion;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
