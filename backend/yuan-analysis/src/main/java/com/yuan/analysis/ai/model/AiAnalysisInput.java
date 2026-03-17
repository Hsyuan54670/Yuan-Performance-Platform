package com.yuan.analysis.ai.model;

import com.yuan.analysis.model.AnalysisSnapshot;
import com.yuan.analysis.model.RuleHit;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiAnalysisInput {

    private Long userId;
    private Long taskId;
    private Long runId;
    private String finalStatus;
    private Integer baseScore;
    private String baseGrade;
    private String baseSummary;
    private AnalysisSnapshot snapshot;
    private List<RuleHit> ruleHits = new ArrayList<>();
    private List<AiRuleInstruction> aiRules = new ArrayList<>();
}
