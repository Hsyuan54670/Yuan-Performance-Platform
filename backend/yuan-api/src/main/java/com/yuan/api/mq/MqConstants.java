package com.yuan.api.mq;
public final class MqConstants {
    private MqConstants() {
    }
    public static final String TEST_MONITOR_EXCHANGE = "test.monitor.exchange";
    public static final String TEST_STATUS_ROUTING_KEY = "test.status";
    public static final String TEST_METRIC_ROUTING_KEY = "test.metric";
    public static final String TEST_STATUS_QUEUE = "monitor.test.status.queue";
    public static final String TEST_METRIC_QUEUE = "monitor.test.metric.queue";
}