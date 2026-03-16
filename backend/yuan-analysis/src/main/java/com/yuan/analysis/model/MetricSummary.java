package com.yuan.analysis.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MetricSummary {
    /** 平均 QPS */
    private BigDecimal avgQps;

    /** P50 响应时间 */
    private BigDecimal p50;

    /** P90 响应时间 */
    private BigDecimal p90;

    /** P99 响应时间 */
    private BigDecimal p99;

    /** 错误率 */
    private BigDecimal errorRate;

    /** 平均 CPU 使用率 */
    private BigDecimal avgCpu;

    /** 平均内存使用率 */
    private BigDecimal avgMemory;
}
