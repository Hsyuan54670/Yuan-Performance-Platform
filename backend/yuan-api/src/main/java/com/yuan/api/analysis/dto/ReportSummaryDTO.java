package com.yuan.api.analysis.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportSummaryDTO {
    private Long id;
    private Long taskId;
    private Long runId;
    private String grade;
    private Integer score;
    private String summary;
    private String status;
    private String source;
    private LocalDateTime createdAt;
}
