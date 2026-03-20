package com.yuan.monitor.service;

import com.yuan.api.test.mq.TestMetricMessage;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.vo.SystemMetricVO;

public interface AlertEvaluateService {
    void evaluateTestMetric(TestMetricMessage message);

    void evaluateSystemMetric(SystemMetricVO metric);

    void evaluateSystemMetric(TestStatusRecord runningTask, SystemMetricVO metric);

    void recoverAlertsOnRunFinished(TestStatusRecord statusRecord);
}
