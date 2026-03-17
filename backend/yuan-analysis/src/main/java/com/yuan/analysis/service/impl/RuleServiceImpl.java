package com.yuan.analysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.analysis.dto.CreateAnalysisRuleDTO;
import com.yuan.analysis.dto.UpdateAnalysisRuleDTO;
import com.yuan.analysis.dto.UpdateAnalysisRuleEnabledDTO;
import com.yuan.analysis.entity.AnalysisRule;
import com.yuan.analysis.mapper.AnalysisRuleMapper;
import com.yuan.analysis.service.RuleService;
import com.yuan.analysis.vo.AnalysisRuleVO;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class RuleServiceImpl implements RuleService {

    private final AnalysisRuleMapper analysisRuleMapper;

    public RuleServiceImpl(AnalysisRuleMapper analysisRuleMapper) {
        this.analysisRuleMapper = analysisRuleMapper;
    }

    @Override
    public R<List<AnalysisRuleVO>> listRules(Long userId) {
        LambdaQueryWrapper<AnalysisRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisRule::getUserId, userId)
                .orderByAsc(AnalysisRule::getRuleType)
                .orderByDesc(AnalysisRule::getUpdatedAt)
                .orderByDesc(AnalysisRule::getId);
        List<AnalysisRuleVO> rules = analysisRuleMapper.selectList(wrapper).stream().map(this::toVO).toList();
        return R.success(rules);
    }

    @Override
    public R<AnalysisRuleVO> create(Long userId, CreateAnalysisRuleDTO request) {
        String validationMessage = validateRequest(request.getRuleType(), request.getName(), request.getExpression(), request.getInstruction());
        if (validationMessage != null) {
            return R.fail(HttpStatus.BAD_REQUEST, validationMessage);
        }

        AnalysisRule entity = new AnalysisRule();
        entity.setUserId(userId);
        BeanUtils.copyProperties(request, entity);
        normalizeRule(entity);
        analysisRuleMapper.insert(entity);
        return R.success(toVO(entity));
    }

    @Override
    public R<AnalysisRuleVO> update(Long userId, Long ruleId, UpdateAnalysisRuleDTO request) {
        AnalysisRule entity = requireOwnedRule(userId, ruleId);
        if (entity == null) {
            return R.fail(HttpStatus.NOT_FOUND, "分析规则不存在");
        }

        String validationMessage = validateRequest(request.getRuleType(), request.getName(), request.getExpression(), request.getInstruction());
        if (validationMessage != null) {
            return R.fail(HttpStatus.BAD_REQUEST, validationMessage);
        }

        BeanUtils.copyProperties(request, entity);
        normalizeRule(entity);
        analysisRuleMapper.updateById(entity);
        return R.success(toVO(entity));
    }

    @Override
    public R<Void> switchRule(Long userId, Long ruleId, UpdateAnalysisRuleEnabledDTO request) {
        AnalysisRule entity = requireOwnedRule(userId, ruleId);
        if (entity == null) {
            return R.fail(HttpStatus.NOT_FOUND, "分析规则不存在");
        }
        entity.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        analysisRuleMapper.updateById(entity);
        return R.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> delete(Long userId, Long ruleId) {
        AnalysisRule entity = requireOwnedRule(userId, ruleId);
        if (entity == null) {
            return R.fail(HttpStatus.NOT_FOUND, "分析规则不存在");
        }
        analysisRuleMapper.deleteById(entity.getId());
        return R.success();
    }

    private void normalizeRule(AnalysisRule entity) {
        String ruleType = entity.getRuleType() == null ? "ENGINE" : entity.getRuleType().trim().toUpperCase();
        entity.setRuleType(ruleType);
        entity.setEnabled(entity.getEnabled() == null || entity.getEnabled());
        entity.setPriority(StringUtils.hasText(entity.getPriority()) ? entity.getPriority().trim().toUpperCase() : "P1");
        entity.setSeverity(StringUtils.hasText(entity.getSeverity()) ? entity.getSeverity().trim().toUpperCase() : "MEDIUM");

        if ("ENGINE".equals(ruleType)) {
            entity.setInstruction(null);
        } else {
            entity.setExpression(null);
        }
    }

    private String validateRequest(String ruleType, String name, String expression, String instruction) {
        if (!StringUtils.hasText(name)) {
            return "规则名称不能为空";
        }
        String normalizedRuleType = ruleType == null ? "ENGINE" : ruleType.trim().toUpperCase();
        if (!"ENGINE".equals(normalizedRuleType) && !"AI".equals(normalizedRuleType)) {
            return "规则类型不合法";
        }
        if ("ENGINE".equals(normalizedRuleType) && !StringUtils.hasText(expression)) {
            return "规则引擎规则必须填写表达式";
        }
        if ("AI".equals(normalizedRuleType) && !StringUtils.hasText(instruction)) {
            return "AI规则必须填写提示词/分析指令";
        }
        return null;
    }

    private AnalysisRule requireOwnedRule(Long userId, Long ruleId) {
        AnalysisRule rule = analysisRuleMapper.selectById(ruleId);
        if (rule == null || !userId.equals(rule.getUserId())) {
            return null;
        }
        return rule;
    }

    private AnalysisRuleVO toVO(AnalysisRule entity) {
        AnalysisRuleVO vo = new AnalysisRuleVO();
        vo.setId(entity.getId());
        vo.setRuleType(entity.getRuleType());
        vo.setName(entity.getName());
        vo.setExpression(entity.getExpression());
        vo.setInstruction(entity.getInstruction());
        vo.setBottleneckType(entity.getBottleneckType());
        vo.setSeverity(entity.getSeverity());
        vo.setPriority(entity.getPriority());
        vo.setEnabled(entity.getEnabled());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
