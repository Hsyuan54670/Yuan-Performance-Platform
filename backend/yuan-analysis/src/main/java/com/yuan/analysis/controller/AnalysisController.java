package com.yuan.analysis.controller;

import com.yuan.analysis.service.AnalysisService;
import com.yuan.analysis.vo.AnalysisResultVO;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {
    @Autowired
    AnalysisService analysisService;

    @GetMapping("/reports/runs/{runId}")
    public R<AnalysisResultVO> getReportByRunId(@RequestHeader("X-User-Id") Long userId,@PathVariable Long runId){
        return analysisService.getReportByRunId(userId,runId);
    }

    @GetMapping("/reports/tasks/{taskId}/latest")
    public R<AnalysisResultVO> getLatestReportByTaskId(@RequestHeader("X-User-Id") Long userId,@PathVariable Long taskId){
        return analysisService.getLatestReportByTaskId(userId,taskId);
    }
}
