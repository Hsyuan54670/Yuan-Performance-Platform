package com.yuan.api.monitor.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RunSystemMetricPointDTO {
    /** 采样时间 */
    private LocalDateTime ts;

    /** 当前秒 CPU 平均使用率 */
    private BigDecimal cpu;

    /** 当前秒 CPU 峰值使用率 */
    private BigDecimal cpuMax;

    /** 当前秒内存平均使用率 */
    private BigDecimal memory;

    /** 当前秒内存峰值使用率 */
    private BigDecimal memoryMax;

    /** 该秒聚合到的原始样本数 */
    private Integer sampleCount;

    /** 是否为空桶（该秒没有采到有效资源样本） */
    private Boolean missing;
}
