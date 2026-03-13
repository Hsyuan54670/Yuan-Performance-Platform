package com.yuan.api.test.mq;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class TestStatusMessage {
    private Long userId;
    private Long taskId;
    private Long runId;
    private String status;
    private String message;
    private LocalDateTime timestamp;
}