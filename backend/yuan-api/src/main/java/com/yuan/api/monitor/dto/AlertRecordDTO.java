package com.yuan.api.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertRecordDTO {
    private Long id;
    private Long taskId;
    private Long runId;
    private String ruleName;
    private String level;
    private String eventType;
    private BigDecimal currentValue;
    private LocalDateTime createdAt;
}
