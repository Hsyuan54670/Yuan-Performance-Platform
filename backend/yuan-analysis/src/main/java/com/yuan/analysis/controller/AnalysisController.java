package com.yuan.analysis.controller;

import com.yuan.analysis.service.AnalysisService;
import com.yuan.analysis.vo.AnalysisResultVO;
import com.yuan.common.result.R;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/reports/runs/{runId}")
    public R<AnalysisResultVO> getReportByRunId(@RequestHeader("X-User-Id") Long userId, @PathVariable Long runId) {
        return analysisService.getReportByRunId(userId, runId);
    }

    @GetMapping("/reports/tasks/{taskId}/latest")
    public R<AnalysisResultVO> getLatestReportByTaskId(@RequestHeader("X-User-Id") Long userId, @PathVariable Long taskId) {
        return analysisService.getLatestReportByTaskId(userId, taskId);
    }

    @GetMapping(value = "/reports/runs/{runId}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getReportHtmlByRunId(@RequestHeader("X-User-Id") Long userId, @PathVariable Long runId) {
        return ResponseEntity.ok(analysisService.getReportHtmlByRunId(userId, runId));
    }

    @GetMapping(value = "/reports/tasks/{taskId}/latest/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getLatestReportHtmlByTaskId(@RequestHeader("X-User-Id") Long userId, @PathVariable Long taskId) {
        return ResponseEntity.ok(analysisService.getLatestReportHtmlByTaskId(userId, taskId));
    }
}
