package com.yuan.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("monitor_alert_record")
public class AlertRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("task_id")
    private Long taskId;

    @TableField("run_id")
    private Long runId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("rule_name")
    private String ruleName;

    @TableField("level")
    private String level;

    @TableField("event_type")
    private String eventType;

    @TableField("current_value")
    private BigDecimal currentValue;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
