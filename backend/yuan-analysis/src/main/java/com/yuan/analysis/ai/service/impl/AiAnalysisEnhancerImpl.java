package com.yuan.analysis.ai.service.impl;

import com.yuan.analysis.ai.config.AnalysisAiProperties;
import com.yuan.analysis.ai.model.AiAnalysisInput;
import com.yuan.analysis.ai.model.AiAnalysisResult;
import com.yuan.analysis.ai.model.AiBottleneckInsight;
import com.yuan.analysis.ai.model.AiSuggestionInsight;
import com.yuan.analysis.ai.prompt.AnalysisPromptBuilder;
import com.yuan.analysis.ai.service.AiAnalysisEnhancer;
import com.yuan.analysis.ai.service.AiRuleProvider;
import com.yuan.analysis.entity.AnalysisSuggestion;
import com.yuan.analysis.entity.BottleneckRecord;
import com.yuan.analysis.llm.LlmClient;
import com.yuan.analysis.model.AnalysisComputationResult;
import com.yuan.analysis.model.AnalysisSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
public class AiAnalysisEnhancerImpl implements AiAnalysisEnhancer {

    private final AnalysisAiProperties properties;
    private final AiRuleProvider aiRuleProvider;
    private final AnalysisPromptBuilder promptBuilder;
    private final LlmClient llmClient;

    public AiAnalysisEnhancerImpl(
            AnalysisAiProperties properties,
            AiRuleProvider aiRuleProvider,
            AnalysisPromptBuilder promptBuilder,
            LlmClient llmClient
    ) {
        this.properties = properties;
        this.aiRuleProvider = aiRuleProvider;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
    }

    @Override
    public AnalysisComputationResult enhance(Long userId, AnalysisSnapshot snapshot, AnalysisComputationResult baseResult) {
        seedBaseResult(baseResult);
        if (!properties.isEnabled()) {
            baseResult.setSource("DATA_RULE");
            return baseResult;
        }

        AiAnalysisInput input = buildInput(userId, snapshot, baseResult);
        String prompt = promptBuilder.build(input);
        AiAnalysisResult aiResult = llmClient.analyze(prompt, input);
        if (aiResult == null) {
            baseResult.setSource("DATA_RULE");
            return baseResult;
        }

        if (StringUtils.hasText(aiResult.getSummary())) {
            baseResult.setSummary(aiResult.getSummary());
        }
        baseResult.setScore(resolveFinalScore(baseResult.getBaseScore(), aiResult.getScoreAdjustment()));
        baseResult.setGrade(resolveGrade(baseResult.getScore()));
        baseResult.setSource("DATA_RULE_AI");
        mergeBottlenecks(baseResult, aiResult);
        mergeSuggestions(baseResult, aiResult);
        return baseResult;
    }

    private void seedBaseResult(AnalysisComputationResult baseResult) {
        baseResult.setBaseScore(baseResult.getScore());
        baseResult.setBaseGrade(baseResult.getGrade());
        baseResult.setBaseSummary(baseResult.getSummary());
        if (!StringUtils.hasText(baseResult.getSource())) {
            baseResult.setSource("DATA_RULE");
        }
    }

    private AiAnalysisInput buildInput(Long userId, AnalysisSnapshot snapshot, AnalysisComputationResult baseResult) {
        AiAnalysisInput input = new AiAnalysisInput();
        input.setUserId(userId);
        input.setTaskId(snapshot.getTaskId());
        input.setRunId(snapshot.getRunId());
        input.setFinalStatus(snapshot.getFinalStatus());
        input.setBaseScore(baseResult.getBaseScore());
        input.setBaseGrade(baseResult.getBaseGrade());
        input.setBaseSummary(baseResult.getBaseSummary());
        input.setSnapshot(snapshot);
        input.setRuleHits(baseResult.getRuleHits());
        input.setAiRules(aiRuleProvider.listEnabledRules(userId));
        return input;
    }

    private int resolveFinalScore(Integer baseScore, Integer adjustment) {
        int score = baseScore == null ? 0 : baseScore;
        score += adjustment == null ? 0 : adjustment;
        return Math.max(0, Math.min(100, score));
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

    private void mergeBottlenecks(AnalysisComputationResult baseResult, AiAnalysisResult aiResult) {
        Set<String> fingerprints = new HashSet<>();
        baseResult.getBottlenecks().forEach(item -> fingerprints.add(item.getType() + "|" + item.getReason()));

        for (AiBottleneckInsight insight : aiResult.getBottlenecks()) {
            String fingerprint = safe(insight.getType()) + "|" + safe(insight.getReason());
            if (!fingerprints.add(fingerprint)) {
                continue;
            }
            BottleneckRecord record = new BottleneckRecord();
            record.setTimePoint(StringUtils.hasText(insight.getTime()) ? insight.getTime() : "AI 归因");
            record.setType(StringUtils.hasText(insight.getType()) ? insight.getType() : "AI_INSIGHT");
            record.setReason(insight.getReason());
            record.setEvidence(insight.getEvidence());
            record.setSeverity(StringUtils.hasText(insight.getSeverity()) ? insight.getSeverity() : "MEDIUM");
            baseResult.getBottlenecks().add(record);
        }
    }

    private void mergeSuggestions(AnalysisComputationResult baseResult, AiAnalysisResult aiResult) {
        Set<String> fingerprints = new HashSet<>();
        baseResult.getSuggestions().forEach(item -> fingerprints.add(item.getTitle() + "|" + item.getDetail()));

        for (AiSuggestionInsight insight : aiResult.getSuggestions()) {
            String fingerprint = safe(insight.getTitle()) + "|" + safe(insight.getDetail());
            if (!fingerprints.add(fingerprint)) {
                continue;
            }
            AnalysisSuggestion suggestion = new AnalysisSuggestion();
            suggestion.setPriority(StringUtils.hasText(insight.getPriority()) ? insight.getPriority() : "P2");
            suggestion.setTitle(insight.getTitle());
            suggestion.setDetail(insight.getDetail());
            baseResult.getSuggestions().add(suggestion);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
