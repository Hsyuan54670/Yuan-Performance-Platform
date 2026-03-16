package com.yuan.analysis.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MetricSeriesBundle {
    /** 采样间隔，单位秒 */
    private Integer samplingIntervalSeconds;

    /** 秒级指标点列表 */
    private List<SecondMetricPoint> points = new ArrayList<>();
}
