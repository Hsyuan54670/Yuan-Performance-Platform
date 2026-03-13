package com.yuan.test.mq;

import com.yuan.api.test.mq.TestMetricMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.yuan.api.mq.MqConstants.*;

@Slf4j
@Component
public class TestMetricsProducer {
    @Autowired
    RabbitTemplate rabbitTemplate;

    public void send(TestMetricMessage message) {
        log.info("Sending test metric message: {}", message);
        rabbitTemplate.convertAndSend(TEST_MONITOR_EXCHANGE,TEST_METRIC_ROUTING_KEY, message);
    }

    public void send(Long userId,Long taskId, Long runId, BigDecimal qps, BigDecimal p50,BigDecimal p90,BigDecimal p99, BigDecimal errorRate,LocalDateTime timestamp) {
        TestMetricMessage metricMessage = new TestMetricMessage();
        metricMessage.setUserId(userId);
        metricMessage.setTaskId(taskId);
        metricMessage.setRunId(runId);
        metricMessage.setQps(qps);
        metricMessage.setP50(p50);
        metricMessage.setP90(p90);
        metricMessage.setP99(p99);
        metricMessage.setErrorRate(errorRate);
        metricMessage.setTimestamp(timestamp);
        send(metricMessage);
    }
}