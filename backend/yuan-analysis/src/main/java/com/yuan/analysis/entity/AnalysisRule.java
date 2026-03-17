package com.yuan.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_rule")
public class AnalysisRule {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("rule_type")
    private String ruleType;

    @TableField("name")
    private String name;

    @TableField("expression")
    private String expression;

    @TableField("instruction")
    private String instruction;

    @TableField("bottleneck_type")
    private String bottleneckType;

    @TableField("severity")
    private String severity;

    @TableField("priority")
    private String priority;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
