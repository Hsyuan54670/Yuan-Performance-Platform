package com.yuan.test.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TestSceneStepDTO {

    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String method;

    @NotBlank
    private String path;

    @NotNull
    @Min(1)
    private Integer weight;
}
