package com.yuan.auth.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SystemMenuVO {
    private Long id;
    private String name;
    private String path;
    private List<SystemMenuVO> children = new ArrayList<>();
}
