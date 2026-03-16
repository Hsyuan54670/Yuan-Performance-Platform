package com.yuan.test.jmeter;

import com.yuan.test.collector.TaskMetricAggregator;
import com.yuan.test.collector.TaskRuntimeContext;
import com.yuan.test.entity.TestTask;
import com.yuan.test.entity.TestTaskRun;
import com.yuan.test.mapper.TestTaskMapper;
import com.yuan.test.mapper.TestTaskRunMapper;
import com.yuan.test.mq.TestCompletedProducer;
import com.yuan.test.mq.TestStatusProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jorphan.collections.ListedHashTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JmeterExecutionManager {

    @Autowired
    TestTaskMapper ttMapper;
    @Autowired
    TestTaskRunMapper ttrMapper;
    @Autowired
    TestStatusProducer tsProducer;
    @Autowired
    TestCompletedProducer tcProducer;
    @Autowired
    TaskMetricAggregator taskMetricAggregator;
    @Autowired
    ThreadPoolTaskExecutor taskExecutor;

    private final ConcurrentHashMap<Long, TaskRuntimeContext> jmeterEngineMap = new ConcurrentHashMap<>();

    public Boolean start(Long userId, Long taskId, Long runId, ListedHashTree testPlanTree, Integer durationSeconds) {
        StandardJMeterEngine jmeterEngine = new StandardJMeterEngine();
        jmeterEngine.configure(testPlanTree);
        TaskRuntimeContext context = new TaskRuntimeContext();
        context.setTaskId(taskId);
        context.setEngine(jmeterEngine);
        context.setStartTime(LocalDateTime.now());
        context.setRunId(runId);
        context.setDurationSeconds(durationSeconds);
        context.setStopped(false);

        TaskRuntimeContext existingContext = jmeterEngineMap.putIfAbsent(taskId, context);
        if (existingContext != null) {
            return false;
        }

        try {
            taskExecutor.execute(() -> {
                try {
                    jmeterEngine.run();
                } catch (Exception e) {
                    markAsyncRunFailed(userId, taskId, runId, "任务执行失败，异常信息：" + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            jmeterEngineMap.remove(taskId);
            throw new RuntimeException("任务提交线程池失败", e);
        }
        return true;
    }

    private void markAsyncRunFailed(Long userId, Long taskId, Long runId, String message, Exception exception) {
        log.error("执行任务异常，taskId={}, runId={}", taskId, runId, exception);
        jmeterEngineMap.remove(taskId);
        taskMetricAggregator.markRunFinished(runId);

        boolean runMarkedFailed = false;
        TestTaskRun testTaskRun = ttrMapper.selectById(runId);
        if (testTaskRun != null && "RUNNING".equals(testTaskRun.getStatus())) {
            testTaskRun.setStatus("FAILED");
            testTaskRun.setEndTime(LocalDateTime.now());
            ttrMapper.updateById(testTaskRun);
            runMarkedFailed = true;
        }

        TestTask testTask = ttMapper.selectById(taskId);
        if (testTask != null && "RUNNING".equals(testTask.getStatus())) {
            testTask.setStatus("FAILED");
            testTask.setEndTime(LocalDateTime.now());
            ttMapper.updateById(testTask);
        }

        if (runMarkedFailed) {
            tsProducer.send(userId, taskId, runId, "FAILED", message);
            tcProducer.send(userId, taskId, runId, "FAILED");
        }
    }

    public Boolean stop(Long taskId) {
        if (jmeterEngineMap.containsKey(taskId)) {
            TaskRuntimeContext context = jmeterEngineMap.get(taskId);
            context.setStopped(true);

            StandardJMeterEngine jmeterEngine = context.getEngine();
            jmeterEngine.stopTest();
            jmeterEngineMap.remove(taskId);
            return true;
        } else {
            log.info("当前任务-{}已停止", taskId);
            return false;
        }
    }

    @Scheduled(fixedRate = 5000)
    public void monitor() {
        for (Long taskId : jmeterEngineMap.keySet()) {
            TaskRuntimeContext context = jmeterEngineMap.get(taskId);
            if (context == null) {
                log.error("未找到任务运行上下文，taskId={}", taskId);
                jmeterEngineMap.remove(taskId);
                continue;
            }
            StandardJMeterEngine jmeterEngine = context.getEngine();
            if (jmeterEngine.isActive()) {
                continue;
            }

            TestTask testTask = ttMapper.selectById(taskId);
            TestTaskRun testTaskRun = ttrMapper.selectById(context.getRunId());
            if (testTaskRun == null) {
                log.error("未找到任务运行记录，taskId={},runId={}", taskId, context.getRunId());
                jmeterEngineMap.remove(taskId);
                continue;
            }
            if (context.isStopped()) {
                jmeterEngineMap.remove(taskId);
                continue;
            }
            if (testTask == null) {
                taskMetricAggregator.markRunFinished(testTaskRun.getId());
                testTaskRun.setStatus("FAILED");
                testTaskRun.setEndTime(LocalDateTime.now());
                ttrMapper.updateById(testTaskRun);
                log.error("未找到任务记录，taskId={}", taskId);
                jmeterEngineMap.remove(taskId);
                continue;
            }

            long runTime = Duration.between(context.getStartTime(), LocalDateTime.now()).getSeconds();
            taskMetricAggregator.markRunFinished(testTaskRun.getId());
            if (Math.abs(runTime - context.getDurationSeconds()) <= 5) {
                testTaskRun.setStatus("SUCCESS");
                testTask.setStatus("SUCCESS");
            } else {
                testTaskRun.setStatus("FAILED");
                testTask.setStatus("FAILED");
            }
            testTaskRun.setEndTime(LocalDateTime.now());
            ttrMapper.updateById(testTaskRun);

            testTask.setEndTime(LocalDateTime.now());
            ttMapper.updateById(testTask);

            tsProducer.send(testTask.getUserId(), taskId, testTaskRun.getId(), testTaskRun.getStatus(),
                    "任务执行" + (testTaskRun.getStatus().equals("SUCCESS") ? "成功" : "失败"));
            tcProducer.send(testTask.getUserId(), taskId, testTaskRun.getId(), testTaskRun.getStatus());
            jmeterEngineMap.remove(taskId);
        }
    }
}
