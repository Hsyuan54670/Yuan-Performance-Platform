package com.yuan.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_suggestion")
public class AnalysisSuggestion {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    @TableField("report_id")
    private Long reportId;
    @TableField("priority")
    private String priority;
    @TableField("title")
    private String title;
    @TableField("detail")
    private String detail;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
