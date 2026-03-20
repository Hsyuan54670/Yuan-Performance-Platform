package com.yuan.monitor.controller;

import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import com.yuan.monitor.service.AlertRecordService;
import com.yuan.monitor.vo.AlertRecordVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitor")
public class AlertRecordController {

    private final AlertRecordService alertRecordService;

    public AlertRecordController(AlertRecordService alertRecordService) {
        this.alertRecordService = alertRecordService;
    }

    @GetMapping("/alert-records")
    public R<List<AlertRecordVO>> listAlertRecords(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.MONITOR_ALERT_READ)) {
            return PermissionUtil.forbidden(PermissionCode.MONITOR_ALERT_READ);
        }
        return alertRecordService.listAlertRecords(userId);
    }

    @GetMapping("/alert-records/runs/{runId}")
    public R<List<AlertRecordVO>> listAlertRecordsByRunId(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long runId
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.MONITOR_ALERT_READ)) {
            return PermissionUtil.forbidden(PermissionCode.MONITOR_ALERT_READ);
        }
        return alertRecordService.listAlertRecordsByRunId(userId, runId);
    }
}
