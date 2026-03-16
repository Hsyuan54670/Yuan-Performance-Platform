package com.yuan.analysis.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MetricFeature {
    /** 峰值 QPS */
    private BigDecimal peakQps;

    /** 峰值 P99 */
    private BigDecimal peakP99;

    /** 峰值错误率 */
    private BigDecimal peakErrorRate;

    /** 峰值 CPU 使用率 */
    private BigDecimal peakCpu;

    /** 峰值内存使用率 */
    private BigDecimal peakMemory;

    /** QPS 趋势斜率，可用于判断吞吐是否衰减 */
    private BigDecimal qpsTrend;

    /** P99 趋势斜率，可用于判断延迟是否恶化 */
    private BigDecimal p99Trend;

    /** 内存趋势斜率，可用于判断是否存在持续上涨 */
    private BigDecimal memoryTrend;

    /** 高延迟持续秒数 */
    private Integer highLatencySeconds;

    /** 错误率突刺持续秒数 */
    private Integer errorSpikeSeconds;

    /** 高 CPU 持续秒数 */
    private Integer highCpuSeconds;

    /** 高内存持续秒数 */
    private Integer highMemorySeconds;
}
