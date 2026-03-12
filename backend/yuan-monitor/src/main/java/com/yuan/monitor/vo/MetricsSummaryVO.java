package com.yuan.monitor.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MetricsSummaryVO {
    private Long taskId;
    private Long runId;
    private String status;
    private BigDecimal qps;
    private BigDecimal p99;
    private BigDecimal errorRate;
    private LocalDateTime timestamp;

}