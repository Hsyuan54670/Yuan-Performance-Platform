package com.yuan.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoleMenuBindingDTO {
    private List<Long> menuIds = new ArrayList<>();
}
