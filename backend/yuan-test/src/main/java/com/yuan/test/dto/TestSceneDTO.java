package com.yuan.test.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TestSceneDTO {

    @NotBlank
    private String name;

    @Valid
    @NotEmpty
    private List<TestSceneStepDTO> steps;
}
