package com.yuan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserStatusUpdateDTO {

    @NotBlank(message = "用户状态不能为空")
    private String status;
}
