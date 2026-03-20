package com.yuan.test.controller;

import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import com.yuan.test.dto.TestSceneDTO;
import com.yuan.test.service.TestSceneService;
import com.yuan.test.vo.TestSceneVO;
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
@RequestMapping("/test")
public class TestSceneController {

    final TestSceneService testSceneService;

    public TestSceneController(TestSceneService testSceneService) {
        this.testSceneService = testSceneService;
    }

    @GetMapping("/scenes")
    public R<List<TestSceneVO>> scenes(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_SCENE_READ)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_SCENE_READ);
        }
        return testSceneService.scenes(userId);
    }

    @PostMapping("/scenes")
    public R<Long> create(
            @RequestBody @Valid TestSceneDTO request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_SCENE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_SCENE_WRITE);
        }
        return testSceneService.create(request, userId);
    }

    @PutMapping("/scenes/{id}")
    public R<Void> update(
            @PathVariable Long id,
            @RequestBody @Valid TestSceneDTO request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_SCENE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_SCENE_WRITE);
        }
        return testSceneService.update(id, request, userId);
    }

    @DeleteMapping("/scenes/{id}")
    public R<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_SCENE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_SCENE_WRITE);
        }
        return testSceneService.delete(id, userId);
    }
}
