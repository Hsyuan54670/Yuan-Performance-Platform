package com.yuan.monitor.controller;

import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import com.yuan.monitor.service.AlertStateService;
import com.yuan.monitor.vo.ActiveAlertVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitor")
public class AlertStateController {

    private final AlertStateService alertStateService;

    public AlertStateController(AlertStateService alertStateService) {
        this.alertStateService = alertStateService;
    }

    @GetMapping("/alert-states/active")
    public R<List<ActiveAlertVO>> listActiveAlerts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.MONITOR_ALERT_READ)) {
            return PermissionUtil.forbidden(PermissionCode.MONITOR_ALERT_READ);
        }
        return alertStateService.listActiveAlerts(userId);
    }
}
