package com.yuan.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RolePermissionBindingDTO {
    private List<Long> permissionIds = new ArrayList<>();
}
