package com.yuan.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.common.result.R;
import com.yuan.monitor.dto.CreateAlertRuleDTO;
import com.yuan.monitor.dto.UpdateAlertRuleDTO;
import com.yuan.monitor.dto.UpdateAlertRuleEnabledDTO;
import com.yuan.monitor.entity.AlertRule;
import com.yuan.monitor.mapper.AlertRuleMapper;
import com.yuan.monitor.service.AlertRuleService;
import com.yuan.monitor.vo.AlertRuleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleMapper alertRuleMapper;

    public AlertRuleServiceImpl(AlertRuleMapper alertRuleMapper) {
        this.alertRuleMapper = alertRuleMapper;
    }

    @Override
    public R<List<AlertRuleVO>> listAlertRules(Long userId) {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRule::getUserId, userId)
            .orderByDesc(AlertRule::getUpdatedAt)
            .orderByDesc(AlertRule::getId);

        List<AlertRuleVO> records = alertRuleMapper.selectList(wrapper).stream()
            .map(this::toVO)
            .toList();
        return R.success(records);
    }

    @Override
    public R<AlertRuleVO> create(Long userId, CreateAlertRuleDTO request) {
        AlertRule entity = new AlertRule();
        entity.setUserId(userId);
        BeanUtils.copyProperties(request, entity);
        alertRuleMapper.insert(entity);
        return R.success(this.toVO(entity));
    }

    @Override
    public R<AlertRuleVO> update(Long userId,Long ruleId, UpdateAlertRuleDTO request) {
        AlertRule alertRule = this.requireOwnedRule(userId, ruleId);
        BeanUtils.copyProperties(request, alertRule);
        alertRuleMapper.updateById(alertRule);
        return R.success(this.toVO(alertRule));
    }

    @Override
    public R<Void> switchAlertRule(Long userId,Long ruleId, UpdateAlertRuleEnabledDTO request) {
        AlertRule alertRule = this.requireOwnedRule(userId, ruleId);
        alertRule.setEnabled(request.getEnabled());
        alertRuleMapper.updateById(alertRule);
        return R.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> delete(Long userId, Long ruleId) {
        AlertRule alertRule = this.requireOwnedRule(userId, ruleId);
        alertRuleMapper.deleteById(alertRule.getId());
        return R.success();
    }

    private AlertRuleVO toVO(AlertRule entity) {
        AlertRuleVO vo = new AlertRuleVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setMetric(entity.getMetric());
        vo.setOp(entity.getOp());
        vo.setThreshold(entity.getThreshold());
        vo.setLevel(entity.getLevel());
        vo.setEnabled(entity.getEnabled());
        return vo;
    }

    private AlertRule requireOwnedRule(Long userId, Long ruleId) {
        AlertRule rule = alertRuleMapper.selectById(ruleId);
        if (rule == null || !userId.equals(rule.getUserId())) {
            throw new RuntimeException("告警规则不存在");
        }
        return rule;
    }

}
