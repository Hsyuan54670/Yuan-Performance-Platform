package com.yuan.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_plan")
public class TestPlan {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("name")
    private String name;

    @TableField("target_url")
    private String targetUrl;

    @TableField("concurrency")
    private Integer concurrency;

    @TableField("duration")
    private Integer duration;

    @TableField("ramp_type")
    private String rampType;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
