package com.yuan.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_report")
public class AnalysisReport {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("task_id")
    private Long taskId;

    @TableField("run_id")
    private Long runId;

    @TableField("grade")
    private String grade;

    @TableField("score")
    private Integer score;

    @TableField("summary")
    private String summary;

    @TableField("status")
    private String status;

    @TableField("source")
    private String source;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
