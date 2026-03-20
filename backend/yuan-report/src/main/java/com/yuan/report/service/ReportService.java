package com.yuan.report.service;

import com.yuan.api.analysis.dto.ReportDTO;
import com.yuan.api.analysis.dto.ReportSummaryDTO;
import com.yuan.common.result.R;

import java.util.List;

public interface ReportService {

    R<List<ReportSummaryDTO>> listReports(
            Long userId,
            Long taskId,
            String grade,
            String createdFrom,
            String createdTo
    );

    R<ReportDTO> getReportByRunId(Long userId, Long runId);

    String getReportHtmlByRunId(Long userId, Long runId);
}
