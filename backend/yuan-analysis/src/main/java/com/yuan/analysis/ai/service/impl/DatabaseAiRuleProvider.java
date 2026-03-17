package com.yuan.analysis.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.analysis.ai.model.AiRuleInstruction;
import com.yuan.analysis.ai.service.AiRuleProvider;
import com.yuan.analysis.entity.AnalysisRule;
import com.yuan.analysis.mapper.AnalysisRuleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseAiRuleProvider implements AiRuleProvider {

    private final AnalysisRuleMapper analysisRuleMapper;

    public DatabaseAiRuleProvider(AnalysisRuleMapper analysisRuleMapper) {
        this.analysisRuleMapper = analysisRuleMapper;
    }

    @Override
    public List<AiRuleInstruction> listEnabledRules(Long userId) {
        LambdaQueryWrapper<AnalysisRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisRule::getUserId, userId)
                .eq(AnalysisRule::getRuleType, "AI")
                .eq(AnalysisRule::getEnabled, true)
                .orderByDesc(AnalysisRule::getUpdatedAt)
                .orderByDesc(AnalysisRule::getId);
        return analysisRuleMapper.selectList(wrapper).stream()
                .map(this::toInstruction)
                .toList();
    }

    private AiRuleInstruction toInstruction(AnalysisRule rule) {
        AiRuleInstruction instruction = new AiRuleInstruction();
        instruction.setName(rule.getName());
        instruction.setInstruction(rule.getInstruction());
        instruction.setBottleneckType(rule.getBottleneckType());
        instruction.setSeverity(rule.getSeverity());
        instruction.setPriority(rule.getPriority());
        return instruction;
    }
}
