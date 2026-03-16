package com.yuan.analysis.model;

import com.yuan.analysis.entity.AnalysisSuggestion;
import com.yuan.analysis.entity.BottleneckRecord;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AnalysisComputationResult {
    /** 报告评级，例如 A / B / C / D */
    private String grade;

    /** 报告评分，范围建议为 0-100 */
    private Integer score;

    /** 本次分析摘要 */
    private String summary;

    /** 结构化规则命中结果，后续既可用于落库也可用于拼装 AI Prompt */
    private List<RuleHit> ruleHits = new ArrayList<>();

    /** 当前兼容层的瓶颈列表，便于平滑迁移现有实现 */
    private List<BottleneckRecord> bottlenecks = new ArrayList<>();

    /** 当前兼容层的建议列表，便于平滑迁移现有实现 */
    private List<AnalysisSuggestion> suggestions = new ArrayList<>();
}
