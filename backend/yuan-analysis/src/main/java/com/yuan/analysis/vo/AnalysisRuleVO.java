package com.yuan.analysis.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnalysisRuleVO {
    private Long id;
    private String ruleType;
    private String name;
    private String expression;
    private String instruction;
    private String bottleneckType;
    private String severity;
    private String priority;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
