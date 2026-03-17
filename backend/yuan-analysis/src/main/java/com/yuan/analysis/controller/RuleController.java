package com.yuan.analysis.controller;

import com.yuan.analysis.dto.CreateAnalysisRuleDTO;
import com.yuan.analysis.dto.UpdateAnalysisRuleDTO;
import com.yuan.analysis.dto.UpdateAnalysisRuleEnabledDTO;
import com.yuan.analysis.service.RuleService;
import com.yuan.analysis.vo.AnalysisRuleVO;
import com.yuan.common.result.R;
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
    public R<List<AnalysisRuleVO>> listRules(@RequestHeader("X-User-Id") Long userId) {
        return ruleService.listRules(userId);
    }

    @PostMapping("/rules")
    public R<AnalysisRuleVO> create(@RequestHeader("X-User-Id") Long userId,
                                    @RequestBody CreateAnalysisRuleDTO request) {
        return ruleService.create(userId, request);
    }

    @PutMapping("/rules/{ruleId}")
    public R<AnalysisRuleVO> update(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long ruleId,
                                    @RequestBody UpdateAnalysisRuleDTO request) {
        return ruleService.update(userId, ruleId, request);
    }

    @PatchMapping("/rules/{ruleId}/switch")
    public R<Void> switchRule(@RequestHeader("X-User-Id") Long userId,
                              @PathVariable Long ruleId,
                              @RequestBody UpdateAnalysisRuleEnabledDTO request) {
        return ruleService.switchRule(userId, ruleId, request);
    }

    @DeleteMapping("/rules/{ruleId}")
    public R<Void> delete(@RequestHeader("X-User-Id") Long userId,
                          @PathVariable Long ruleId) {
        return ruleService.delete(userId, ruleId);
    }
}
