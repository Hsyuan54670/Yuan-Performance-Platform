package com.yuan.api.analysis.feign;

import com.yuan.api.analysis.dto.ReportDTO;
import com.yuan.api.analysis.dto.ReportSummaryDTO;
import com.yuan.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "yuan-analysis")
public interface AnalysisFeignClient {

    @GetMapping("/analysis/reports")
    R<List<ReportSummaryDTO>> listReports(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "taskId", required = false) Long taskId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "createdFrom", required = false) String createdFrom,
            @RequestParam(value = "createdTo", required = false) String createdTo
    );

    @GetMapping("/analysis/reports/runs/{runId}")
    R<ReportDTO> getReportByRunId(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("runId") Long runId
    );

    @GetMapping(value = "/analysis/reports/runs/{runId}/html", produces = MediaType.TEXT_HTML_VALUE)
    String getReportHtmlByRunId(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("runId") Long runId
    );
}
