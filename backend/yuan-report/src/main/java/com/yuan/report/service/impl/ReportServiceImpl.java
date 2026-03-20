package com.yuan.report.service.impl;

import com.yuan.api.analysis.dto.ReportDTO;
import com.yuan.api.analysis.dto.ReportSummaryDTO;
import com.yuan.api.analysis.feign.AnalysisFeignClient;
import com.yuan.common.result.R;
import com.yuan.report.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final AnalysisFeignClient analysisFeignClient;

    public ReportServiceImpl(AnalysisFeignClient analysisFeignClient) {
        this.analysisFeignClient = analysisFeignClient;
    }

    @Override
    public R<List<ReportSummaryDTO>> listReports(
            Long userId,
            Long taskId,
            String grade,
            String createdFrom,
            String createdTo
    ) {
        return analysisFeignClient.listReports(userId, taskId, grade, createdFrom, createdTo);
    }

    @Override
    public R<ReportDTO> getReportByRunId(Long userId, Long runId) {
        return analysisFeignClient.getReportByRunId(userId, runId);
    }

    @Override
    public String getReportHtmlByRunId(Long userId, Long runId) {
        return analysisFeignClient.getReportHtmlByRunId(userId, runId);
    }
}
