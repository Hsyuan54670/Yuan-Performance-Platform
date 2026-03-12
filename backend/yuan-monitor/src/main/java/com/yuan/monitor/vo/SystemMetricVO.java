package com.yuan.monitor.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SystemMetricVO {
    private BigDecimal cpu;
    private BigDecimal memory;
    private BigDecimal disk;
    private BigDecimal networkIn;
    private BigDecimal networkOut;
}
