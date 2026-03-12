package com.yuan.monitor.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlertRuleVO {
    private Long id;
    private String name;
    private String metric;
    private String op;
    private BigDecimal threshold;
    private String level;
    private Boolean enabled;
}
