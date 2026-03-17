package com.yuan.analysis.service;

import com.yuan.analysis.dto.CreateAnalysisRuleDTO;
import com.yuan.analysis.dto.UpdateAnalysisRuleDTO;
import com.yuan.analysis.dto.UpdateAnalysisRuleEnabledDTO;
import com.yuan.analysis.vo.AnalysisRuleVO;
import com.yuan.common.result.R;

import java.util.List;

public interface RuleService {

    R<List<AnalysisRuleVO>> listRules(Long userId);

    R<AnalysisRuleVO> create(Long userId, CreateAnalysisRuleDTO request);

    R<AnalysisRuleVO> update(Long userId, Long ruleId, UpdateAnalysisRuleDTO request);

    R<Void> switchRule(Long userId, Long ruleId, UpdateAnalysisRuleEnabledDTO request);

    R<Void> delete(Long userId, Long ruleId);
}
