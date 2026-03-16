package com.yuan.analysis.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AnalysisResultVO {

    private Long taskId;
    private Long runId;
    private String grade;
    private Integer score;
    private String summary;
    private String status;
    private String source;
    private LocalDateTime createdAt;
    private List<BottleneckItemVO> bottlenecks = new ArrayList<>();
    private List<SuggestionItemVO> suggestions = new ArrayList<>();

    @Data
    public static class BottleneckItemVO {
        private String time;
        private String type;
        private String reason;
        private String evidence;
        private String severity;
    }

    @Data
    public static class SuggestionItemVO {
        private Long id;
        private String priority;
        private String title;
        private String detail;
    }
}
