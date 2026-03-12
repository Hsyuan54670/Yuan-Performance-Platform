package com.yuan.monitor.controller;

import com.yuan.common.result.R;
import com.yuan.monitor.service.AlertRuleService;
import com.yuan.monitor.vo.AlertRuleVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitor")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    public AlertRuleController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @GetMapping("/alert-rules")
    public R<List<AlertRuleVO>> listAlertRules(@RequestHeader("X-User-Id") Long userId) {
        return alertRuleService.listAlertRules(userId);
    }
}
