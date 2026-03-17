package com.yuan.monitor.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskStatusPushVO {
    private String messageType;
    private Long taskId;
    private Long runId;
    private String status;
    private String message;
    private LocalDateTime timestamp;
}
