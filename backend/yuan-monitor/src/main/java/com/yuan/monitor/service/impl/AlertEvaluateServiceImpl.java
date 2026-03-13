package com.yuan.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.api.test.mq.TestMetricMessage;
import com.yuan.monitor.entity.AlertRecord;
import com.yuan.monitor.entity.AlertRule;
import com.yuan.monitor.entity.AlertState;
import com.yuan.monitor.mapper.AlertRecordMapper;
import com.yuan.monitor.mapper.AlertRuleMapper;
import com.yuan.monitor.mapper.AlertStateMapper;
import com.yuan.monitor.service.AlertEvaluateService;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertEvaluateServiceImpl implements AlertEvaluateService {
    @Autowired
    AlertRuleMapper alertRuleMapper;
    @Autowired
    AlertRecordMapper  alertRecordMapper;
    @Autowired
    AlertStateMapper alertStateMapper;

    @Override
    public void evaluateTestMetric(TestMetricMessage message) {
        // 这里可以添加实际的评估逻辑，例如根据消息内容判断是否触发告警
        // 示例中简单地查询所有启用的告警规则
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRule::getEnabled, true)
                .eq(AlertRule::getUserId, message.getUserId());

        List<AlertRule> alertRules = alertRuleMapper.selectList(wrapper);


        LambdaQueryWrapper<AlertState> stateWrapper = new LambdaQueryWrapper<>();
        stateWrapper.eq(AlertState::getUserId, message.getUserId())
                .eq(AlertState::getTaskId, message.getTaskId())
                .orderByDesc(AlertState::getCreatedAt);

        List<AlertState> allStates = alertStateMapper.selectList(stateWrapper);

        AlertRecord record = new AlertRecord();
        record.setUserId(message.getUserId());
        record.setTaskId(message.getTaskId());

        for (AlertRule alertRule : alertRules) {

            List<AlertState> states = allStates.stream()
                    .filter(s -> s.getRuleId().equals(alertRule.getId())).toList();
            AlertState state = states.getFirst();

            record.setRuleId(alertRule.getId());
            record.setRuleName(alertRule.getName());
            record.setLevel(alertRule.getLevel());
            record.setCurrentValue(
                    alertRule.getMetric().equals("P99") ? message.getP99() : message.getErrorRate()
            );

            // 未命中
            if(!evaluateRule(alertRule, message)){
                // 恢复
                if(state!=null&&state.getActive().equals(true)) {
                    state.setActive(false);
                    state.setLastRecoveredAt(LocalDateTime.now());
                    record.setEventType("RECOVER");
                    alertStateMapper.updateById(state);
                    alertRecordMapper.insert(record);
                }
                continue;
            }
            // 命中
            analysisState(state,record);
        }
    }

    private void analysisState(AlertState state, AlertRecord record) {

        if(state==null){
            state = new AlertState();
            state.setUserId(record.getUserId());
            state.setTaskId(record.getTaskId());
            state.setRuleId(record.getRuleId());
            state.setActive(true);
            state.setLatestValue(record.getCurrentValue());
            record.setEventType("TRIGGER");
            alertStateMapper.insert(state);
            alertRecordMapper.insert(record);
            return;
        }

        if(state.getActive().equals(false)){
            state.setActive(true);
            state.setLatestValue(record.getCurrentValue());
            state.setLastTriggeredAt(LocalDateTime.now());
            record.setEventType("TRIGGER");
            alertStateMapper.insert(state);
            alertRecordMapper.insert(record);
            return;
        }

        state.setLatestValue(record.getCurrentValue());
        alertStateMapper.updateById(state);
    }

    private boolean evaluateRule(AlertRule rule, TestMetricMessage message) {
        // TODO 优化评估逻辑
        String metric = rule.getMetric();
        String op = rule.getOp();
        BigDecimal threshold = rule.getThreshold();
        if("P99".equals(metric)){
            BigDecimal val = message.getP99();
            return match(op, val, threshold);
        }else if("ERROR_RATE".equals(metric)){
            BigDecimal val = message.getErrorRate();
            return match(op, val, threshold);
        }
        return false; // 示例中默认不触发告警
    }

    private boolean match(String op,BigDecimal val,BigDecimal threshold){
        switch (op) {
            case ">":
                if (val.compareTo(threshold) > 0) {
                    return true;
                }
                break;
            case ">=":
                if (val.compareTo(threshold) >= 0) {
                    return true;
                }
                break;
        }
        return false;
    }
}
