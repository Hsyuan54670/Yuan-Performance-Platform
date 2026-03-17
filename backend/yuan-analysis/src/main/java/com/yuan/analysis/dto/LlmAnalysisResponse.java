package com.yuan.analysis.dto;

import com.yuan.analysis.ai.model.AiBottleneckInsight;
import com.yuan.analysis.ai.model.AiSuggestionInsight;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class LlmAnalysisResponse {
    private String summary;
    private Integer scoreAdjustment;
    private String rationale;
    private List<AiBottleneckInsight> bottlenecks = new ArrayList<>();
    private List<AiSuggestionInsight> suggestions = new ArrayList<>();
}