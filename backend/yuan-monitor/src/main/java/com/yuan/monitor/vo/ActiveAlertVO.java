package com.yuan.monitor.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActiveAlertVO {
    private Long ruleId;
    private Long taskId;
    private Long runId;
    private String ruleName;
    private String metric;
    private String op;
    private BigDecimal threshold;
    private String level;
    private BigDecimal latestValue;
    private LocalDateTime latestTriggeredAt;
}
