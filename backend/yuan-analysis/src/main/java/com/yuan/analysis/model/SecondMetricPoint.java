package com.yuan.analysis.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SecondMetricPoint {
    /** 采样时间 */
    private LocalDateTime ts;

    /** 当前秒 QPS */
    private BigDecimal qps;

    /** 当前秒 P50 */
    private BigDecimal p50;

    /** 当前秒 P90 */
    private BigDecimal p90;

    /** 当前秒 P99 */
    private BigDecimal p99;

    /** 当前秒错误率 */
    private BigDecimal errorRate;

    /** 当前秒 CPU 使用率 */
    private BigDecimal cpu;

    /** 当前秒内存使用率 */
    private BigDecimal memory;
}
