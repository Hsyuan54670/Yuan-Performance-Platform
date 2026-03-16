package com.yuan.monitor.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertRecordVO {
    private Long id;
    private Long taskId;
    private Long runId;
    private String ruleName;
    private String metric;
    private String level;
    private BigDecimal currentValue;
    private String eventType;
    private LocalDateTime createdAt;
}
