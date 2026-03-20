package com.yuan.test.controller;

import com.yuan.api.test.dto.TestMetricDTO;
import com.yuan.api.test.dto.TestRunBaselineDTO;
import com.yuan.api.test.dto.TestRunContextDTO;
import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import com.yuan.test.dto.TestTaskCreateDTO;
import com.yuan.test.service.TestTaskService;
import com.yuan.test.vo.TestMetricVO;
import com.yuan.test.vo.TestTaskRunVO;
import com.yuan.test.vo.TestTaskVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestTaskController {

    @Autowired
    TestTaskService testTaskService;

    @GetMapping("/tasks")
    public R<List<TestTaskVO>> tasks(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_TASK_READ)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_TASK_READ);
        }
        return testTaskService.tasks(userId);
    }

    @PostMapping("/tasks/{id}/start")
    public R<Void> start(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_TASK_START)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_TASK_START);
        }
        return testTaskService.start(id, userId);
    }

    @PostMapping("/tasks/{id}/stop")
    public R<Void> stop(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_TASK_STOP)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_TASK_STOP);
        }
        return testTaskService.stop(id, userId);
    }

    @PostMapping("/tasks")
    public R<Long> create(
            @RequestBody @Valid TestTaskCreateDTO request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_TASK_CREATE)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_TASK_CREATE);
        }
        return testTaskService.create(request, userId);
    }

    @GetMapping("/tasks/{id}/runs")
    public R<List<TestTaskRunVO>> runs(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_TASK_READ)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_TASK_READ);
        }
        return testTaskService.runs(id, userId);
    }

    @GetMapping("/tasks/{id}/metrics")
    public R<List<TestMetricVO>> metrics(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.TEST_TASK_READ)) {
            return PermissionUtil.forbidden(PermissionCode.TEST_TASK_READ);
        }
        return testTaskService.metrics(id, userId);
    }

    @GetMapping("/runs/{runId}/metrics")
    public R<List<TestMetricDTO>> runMetrics(
            @PathVariable Long runId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return testTaskService.runMetrics(runId, userId);
    }

    @GetMapping("/runs/{runId}/context")
    public R<TestRunContextDTO> runContext(
            @PathVariable Long runId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return testTaskService.runContext(runId, userId);
    }

    @GetMapping("/runs/{runId}/baseline")
    public R<TestRunBaselineDTO> runBaseline(
            @PathVariable Long runId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return testTaskService.runBaseline(runId, userId);
    }
}
