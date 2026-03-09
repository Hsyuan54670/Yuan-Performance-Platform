package com.yuan.auth.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("auth_role")
public class Role {
    private Long id;
    private String code;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}