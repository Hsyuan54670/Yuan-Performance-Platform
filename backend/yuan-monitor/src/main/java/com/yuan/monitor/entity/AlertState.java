package com.yuan.monitor.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("monitor_alert_state")
public class AlertState {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("task_id")
    private Long taskId;
    @TableField("rule_id")
    private Long ruleId;
    @TableField("active")
    private Boolean active;
    @TableField("latest_value")
    private BigDecimal latestValue;
    @TableField("last_triggered_at")
    private LocalDateTime lastTriggeredAt;
    @TableField("last_recovered_at")
    private LocalDateTime lastRecoveredAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
