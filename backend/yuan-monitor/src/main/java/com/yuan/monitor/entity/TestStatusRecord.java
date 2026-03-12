package com.yuan.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("test_status_record")
public class TestStatusRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("run_id")
    private Long runId;

    @TableField("status")
    private String status;

    @TableField("message")
    private String message;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
