package com.yuan.test.mq;

import com.yuan.api.test.mq.TestStatusMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.yuan.api.mq.MqConstants.TEST_MONITOR_EXCHANGE;
import static com.yuan.api.mq.MqConstants.TEST_STATUS_ROUTING_KEY;

@Slf4j
@Component
public class TestStatusProducer {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(TestStatusMessage message) {
        log.info("Sending test status message: {}", message);
        rabbitTemplate.convertAndSend(TEST_MONITOR_EXCHANGE, TEST_STATUS_ROUTING_KEY, message);
    }

    public void send(Long userId,Long taskId, Long runId, String status, String message){
        TestStatusMessage statusMessage = new TestStatusMessage();
        statusMessage.setUserId(userId);
        statusMessage.setTaskId(taskId);
        statusMessage.setRunId(runId);
        statusMessage.setStatus(status);
        statusMessage.setMessage(message);
        statusMessage.setTimestamp(LocalDateTime.now());
        send(statusMessage);
    }

}