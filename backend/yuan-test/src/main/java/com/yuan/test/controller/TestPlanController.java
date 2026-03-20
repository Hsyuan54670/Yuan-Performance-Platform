package com.yuan.test.controller;

import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import com.yuan.test.dto.TestPlanDTO;
import com.yuan.test.service.TestPlanService;
import com.yuan.test.vo.TestPlanVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/test")
public class TestPlanController {

    @Autowired
    TestPlanService testPlanService;

    @GetMapping("/plans")
    public R<List<TestPlanVO>> plans(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_PLAN_READ)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_PLAN_READ);
        }
        return testPlanService.plans(userId);
    }

    @PostMapping("/plans")
    public R<Long> create(
            @RequestBody @Valid TestPlanDTO request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_PLAN_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_PLAN_WRITE);
        }
        return testPlanService.create(request, userId);
    }

    @PutMapping("/plans/{id}")
    public R<Void> update(
            @PathVariable Long id,
            @RequestBody @Valid TestPlanDTO request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_PLAN_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_PLAN_WRITE);
        }
        return testPlanService.update(id, request, userId);
    }

    @DeleteMapping("/plans/{id}")
    public R<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_PLAN_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_PLAN_WRITE);
        }
        return testPlanService.delete(id, userId);
    }
}
