package com.yuan.test.collector;

import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;

/*
* 监听器
* */

@Slf4j
public class TaskResultCollector extends ResultCollector {
    private final Long taskId;
    private final Long runId;
    private final TaskMetricAggregator aggregator;

    public TaskResultCollector(TaskMetricAggregator aggregator, Long taskId, Long runId) {
        this.taskId = taskId;
        this.aggregator = aggregator;
        this.runId = runId;
    }
    // 样本回调
    @Override
    public void sampleOccurred(SampleEvent event) {
        super.sampleOccurred(event);
        // 1. 获取样本结果
        SampleResult sampleResult = event.getResult();
        // 2. 记录样本结果
        log.info("TaskResultCollector received sample: runId={}, time={}, success={}",
                runId , sampleResult.getTime(), sampleResult.isSuccessful());
        aggregator.record(taskId,runId, sampleResult);
    }
}
