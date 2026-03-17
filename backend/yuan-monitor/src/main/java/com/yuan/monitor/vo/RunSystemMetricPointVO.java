package com.yuan.monitor.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RunSystemMetricPointVO {
    /** 采样时间 */
    private LocalDateTime ts;

    /** 当前秒 CPU 使用率 */
    private BigDecimal cpu;

    /** 当前秒内存使用率 */
    private BigDecimal memory;
}
