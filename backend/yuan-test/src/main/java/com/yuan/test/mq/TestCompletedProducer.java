package com.yuan.test.mq;

import com.yuan.api.test.mq.TestCompletedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.yuan.api.mq.MqConstants.TEST_COMPLETED_ROUTING_KEY;
import static com.yuan.api.mq.MqConstants.YUAN_TEST_EXCHANGE;


@Slf4j
@Component
public class TestCompletedProducer {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(TestCompletedMessage message) {
        log.info("Sending test completed message: {}", message);
        rabbitTemplate.convertAndSend(YUAN_TEST_EXCHANGE, TEST_COMPLETED_ROUTING_KEY, message);
    }

    public void send(Long userId, Long taskId, Long runId, String status) {
        TestCompletedMessage message = new TestCompletedMessage();
        message.setUserId(userId);
        message.setTaskId(taskId);
        message.setRunId(runId);
        message.setStatus(status);
        message.setEndTime(LocalDateTime.now());
        send(message);
    }
}
