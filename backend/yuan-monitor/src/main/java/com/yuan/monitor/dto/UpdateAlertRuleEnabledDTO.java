package com.yuan.monitor.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class UpdateAlertRuleEnabledDTO {
    @NotNull
    Boolean enabled;
}
