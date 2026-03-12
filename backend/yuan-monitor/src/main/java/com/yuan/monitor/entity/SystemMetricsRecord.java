package com.yuan.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("monitor_sys_metric_second")
public class SystemMetricsRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("ts")
    private LocalDateTime ts;

    @TableField("cpu")
    private BigDecimal cpu;

    @TableField("memory")
    private BigDecimal memory;

    @TableField("disk")
    private BigDecimal disk;

    @TableField("network_in")
    private BigDecimal networkIn;

    @TableField("network_out")
    private BigDecimal networkOut;
}
