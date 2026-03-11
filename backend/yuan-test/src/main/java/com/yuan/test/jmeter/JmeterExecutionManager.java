package com.yuan.test.jmeter;

import com.yuan.test.collector.TaskMetricAggregator;
import com.yuan.test.collector.TaskRuntimeContext;
import com.yuan.test.entity.TestTask;
import com.yuan.test.entity.TestTaskRun;
import com.yuan.test.mapper.TestTaskMapper;
import com.yuan.test.mapper.TestTaskRunMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jorphan.collections.ListedHashTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
    TaskMetricAggregator taskMetricAggregator;

    private final ConcurrentHashMap<Long, TaskRuntimeContext> jmeterEngineMap = new ConcurrentHashMap<>();

    public Boolean start(Long taskId, Long runId, ListedHashTree testPlanTree,Integer durationSeconds){
        if(jmeterEngineMap.containsKey(taskId)){
            log.info("当前任务-{}正在进行",taskId);
            return false;
        }
        StandardJMeterEngine jmeterEngine = new StandardJMeterEngine();
        jmeterEngine.configure(testPlanTree);
        TaskRuntimeContext context = new TaskRuntimeContext();
        context.setTaskId(taskId);
        context.setEngine(jmeterEngine);
        context.setStartTime(LocalDateTime.now());
        context.setRunId(runId);
        context.setDurationSeconds(durationSeconds);
        context.setStopped(false);
        jmeterEngineMap.put(taskId, context);
        jmeterEngine.run();
        return true;
    }

    public Boolean stop(Long taskId){
        if(jmeterEngineMap.containsKey(taskId)){
            TaskRuntimeContext context = jmeterEngineMap.get(taskId);
            context.setStopped(true);

            StandardJMeterEngine jmeterEngine = context.getEngine();
            jmeterEngine.stopTest();
            jmeterEngineMap.remove(taskId);
            return true;
        }else{
            log.info("当前任务-{}已停止",taskId);
            return false;
        }
    }

    @Scheduled(fixedRate = 5000)
    public void monitor(){
        log.debug("当前正在执行的任务列表：{}",jmeterEngineMap.keySet());
        for(Long taskId:jmeterEngineMap.keySet()){
            TaskRuntimeContext context = jmeterEngineMap.get(taskId);
            if(context == null){
                log.error("未找到任务运行上下文，taskId={}",taskId);
                jmeterEngineMap.remove(taskId);
                continue;
            }
            StandardJMeterEngine jmeterEngine = context.getEngine();
            if(jmeterEngine.isActive()){
                log.info("当前任务-{}正在执行",taskId);
            }else {
                // 判断任务停止的原因，是正常结束还是被强制停止
                TestTask testTask = ttMapper.selectById(taskId);
                TestTaskRun testTaskRun = ttrMapper.selectById(context.getRunId());
                if(testTaskRun == null){
                    log.error("未找到任务运行记录，taskId={},runId={}",taskId,context.getRunId());
                    jmeterEngineMap.remove(taskId);
                    continue;
                }
                if (context.isStopped()) {
                    jmeterEngineMap.remove(taskId);
                }else{
                    if (testTask != null) {
                        long runTime = Duration.between(context.getStartTime(), LocalDateTime.now()).getSeconds();
                        if(Math.abs(runTime-context.getDurationSeconds())<=5) {
                            // 正常结束
                            taskMetricAggregator.markRunFinished(testTaskRun.getId());
                            testTaskRun.setStatus("SUCCESS");
                            testTaskRun.setEndTime(LocalDateTime.now());
                            ttrMapper.updateById(testTaskRun);
                            testTask.setStatus("SUCCESS");
                        }else {
                            taskMetricAggregator.markRunFinished(testTaskRun.getId());
                            testTaskRun.setStatus("FAILED");
                            testTaskRun.setEndTime(LocalDateTime.now());
                            ttrMapper.updateById(testTaskRun);
                            testTask.setStatus("FAILED");
                        }
                        testTask.setEndTime(LocalDateTime.now());
                        ttMapper.updateById(testTask);
                        jmeterEngineMap.remove(taskId);
                    }else{
                        taskMetricAggregator.markRunFinished(testTaskRun.getId());
                        testTaskRun.setStatus("FAILED");
                        testTaskRun.setEndTime(LocalDateTime.now());
                        ttrMapper.updateById(testTaskRun);
                        log.error("未找到任务记录，taskId={}",taskId);
                        jmeterEngineMap.remove(taskId);
                    }
                }
            }
        }
    }
}
