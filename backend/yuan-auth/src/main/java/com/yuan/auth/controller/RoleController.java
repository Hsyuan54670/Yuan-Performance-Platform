package com.yuan.auth.controller;

import com.yuan.auth.dto.RoleMenuBindingDTO;
import com.yuan.auth.dto.RolePermissionBindingDTO;
import com.yuan.auth.dto.RoleUpsertDTO;
import com.yuan.auth.service.RoleService;
import com.yuan.auth.vo.SystemRoleVO;
import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    public R<List<SystemRoleVO>> listRoles(@RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_ROLE_READ)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_ROLE_READ);
        }
        return roleService.listRoles();
    }

    @PostMapping("/roles")
    public R<Long> createRole(
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @Valid @RequestBody RoleUpsertDTO request
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_ROLE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_ROLE_WRITE);
        }
        return roleService.createRole(request);
    }

    @PutMapping("/roles/{roleId}")
    public R<Void> updateRole(
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long roleId,
            @Valid @RequestBody RoleUpsertDTO request
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_ROLE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_ROLE_WRITE);
        }
        return roleService.updateRole(roleId, request);
    }

    @DeleteMapping("/roles/{roleId}")
    public R<Void> deleteRole(
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long roleId
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_ROLE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_ROLE_WRITE);
        }
        return roleService.deleteRole(roleId);
    }

    @GetMapping("/roles/{roleId}/permissions")
    public R<List<Long>> getRolePermissionIds(
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long roleId
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_ROLE_READ)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_ROLE_READ);
        }
        return roleService.getRolePermissionIds(roleId);
    }

    @PutMapping("/roles/{roleId}/permissions")
    public R<Void> updateRolePermissionIds(
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long roleId,
            @RequestBody RolePermissionBindingDTO request
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_ROLE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_ROLE_WRITE);
        }
        return roleService.updateRolePermissionIds(roleId, request);
    }

    @GetMapping("/roles/{roleId}/menus")
    public R<List<Long>> getRoleMenuIds(
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long roleId
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_ROLE_READ)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_ROLE_READ);
        }
        return roleService.getRoleMenuIds(roleId);
    }

    @PutMapping("/roles/{roleId}/menus")
    public R<Void> updateRoleMenuIds(
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long roleId,
            @RequestBody RoleMenuBindingDTO request
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_ROLE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_ROLE_WRITE);
        }
        return roleService.updateRoleMenuIds(roleId, request);
    }
}
