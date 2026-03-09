package com.yuan.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("test_metric_second")
public class TestMetricSecond {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("ts")
    private LocalDateTime ts;

    @TableField("qps")
    private BigDecimal qps;

    @TableField("p50")
    private BigDecimal p50;

    @TableField("p90")
    private BigDecimal p90;

    @TableField("p99")
    private BigDecimal p99;

    @TableField("error_rate")
    private BigDecimal errorRate;
}
