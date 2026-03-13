package com.yuan.monitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAlertRuleDTO {
    @NotBlank
    String name;
    @NotBlank
    String metric;
    @NotBlank
    String op;
    @NotNull
    BigDecimal threshold;
    @NotBlank
    String level;
    @NotNull
    Boolean enabled;


}
