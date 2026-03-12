package com.yuan.monitor.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RealtimeMetricPushVO {
    private Long taskId;
    private Long runId;
    private BigDecimal qps;
    private BigDecimal p50;
    private BigDecimal p90;
    private BigDecimal p99;
    private BigDecimal errorRate;
    private LocalDateTime timestamp;
}
