package com.yuan.auth.vo;

import lombok.Data;

@Data
public class SystemUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private String status;
}
