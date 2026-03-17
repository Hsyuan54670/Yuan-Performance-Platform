package com.yuan.analysis.service;

import com.yuan.analysis.vo.AnalysisResultVO;
import com.yuan.api.test.mq.TestCompletedMessage;
import com.yuan.common.result.R;

public interface AnalysisService {

    void handleMessage(TestCompletedMessage message);

    R<AnalysisResultVO> getReportByRunId(Long userId, Long runId);

    R<AnalysisResultVO> getLatestReportByTaskId(Long userId, Long taskId);

    String getReportHtmlByRunId(Long userId, Long runId);

    String getLatestReportHtmlByTaskId(Long userId, Long taskId);
}
