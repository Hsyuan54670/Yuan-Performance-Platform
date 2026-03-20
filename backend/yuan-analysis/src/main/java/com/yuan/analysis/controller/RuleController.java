package com.yuan.analysis.controller;

import com.yuan.analysis.dto.CreateAnalysisRuleDTO;
import com.yuan.analysis.dto.UpdateAnalysisRuleDTO;
import com.yuan.analysis.dto.UpdateAnalysisRuleEnabledDTO;
import com.yuan.analysis.service.RuleService;
import com.yuan.analysis.vo.AnalysisRuleVO;
import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analysis")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping("/rules")
    public R<List<AnalysisRuleVO>> listRules(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.ANALYSIS_RULE_READ)) {
            return PermissionUtil.forbidden(PermissionCode.ANALYSIS_RULE_READ);
        }
        return ruleService.listRules(userId);
    }

    @PostMapping("/rules")
    public R<AnalysisRuleVO> create(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @RequestBody CreateAnalysisRuleDTO request
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.ANALYSIS_RULE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.ANALYSIS_RULE_WRITE);
        }
        return ruleService.create(userId, request);
    }

    @PutMapping("/rules/{ruleId}")
    public R<AnalysisRuleVO> update(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long ruleId,
            @RequestBody UpdateAnalysisRuleDTO request
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.ANALYSIS_RULE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.ANALYSIS_RULE_WRITE);
        }
        return ruleService.update(userId, ruleId, request);
    }

    @PatchMapping("/rules/{ruleId}/switch")
    public R<Void> switchRule(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long ruleId,
            @RequestBody UpdateAnalysisRuleEnabledDTO request
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.ANALYSIS_RULE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.ANALYSIS_RULE_WRITE);
        }
        return ruleService.switchRule(userId, ruleId, request);
    }

    @DeleteMapping("/rules/{ruleId}")
    public R<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long ruleId
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.ANALYSIS_RULE_WRITE)) {
            return PermissionUtil.forbidden(PermissionCode.ANALYSIS_RULE_WRITE);
        }
        return ruleService.delete(userId, ruleId);
    }
}
