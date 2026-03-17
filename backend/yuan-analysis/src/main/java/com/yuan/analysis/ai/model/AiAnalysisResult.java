package com.yuan.analysis.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiAnalysisResult {

    private String summary;
    private Integer scoreAdjustment;
    private String rationale;
    private String model;
    private boolean fallback;
    private List<AiBottleneckInsight> bottlenecks = new ArrayList<>();
    private List<AiSuggestionInsight> suggestions = new ArrayList<>();
}
