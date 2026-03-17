package com.yuan.analysis.ai.model;

import lombok.Data;

@Data
public class AiRuleInstruction {

    private String name;
    private String instruction;
    private String bottleneckType;
    private String severity;
    private String priority;
}
