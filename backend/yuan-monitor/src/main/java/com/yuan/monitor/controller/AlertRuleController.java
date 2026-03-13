package com.yuan.monitor.controller;

import com.yuan.common.result.R;
import com.yuan.monitor.dto.CreateAlertRuleDTO;
import com.yuan.monitor.dto.UpdateAlertRuleDTO;
import com.yuan.monitor.dto.UpdateAlertRuleEnabledDTO;
import com.yuan.monitor.service.AlertRuleService;
import com.yuan.monitor.vo.AlertRuleVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/alert-rules")
    public R<AlertRuleVO> create(@RequestHeader("X-User-Id") Long userId,
                                 @Valid @RequestBody CreateAlertRuleDTO request) {
        return alertRuleService.create(userId, request);
    }
    @PutMapping("/alert-rules/{ruleId}")
    public R<AlertRuleVO> update(@RequestHeader("X-User-Id") Long userId,
                                 @PathVariable Long ruleId,
                                 @Valid @RequestBody UpdateAlertRuleDTO request) {
        return alertRuleService.update(userId,ruleId, request);
    }
    @PatchMapping("/alert-rules/{ruleId}/switch")
    public R<Void> switchAlertRule(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable Long ruleId,
                                   @Valid @RequestBody UpdateAlertRuleEnabledDTO request) {
        return alertRuleService.switchAlertRule(userId,ruleId, request);
    }
    @DeleteMapping("/alert-rules/{ruleId}")
    public R<Void> delete(@RequestHeader("X-User-Id") Long userId,
                          @PathVariable Long ruleId) {
        return alertRuleService.delete(userId, ruleId);
    }

}
