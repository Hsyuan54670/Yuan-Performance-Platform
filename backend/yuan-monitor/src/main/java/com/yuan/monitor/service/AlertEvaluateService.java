package com.yuan.monitor.service;


import com.yuan.api.test.mq.TestMetricMessage;

public interface AlertEvaluateService  {
    void evaluateTestMetric(TestMetricMessage message);
}
