package com.yuan.test.collector;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
* 秒桶
* */
@Data
public class MetricBucket {

    // 精确到秒的时间点
    private LocalDateTime ts;

    // 这1秒的响应时间
    private List<Long> elapsedTimes = new CopyOnWriteArrayList<>();

    private int totalCount;

    private int errorCount;

    public void addSample(long elapsed,boolean success){
        totalCount++;
        elapsedTimes.add(elapsed);
        if(!success){
            errorCount++;
        }
    }

}
