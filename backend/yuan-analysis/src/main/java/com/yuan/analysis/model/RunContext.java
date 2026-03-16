package com.yuan.analysis.model;

import com.yuan.api.test.dto.TestRunContextDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RunContext {

    /** 计划名称 */
    private String planName;

    /** 场景名称 */
    private String sceneName;

    /** 并发用户数 */
    private Integer concurrency;

    /** 压测持续时间，单位秒 */
    private Integer durationSeconds;

    /** 运行开始时间 */
    private LocalDateTime startTime;

    /** 运行结束时间 */
    private LocalDateTime endTime;


}
