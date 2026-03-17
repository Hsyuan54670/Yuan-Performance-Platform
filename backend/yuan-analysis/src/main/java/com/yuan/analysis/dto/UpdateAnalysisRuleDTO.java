package com.yuan.analysis.dto;

import lombok.Data;

@Data
public class UpdateAnalysisRuleDTO {
    private String ruleType;
    private String name;
    private String expression;
    private String instruction;
    private String bottleneckType;
    private String severity;
    private String priority;
    private Boolean enabled;
}
