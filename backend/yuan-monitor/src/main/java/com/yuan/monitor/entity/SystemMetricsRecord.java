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

    @TableField("run_id")
    private Long runId;

    @TableField("ts")
    private LocalDateTime ts;

    // 该秒内高频采样的平均 CPU 使用率。
    @TableField("cpu")
    private BigDecimal cpu;

    // 该秒内观测到的 CPU 峰值，避免短尖峰被平均值抹平。
    @TableField("cpu_max")
    private BigDecimal cpuMax;

    // 该秒内高频采样的平均内存使用率。
    @TableField("memory")
    private BigDecimal memory;

    // 该秒内观测到的内存峰值。
    @TableField("memory_max")
    private BigDecimal memoryMax;

    @TableField("disk")
    private BigDecimal disk;

    @TableField("network_in")
    private BigDecimal networkIn;

    @TableField("network_out")
    private BigDecimal networkOut;

    // 聚合到这一秒的原始样本数，用于判断该秒数据可信度。
    @TableField("sample_count")
    private Integer sampleCount;

    // true 表示这一秒只保留了空桶占位，没有采到有效资源样本。
    @TableField("missing")
    private Boolean missing;
}
