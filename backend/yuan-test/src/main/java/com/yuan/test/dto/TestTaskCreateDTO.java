package com.yuan.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TestTaskCreateDTO {

    @NotNull
    private Long planId;

    @NotNull
    private Long sceneId;
}
