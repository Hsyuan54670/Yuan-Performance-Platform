package com.yuan.analysis.ai.model;

import lombok.Data;

@Data
public class AiBottleneckInsight {

    private String time;
    private String type;
    private String reason;
    private String evidence;
    private String severity;
}
