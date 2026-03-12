package com.yuan.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.common.result.R;
import com.yuan.monitor.entity.AlertRule;
import com.yuan.monitor.mapper.AlertRuleMapper;
import com.yuan.monitor.service.AlertRuleService;
import com.yuan.monitor.vo.AlertRuleVO;
import org.springframework.stereotype.Service;

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
}
