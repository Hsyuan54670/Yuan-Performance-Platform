package com.yuan.api.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RunSystemMetricSummaryDTO {
    /** 运行ID */
    private Long runId;

    /** 平均 CPU 使用率 */
    private BigDecimal avgCpu;

    /** 平均内存使用率 */
    private BigDecimal avgMemory;

    /** 峰值 CPU 使用率 */
    private BigDecimal peakCpu;

    /** 峰值内存使用率 */
    private BigDecimal peakMemory;

    /** 高 CPU 持续秒数 */
    private Integer highCpuSeconds;

    /** 高内存持续秒数 */
    private Integer highMemorySeconds;
}
