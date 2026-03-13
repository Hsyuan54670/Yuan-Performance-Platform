package com.yuan.monitor.service;

import com.yuan.common.result.R;
import com.yuan.monitor.dto.CreateAlertRuleDTO;
import com.yuan.monitor.dto.UpdateAlertRuleDTO;
import com.yuan.monitor.dto.UpdateAlertRuleEnabledDTO;
import com.yuan.monitor.vo.AlertRuleVO;

import java.util.List;

public interface AlertRuleService {
    R<List<AlertRuleVO>> listAlertRules(Long userId);

    R<AlertRuleVO> create(Long userId, CreateAlertRuleDTO request);

    R<AlertRuleVO> update(Long userId,Long ruleId, UpdateAlertRuleDTO request);

    R<Void> switchAlertRule(Long userId,Long ruleId, UpdateAlertRuleEnabledDTO request);

    R<Void> delete(Long userId, Long ruleId);
}
