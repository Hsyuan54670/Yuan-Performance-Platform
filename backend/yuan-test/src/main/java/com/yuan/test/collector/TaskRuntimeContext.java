package com.yuan.test.collector;

import lombok.Data;
import org.apache.jmeter.engine.StandardJMeterEngine;

import java.time.LocalDateTime;

@Data
public class TaskRuntimeContext {

    private Long taskId;

    private Long runId;

    private StandardJMeterEngine engine;

    private LocalDateTime startTime;

    private Integer durationSeconds;

    private boolean stopped;
}
