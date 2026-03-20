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

    /** 当前秒 CPU 平均使用率 */
    private BigDecimal cpu;

    /** 当前秒 CPU 峰值使用率 */
    private BigDecimal cpuMax;

    /** 当前秒内存平均使用率 */
    private BigDecimal memory;

    /** 当前秒内存峰值使用率 */
    private BigDecimal memoryMax;

    /** 当前秒聚合到的资源原始样本数 */
    private Integer sampleCount;

    /** true 表示该秒只有占位桶，没有采到有效资源样本 */
    private Boolean missing;
}
