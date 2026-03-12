package com.yuan.monitor.service;

import com.yuan.common.result.R;
import com.yuan.monitor.vo.AlertRuleVO;

import java.util.List;

public interface AlertRuleService {
    R<List<AlertRuleVO>> listAlertRules(Long userId);
}
