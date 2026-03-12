package com.yuan.test.collector;


import com.yuan.test.entity.TestMetricSecond;
import com.yuan.test.mapper.TestMetricSecondMapper;
import com.yuan.test.mq.TestMetricsProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.samplers.SampleResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/*
* 聚合器
* - 负责接收样本数据，按照 runId 和时间（秒）进行聚合
* - 定时将聚合结果写入数据库
* */
@Slf4j
@Component
public class TaskMetricAggregator {
    private final TestMetricsProducer tmProducer;
    private final TestMetricSecondMapper tmsMapper;
    private final Set<Long> finishedRuns = ConcurrentHashMap.newKeySet();
    private final
    ConcurrentHashMap<Long,
            ConcurrentHashMap<LocalDateTime, MetricBucket>> buckets = new ConcurrentHashMap<>();
    private final
    ConcurrentHashMap<Long,Long> runTaskMap = new ConcurrentHashMap<>();

    public TaskMetricAggregator(TestMetricSecondMapper tmsMapper, TestMetricsProducer testMetricsProducer) {
        this.tmsMapper = tmsMapper;
        this.tmProducer = testMetricsProducer;
    }

    public void record(Long taskId, Long runId,SampleResult sampleResult){
        runTaskMap.put(runId, taskId);
        // 1. 取结束时间
        long endTime = sampleResult.getEndTime();
        Instant instant = Instant.ofEpochMilli(endTime);
        // 2.转成 LocalDateTime
        LocalDateTime endTimeSecond = LocalDateTime
                .ofInstant(instant, ZoneId.systemDefault())
                .withNano(0);   // 3.截断到秒
        // 3.找到对应桶
        MetricBucket metricBucket = buckets
                .computeIfAbsent(runId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(endTimeSecond, k -> {
                    MetricBucket bucket = new MetricBucket();
                    bucket.setTs(endTimeSecond);
                    return bucket;
                });
        // 4.添加样本
        metricBucket.addSample(sampleResult.getTime(),sampleResult.isSuccessful());
    }


    @Scheduled(fixedRate = 1000)
    public void flush(){
        // 1. 遍历 buckets
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.minusSeconds(1);
        for (Long runId : buckets.keySet()) {
            ConcurrentHashMap<LocalDateTime, MetricBucket> taskBuckets = buckets.get(runId);
            for (LocalDateTime bucketTime : taskBuckets.keySet()) {
                if (bucketTime.isBefore(expireTime)) {
                    // 2. 写入统计结果
                    MetricBucket metricBucket = taskBuckets.get(bucketTime);
                    List<Long> elapsedTimes = new CopyOnWriteArrayList<>(metricBucket.getElapsedTimes());
                    BigDecimal p50 = calculateP(elapsedTimes,50);
                    BigDecimal p90 = calculateP(elapsedTimes,90);
                    BigDecimal p99 = calculateP(elapsedTimes,99);
                    BigDecimal qps = new BigDecimal(metricBucket.getTotalCount());
                    BigDecimal errorRate = new BigDecimal(metricBucket.getErrorCount())
                            .divide(qps.compareTo(BigDecimal.ZERO)!=0 ? qps : BigDecimal.ONE, 2, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100));
                    if(!runTaskMap.containsKey(runId)){
                        log.error("runId={}, bucketTime={}", runId, bucketTime);
                        continue;
                    }
                    tmsMapper.insert(new TestMetricSecond(
                            null,
                            runTaskMap.get(runId),
                            runId,
                            bucketTime,
                            qps,
                            p50,
                            p90,
                            p99,
                            errorRate
                    ));
                    tmProducer.send(runTaskMap.get(runId),
                            runId,
                            qps,
                            p50,
                            p90,
                            p99,
                            errorRate,
                            bucketTime
                    );
                    // 3. 从 buckets 中移除过期桶
                    taskBuckets.remove(bucketTime);
                }
            }
            // 4. 如果这个 run 已经结束且没有桶了，就清理这个 run 的记录
            if (finishedRuns.contains(runId) && taskBuckets.isEmpty()) {
                buckets.remove(runId);
                runTaskMap.remove(runId);
                finishedRuns.remove(runId);
            }
        }
    }

    private BigDecimal calculateP(List<Long> elapsedTimes,int p){
        if(elapsedTimes.isEmpty()){
            return BigDecimal.ZERO;
        }
        // 1. 排序
        Collections.sort(elapsedTimes);
        // 2. 计算索引
        int index = (int) Math.ceil(p / 100.0 * elapsedTimes.size()) - 1;
        // 3. 返回结果
        return new BigDecimal(elapsedTimes.get(index)).setScale(2, RoundingMode.HALF_UP);
    }

    public void markRunFinished(Long runId) {
        finishedRuns.add(runId);
        if(!buckets.containsKey(runId)){
            runTaskMap.remove(runId);
            finishedRuns.remove(runId);
        }

    }

}
