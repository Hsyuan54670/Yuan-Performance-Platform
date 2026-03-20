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
        context.setUserId(userId);
        context.setTaskId(taskId);
        context.setEngine(jmeterEngine);
        context.setRunId(runId);
        context.setDurationSeconds(durationSeconds);
        context.setStopped(false);

        TaskRuntimeContext existingContext = jmeterEngineMap.putIfAbsent(taskId, context);
        if (existingContext != null) {
            return false;
        }

        try {
            taskExecutor.execute(() -> runTask(context));
        } catch (Exception e) {
            jmeterEngineMap.remove(taskId);
            throw new RuntimeException("任务提交线程池失败", e);
        }
        return true;
    }

    private void runTask(TaskRuntimeContext context) {
        try {
            LocalDateTime actualStart = LocalDateTime.now();
            context.setStartTime(actualStart);
            markRunStarted(context.getUserId(), context.getTaskId(), context.getRunId(), actualStart);
            context.getEngine().run();
            completeRunIfNeeded(context, LocalDateTime.now());
        } catch (Exception e) {
            markAsyncRunFailed(context.getUserId(), context.getTaskId(), context.getRunId(), "任务执行失败，异常信息：" + e.getMessage(), e);
        }
    }

    private void markRunStarted(Long userId, Long taskId, Long runId, LocalDateTime actualStart) {
        TestTaskRun testTaskRun = ttrMapper.selectById(runId);
        if (testTaskRun != null) {
            testTaskRun.setStartTime(actualStart);
            ttrMapper.updateById(testTaskRun);
        }

        TestTask testTask = ttMapper.selectById(taskId);
        if (testTask != null) {
            testTask.setStartTime(actualStart);
            ttMapper.updateById(testTask);
        }

        tsProducer.send(userId, taskId, runId, "RUNNING", "任务开始执行");
    }

    private void markAsyncRunFailed(Long userId, Long taskId, Long runId, String message, Exception exception) {
        log.error("执行任务异常，taskId={}, runId={}", taskId, runId, exception);
        jmeterEngineMap.remove(taskId);
        taskMetricAggregator.markRunFinished(runId);

        boolean runMarkedFailed = false;
        LocalDateTime finishedAt = LocalDateTime.now();
        TestTaskRun testTaskRun = ttrMapper.selectById(runId);
        if (testTaskRun != null && "RUNNING".equals(testTaskRun.getStatus())) {
            testTaskRun.setStatus("FAILED");
            testTaskRun.setEndTime(finishedAt);
            ttrMapper.updateById(testTaskRun);
            runMarkedFailed = true;
        }

        TestTask testTask = ttMapper.selectById(taskId);
        if (testTask != null && "RUNNING".equals(testTask.getStatus())) {
            testTask.setStatus("FAILED");
            testTask.setEndTime(finishedAt);
            ttMapper.updateById(testTask);
        }

        if (runMarkedFailed) {
            tsProducer.send(userId, taskId, runId, "FAILED", message);
            tcProducer.send(userId, taskId, runId, "FAILED");
        }
    }

    public Boolean stop(Long taskId) {
        TaskRuntimeContext context = jmeterEngineMap.get(taskId);
        if (context == null) {
            log.info("当前任务-{}已停止", taskId);
            return false;
        }

        context.setStopped(true);
        context.getEngine().stopTest();
        return true;
    }

    // 正常情况下 run() 返回时会立刻收尾；这个定时检测只保留作兜底，防止执行线程异常退出时上下文残留。
    @Scheduled(fixedRate = 1000)
    public void monitor() {
        for (Long taskId : jmeterEngineMap.keySet()) {
            TaskRuntimeContext context = jmeterEngineMap.get(taskId);
            if (context == null) {
                log.error("未找到任务运行上下文，taskId={}", taskId);
                jmeterEngineMap.remove(taskId);
                continue;
            }
            if (context.getEngine().isActive()) {
                continue;
            }
            completeRunIfNeeded(context, LocalDateTime.now());
        }
    }

    private void completeRunIfNeeded(TaskRuntimeContext context, LocalDateTime finishedAt) {
        if (context == null || context.getTaskId() == null || context.getRunId() == null) {
            return;
        }
        if (!jmeterEngineMap.remove(context.getTaskId(), context)) {
            return;
        }

        TestTaskRun testTaskRun = ttrMapper.selectById(context.getRunId());
        if (testTaskRun == null) {
            log.error("未找到任务运行记录，taskId={}, runId={}", context.getTaskId(), context.getRunId());
            return;
        }

        taskMetricAggregator.markRunFinished(testTaskRun.getId());

        String status;
        String message;
        if (context.isStopped()) {
            status = "STOPPED";
            message = "任务被用户停止";
        } else if (isExpectedDuration(context, finishedAt)) {
            status = "SUCCESS";
            message = "任务执行成功";
        } else {
            status = "FAILED";
            message = "任务执行失败";
        }

        testTaskRun.setStatus(status);
        testTaskRun.setEndTime(finishedAt);
        ttrMapper.updateById(testTaskRun);

        TestTask testTask = ttMapper.selectById(context.getTaskId());
        if (testTask != null) {
            testTask.setStatus(status);
            testTask.setEndTime(finishedAt);
            ttMapper.updateById(testTask);
        } else {
            log.error("未找到任务记录，taskId={}", context.getTaskId());
        }

        tsProducer.send(context.getUserId(), context.getTaskId(), context.getRunId(), status, message);
        tcProducer.send(context.getUserId(), context.getTaskId(), context.getRunId(), status);
    }

    private boolean isExpectedDuration(TaskRuntimeContext context, LocalDateTime finishedAt) {
        if (context.getStartTime() == null || context.getDurationSeconds() == null) {
            return false;
        }
        long runTime = Duration.between(context.getStartTime(), finishedAt).getSeconds();
        return Math.abs(runTime - context.getDurationSeconds()) <= 5;
    }
}
