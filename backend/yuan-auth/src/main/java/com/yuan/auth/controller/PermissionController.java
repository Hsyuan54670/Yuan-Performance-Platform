package com.yuan.auth.controller;

import com.yuan.auth.service.PermissionService;
import com.yuan.auth.vo.SystemPermissionVO;
import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/permissions")
    public R<List<SystemPermissionVO>> listPermissions(@RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_ROLE_READ)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_ROLE_READ);
        }
        return permissionService.listPermissions();
    }
}
