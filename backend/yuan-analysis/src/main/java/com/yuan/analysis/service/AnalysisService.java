package com.yuan.analysis.service;

import com.yuan.api.test.mq.TestCompletedMessage;
import com.yuan.analysis.vo.AnalysisResultVO;
import com.yuan.common.result.R;

public interface AnalysisService {

    void handleMessage(TestCompletedMessage message);


    R<AnalysisResultVO> getReportByRunId(Long userId, Long runId);

    R<AnalysisResultVO> getLatestReportByTaskId(Long userId, Long taskId);
}
