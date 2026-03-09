package com.yuan.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("test_scene_step")
public class TestSceneStep {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("scene_id")
    private Long sceneId;

    @TableField("step_order")
    private Integer stepOrder;

    @TableField("name")
    private String name;

    @TableField("method")
    private String method;

    @TableField("path")
    private String path;

    @TableField("weight")
    private Integer weight;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
