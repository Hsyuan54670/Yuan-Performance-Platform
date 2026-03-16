package com.yuan.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AnalysisSnapshot {
    private Long userId;
    private Long taskId;
    private Long runId;
    private String finalStatus;
    private Integer alertEvidenceCount;
    private List<AlertEvidenceItem> alertEvidence = new ArrayList<>();
}
