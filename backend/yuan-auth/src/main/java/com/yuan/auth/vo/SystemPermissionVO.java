package com.yuan.auth.vo;

import lombok.Data;

@Data
public class SystemPermissionVO {
    private Long id;
    private String code;
    private String name;
    private String module;
    private String description;
}
