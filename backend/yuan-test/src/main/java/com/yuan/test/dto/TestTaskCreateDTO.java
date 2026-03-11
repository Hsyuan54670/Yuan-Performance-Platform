package com.yuan.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestTaskCreateDTO {

    @NotBlank
    private Long planId;

    @NotBlank
    private Long sceneId;
}
