package com.yuan.test.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TestPlanDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String targetUrl;

    @NotNull
    @Min(1)
    private Integer concurrency;

    @NotNull
    @Min(1)
    private Integer duration;

    @NotBlank
    private String rampType;
}
