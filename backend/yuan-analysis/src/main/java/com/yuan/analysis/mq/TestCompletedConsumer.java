package com.yuan.analysis.mq;

import com.yuan.analysis.service.AnalysisService;
import com.yuan.api.test.mq.TestCompletedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.yuan.api.mq.MqConstants.TEST_COMPLETED_QUEUE;

@Slf4j
@Component
public class TestCompletedConsumer {
    @Autowired
    private AnalysisService analysisService;

    @RabbitListener(queues = TEST_COMPLETED_QUEUE)
    public void onMessage(TestCompletedMessage message) {
        log.info("Received test completed message: {}", message);
        analysisService.handleMessage(message);
    }
}
