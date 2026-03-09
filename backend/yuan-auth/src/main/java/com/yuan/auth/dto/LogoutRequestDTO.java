package com.yuan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequestDTO {
    @NotBlank
    private String refreshToken;
}
