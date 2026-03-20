package com.yuan.auth.controller;

import com.yuan.auth.dto.UserCreateDTO;
import com.yuan.auth.dto.UserStatusUpdateDTO;
import com.yuan.auth.service.UserService;
import com.yuan.auth.vo.SystemUserVO;
import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import jakarta.validation.Valid;
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
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public R<List<SystemUserVO>> listUsers(
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_USER_READ)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_USER_READ);
        }
        return userService.listUsers();
    }

    @PostMapping("/users")
    public R<Long> createUser(
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @Valid @RequestBody UserCreateDTO request
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_USER_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_USER_WRITE);
        }
        return userService.createUser(request);
    }

    @PutMapping("/users/{userId}/status")
    public R<Void> updateUserStatus(
            @RequestHeader("X-User-Id") Long operatorUserId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateDTO request
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.SYSTEM_USER_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.SYSTEM_USER_WRITE);
        }
        return userService.updateUserStatus(operatorUserId, userId, request);
    }
}
