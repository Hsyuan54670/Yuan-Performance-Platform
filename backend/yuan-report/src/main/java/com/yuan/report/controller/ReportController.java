package com.yuan.report.controller;

import com.yuan.api.analysis.dto.ReportDTO;
import com.yuan.api.analysis.dto.ReportSummaryDTO;
import com.yuan.common.constant.PermissionCode;
import com.yuan.common.result.R;
import com.yuan.common.util.PermissionUtil;
import com.yuan.report.service.ReportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public R<List<ReportSummaryDTO>> listReports(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @RequestParam(value = "taskId", required = false) Long taskId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "createdFrom", required = false) String createdFrom,
            @RequestParam(value = "createdTo", required = false) String createdTo
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.REPORT_CENTER_VIEW)) {
            return PermissionUtil.forbidden(PermissionCode.REPORT_CENTER_VIEW);
        }
        return reportService.listReports(userId, taskId, grade, createdFrom, createdTo);
    }

    @GetMapping("/reports/runs/{runId}")
    public R<ReportDTO> getReportByRunId(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long runId
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.REPORT_CENTER_VIEW)) {
            return PermissionUtil.forbidden(PermissionCode.REPORT_CENTER_VIEW);
        }
        return reportService.getReportByRunId(userId, runId);
    }

    @GetMapping(value = "/reports/runs/{runId}/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getReportHtmlByRunId(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Permissions", required = false) String permissionHeader,
            @PathVariable Long runId
    ) {
        if (!PermissionUtil.hasPermission(permissionHeader, PermissionCode.REPORT_CENTER_EXPORT)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("缺少权限: " + PermissionCode.REPORT_CENTER_EXPORT);
        }
        return ResponseEntity.ok(reportService.getReportHtmlByRunId(userId, runId));
    }
}
