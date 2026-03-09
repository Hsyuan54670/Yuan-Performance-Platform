package com.yuan.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("auth_user_role")
public class UserRole {
    private Long userId;
    private Long roleId;
    private LocalDateTime createdAt;
}
