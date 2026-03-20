package com.yuan.auth.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private List<SystemMenuVO> menus = new ArrayList<>();
}
