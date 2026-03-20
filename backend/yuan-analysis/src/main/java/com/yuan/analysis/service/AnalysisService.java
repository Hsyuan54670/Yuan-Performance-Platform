package com.yuan.analysis.service;

import com.yuan.analysis.vo.AnalysisReportSummaryVO;
import com.yuan.analysis.vo.AnalysisResultVO;
import com.yuan.api.test.mq.TestCompletedMessage;
import com.yuan.common.result.R;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalysisService {

    void handleMessage(TestCompletedMessage message);

    R<List<AnalysisReportSummaryVO>> listReports(
            Long userId,
            Long taskId,
            String grade,
            LocalDateTime createdFrom,
            LocalDateTime createdTo
    );

    R<AnalysisResultVO> getReportByRunId(Long userId, Long runId);

    R<AnalysisResultVO> getLatestReportByTaskId(Long userId, Long taskId);

    String getReportHtmlByRunId(Long userId, Long runId);

    String getLatestReportHtmlByTaskId(Long userId, Long taskId);
}
