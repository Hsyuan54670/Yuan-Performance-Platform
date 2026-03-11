package com.yuan.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("test_task")
public class TestTask {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("plan_id")
    private Long planId;

    @TableField(exist = false)
    private String planName;

    @TableField("scene_id")
    private Long sceneId;

    @TableField(exist = false)
    private  String sceneName;

    @TableField("status")
    private String status;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("duration")
    private Integer duration;

    @TableField("qps")
    private BigDecimal qps;

    @TableField("p99")
    private BigDecimal p99;

    @TableField("error_rate")
    private BigDecimal errorRate;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
