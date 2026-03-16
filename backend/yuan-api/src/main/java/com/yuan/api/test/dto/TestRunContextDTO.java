package com.yuan.api.test.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestRunContextDTO {
    /** 用户ID */
    private Long userId;

    /** 任务ID */
    private Long taskId;

    /** 运行ID */
    private Long runId;


    /** 计划名称 */
    private String planName;

    /** 场景名称 */
    private String sceneName;


    /** 并发数 */
    private Integer concurrency;

    /** 压测持续时间，单位秒 */
    private Integer durationSeconds;

    /** 运行状态 */
    private String status;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;
}
