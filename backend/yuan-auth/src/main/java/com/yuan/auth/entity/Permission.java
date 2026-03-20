package com.yuan.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auth_permission")
public class Permission {
    private Long id;
    private String code;
    private String name;
    private String module;
    private String description;
    private LocalDateTime createdAt;
}
