package com.yuan.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.common.result.R;
import com.yuan.monitor.entity.AlertRule;
import com.yuan.monitor.entity.AlertState;
import com.yuan.monitor.mapper.AlertRuleMapper;
import com.yuan.monitor.mapper.AlertStateMapper;
import com.yuan.monitor.service.AlertStateService;
import com.yuan.monitor.vo.ActiveAlertVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AlertStateServiceImpl implements AlertStateService {

    private final AlertStateMapper alertStateMapper;
    private final AlertRuleMapper alertRuleMapper;

    public AlertStateServiceImpl(AlertStateMapper alertStateMapper, AlertRuleMapper alertRuleMapper) {
        this.alertStateMapper = alertStateMapper;
        this.alertRuleMapper = alertRuleMapper;
    }

    @Override
    public R<List<ActiveAlertVO>> listActiveAlerts(Long userId) {
        LambdaQueryWrapper<AlertState> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertState::getUserId, userId)
                .eq(AlertState::getActive, true)
                .orderByDesc(AlertState::getLatestTriggeredAt)
                .orderByDesc(AlertState::getUpdatedAt);

        List<ActiveAlertVO> records = alertStateMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .filter(Objects::nonNull)
                .toList();
        return R.success(records);
    }

    private ActiveAlertVO toVO(AlertState state) {
        AlertRule rule = alertRuleMapper.selectById(state.getRuleId());
        if (rule == null) {
            return null;
        }

        ActiveAlertVO vo = new ActiveAlertVO();
        vo.setRuleId(rule.getId());
        vo.setTaskId(state.getTaskId());
        vo.setRunId(state.getRunId());
        vo.setRuleName(rule.getName());
        vo.setMetric(rule.getMetric());
        vo.setOp(rule.getOp());
        vo.setThreshold(rule.getThreshold());
        vo.setLevel(rule.getLevel());
        vo.setLatestValue(state.getLatestValue());
        vo.setLatestTriggeredAt(state.getLatestTriggeredAt());
        return vo;
    }
}
