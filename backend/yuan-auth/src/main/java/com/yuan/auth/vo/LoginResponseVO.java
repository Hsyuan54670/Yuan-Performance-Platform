package com.yuan.auth.vo;


import lombok.Data;

@Data
public class LoginResponseVO {
    private String token;
    private String refreshToken;
    private UserInfoVO user;
}