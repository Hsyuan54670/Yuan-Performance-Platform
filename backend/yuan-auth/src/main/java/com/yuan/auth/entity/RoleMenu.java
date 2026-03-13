package com.yuan.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auth_role_menu")
public class RoleMenu {

    private Long roleId;
    private Long menuId;
    private LocalDateTime createdAt;
}
