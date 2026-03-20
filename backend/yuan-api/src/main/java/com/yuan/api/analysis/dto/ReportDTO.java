package com.yuan.api.analysis.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReportDTO {
    private Long taskId;
    private Long runId;
    private String grade;
    private Integer score;
    private String summary;
    private String status;
    private String source;
    private LocalDateTime createdAt;
    private List<BottleneckItemDTO> bottlenecks = new ArrayList<>();
    private List<SuggestionItemDTO> suggestions = new ArrayList<>();

    @Data
    public static class BottleneckItemDTO {
        private String time;
        private String type;
        private String reason;
        private String evidence;
        private String severity;
    }

    @Data
    public static class SuggestionItemDTO {
        private Long id;
        private String priority;
        private String title;
        private String detail;
    }
}
