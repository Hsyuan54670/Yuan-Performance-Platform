package com.yuan.analysis.ai.service;

import com.yuan.analysis.ai.model.AiRuleInstruction;

import java.util.List;

public interface AiRuleProvider {

    List<AiRuleInstruction> listEnabledRules(Long userId);
}
