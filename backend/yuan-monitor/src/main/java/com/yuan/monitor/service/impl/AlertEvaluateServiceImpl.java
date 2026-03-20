package com.yuan.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.api.test.mq.TestMetricMessage;
import com.yuan.monitor.entity.AlertRecord;
import com.yuan.monitor.entity.AlertRule;
import com.yuan.monitor.entity.AlertState;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.mapper.AlertRecordMapper;
import com.yuan.monitor.mapper.AlertRuleMapper;
import com.yuan.monitor.mapper.AlertStateMapper;
import com.yuan.monitor.runtime.ActiveRunRegistry;
import com.yuan.monitor.service.AlertEvaluateService;
import com.yuan.monitor.vo.SystemMetricVO;
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
    AlertRecordMapper alertRecordMapper;
    @Autowired
    AlertStateMapper alertStateMapper;
    @Autowired
    ActiveRunRegistry activeRunRegistry;

    @Override
    public void evaluateTestMetric(TestMetricMessage message) {
        if (message.getUserId() == null || message.getTaskId() == null || message.getRunId() == null) {
            return;
        }

        List<AlertRule> alertRules = listEnabledRules(message.getUserId());
        List<AlertState> allStates = listTaskStates(message.getUserId(), message.getTaskId(), message.getRunId());

        for (AlertRule alertRule : alertRules) {
            if (!isTestMetricRule(alertRule)) {
                continue;
            }

            AlertState state = findState(allStates, alertRule.getId());
            AlertRecord record = buildTestAlertRecord(alertRule, message);
            if (!evaluateTestRule(alertRule, message)) {
                recoverStateIfNeeded(state, record, LocalDateTime.now());
                continue;
            }
            analysisState(state, record);
        }
    }

    @Override
    public void evaluateSystemMetric(SystemMetricVO metric) {
        TestStatusRecord runningTask = activeRunRegistry.findLatestRunningTask();
        if (runningTask == null) {
            return;
        }
        evaluateSystemMetric(runningTask, metric);
    }

    @Override
    public void evaluateSystemMetric(TestStatusRecord runningTask, SystemMetricVO metric) {
        if (runningTask == null || runningTask.getUserId() == null || runningTask.getTaskId() == null || runningTask.getRunId() == null) {
            return;
        }

        List<AlertRule> alertRules = listEnabledRules(runningTask.getUserId());
        List<AlertState> allStates = listTaskStates(runningTask.getUserId(), runningTask.getTaskId(), runningTask.getRunId());

        for (AlertRule alertRule : alertRules) {
            if (!isSystemMetricRule(alertRule)) {
                continue;
            }

            AlertState state = findState(allStates, alertRule.getId());
            AlertRecord record = buildSystemAlertRecord(alertRule, runningTask, metric);
            if (!evaluateSystemRule(alertRule, metric)) {
                recoverStateIfNeeded(state, record, LocalDateTime.now());
                continue;
            }
            analysisState(state, record);
        }
    }

    @Override
    public void recoverAlertsOnRunFinished(TestStatusRecord statusRecord) {
        if (statusRecord == null
                || statusRecord.getUserId() == null
                || statusRecord.getTaskId() == null
                || statusRecord.getRunId() == null
                || !isTerminalStatus(statusRecord.getStatus())) {
            return;
        }

        List<AlertState> activeStates = listActiveStates(statusRecord.getUserId(), statusRecord.getTaskId(), statusRecord.getRunId());
        if (activeStates.isEmpty()) {
            return;
        }

        LocalDateTime recoveredAt = statusRecord.getCreatedAt() != null ? statusRecord.getCreatedAt() : LocalDateTime.now();
        for (AlertState state : activeStates) {
            AlertRule alertRule = alertRuleMapper.selectById(state.getRuleId());
            if (alertRule == null) {
                continue;
            }

            AlertRecord record = new AlertRecord();
            record.setUserId(statusRecord.getUserId());
            record.setTaskId(statusRecord.getTaskId());
            record.setRunId(statusRecord.getRunId());
            record.setRuleId(alertRule.getId());
            record.setRuleName(alertRule.getName());
            record.setLevel(alertRule.getLevel());
            record.setCurrentValue(state.getLatestValue());
            recoverStateIfNeeded(state, record, recoveredAt);
        }
    }

    private List<AlertRule> listEnabledRules(Long userId) {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRule::getEnabled, true)
                .eq(AlertRule::getUserId, userId);
        return alertRuleMapper.selectList(wrapper);
    }

    private List<AlertState> listTaskStates(Long userId, Long taskId, Long runId) {
        LambdaQueryWrapper<AlertState> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertState::getUserId, userId)
                .eq(AlertState::getTaskId, taskId)
                .eq(AlertState::getRunId, runId)
                .orderByDesc(AlertState::getCreatedAt);
        return alertStateMapper.selectList(wrapper);
    }

    private List<AlertState> listActiveStates(Long userId, Long taskId, Long runId) {
        LambdaQueryWrapper<AlertState> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertState::getUserId, userId)
                .eq(AlertState::getTaskId, taskId)
                .eq(AlertState::getRunId, runId)
                .eq(AlertState::getActive, true)
                .orderByDesc(AlertState::getCreatedAt);
        return alertStateMapper.selectList(wrapper);
    }

    private AlertState findState(List<AlertState> allStates, Long ruleId) {
        return allStates.stream()
                .filter(state -> state.getRuleId().equals(ruleId))
                .findFirst()
                .orElse(null);
    }

    private void recoverStateIfNeeded(AlertState state, AlertRecord record, LocalDateTime recoveredAt) {
        if (state != null && Boolean.TRUE.equals(state.getActive())) {
            state.setActive(false);
            state.setLatestRecoveredAt(recoveredAt);
            record.setEventType("RECOVER");
            record.setCreatedAt(recoveredAt);
            alertStateMapper.updateById(state);
            alertRecordMapper.insert(record);
        }
    }

    private boolean isTestMetricRule(AlertRule rule) {
        return "P99".equals(rule.getMetric()) || "ERROR_RATE".equals(rule.getMetric());
    }

    private boolean isSystemMetricRule(AlertRule rule) {
        return "CPU".equals(rule.getMetric()) || "MEMORY".equals(rule.getMetric());
    }

    private boolean isTerminalStatus(String status) {
        return "SUCCESS".equals(status) || "FAILED".equals(status) || "STOPPED".equals(status);
    }

    private void analysisState(AlertState state, AlertRecord record) {
        if (state == null) {
            state = new AlertState();
            state.setUserId(record.getUserId());
            state.setTaskId(record.getTaskId());
            state.setRunId(record.getRunId());
            state.setRuleId(record.getRuleId());
            state.setActive(true);
            state.setLatestValue(record.getCurrentValue());
            state.setLatestTriggeredAt(LocalDateTime.now());
            record.setEventType("TRIGGER");
            record.setCreatedAt(LocalDateTime.now());
            alertStateMapper.insert(state);
            alertRecordMapper.insert(record);
            return;
        }

        if (Boolean.FALSE.equals(state.getActive())) {
            state.setActive(true);
            state.setLatestValue(record.getCurrentValue());
            state.setLatestTriggeredAt(LocalDateTime.now());
            record.setEventType("TRIGGER");
            record.setCreatedAt(LocalDateTime.now());
            alertStateMapper.updateById(state);
            alertRecordMapper.insert(record);
            return;
        }

        state.setLatestValue(record.getCurrentValue());
        alertStateMapper.updateById(state);
    }

    private boolean evaluateTestRule(AlertRule rule, TestMetricMessage message) {
        return match(rule.getOp(), resolveTestMetricValue(rule, message), rule.getThreshold());
    }

    private boolean evaluateSystemRule(AlertRule rule, SystemMetricVO metric) {
        return match(rule.getOp(), resolveSystemMetricValue(rule, metric), rule.getThreshold());
    }

    private boolean match(String op, BigDecimal val, BigDecimal threshold) {
        if (op == null || val == null || threshold == null) {
            return false;
        }

        switch (op) {
            case ">":
                return val.compareTo(threshold) > 0;
            case ">=":
                return val.compareTo(threshold) >= 0;
            default:
                return false;
        }
    }

    private AlertRecord buildTestAlertRecord(AlertRule alertRule, TestMetricMessage message) {
        AlertRecord record = new AlertRecord();
        record.setUserId(message.getUserId());
        record.setTaskId(message.getTaskId());
        record.setRunId(message.getRunId());
        record.setRuleId(alertRule.getId());
        record.setRuleName(alertRule.getName());
        record.setLevel(alertRule.getLevel());
        record.setCurrentValue(resolveTestMetricValue(alertRule, message));
        return record;
    }

    private AlertRecord buildSystemAlertRecord(AlertRule alertRule, TestStatusRecord runningTask, SystemMetricVO metric) {
        AlertRecord record = new AlertRecord();
        record.setUserId(runningTask.getUserId());
        record.setTaskId(runningTask.getTaskId());
        record.setRunId(runningTask.getRunId());
        record.setRuleId(alertRule.getId());
        record.setRuleName(alertRule.getName());
        record.setLevel(alertRule.getLevel());
        record.setCurrentValue(resolveSystemMetricValue(alertRule, metric));
        return record;
    }

    private BigDecimal resolveTestMetricValue(AlertRule alertRule, TestMetricMessage message) {
        if ("P99".equals(alertRule.getMetric())) {
            return message.getP99();
        }
        if ("ERROR_RATE".equals(alertRule.getMetric())) {
            return message.getErrorRate();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveSystemMetricValue(AlertRule alertRule, SystemMetricVO metric) {
        if ("CPU".equals(alertRule.getMetric())) {
            return metric.getCpu();
        }
        if ("MEMORY".equals(alertRule.getMetric())) {
            return metric.getMemory();
        }
        return BigDecimal.ZERO;
    }
}
