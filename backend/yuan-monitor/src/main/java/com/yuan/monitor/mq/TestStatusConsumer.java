package com.yuan.monitor.mq;

import com.yuan.api.test.mq.TestStatusMessage;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.mapper.TestStatusRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.yuan.api.mq.MqConstants.TEST_STATUS_QUEUE;

@Slf4j
@Component
public class TestStatusConsumer {
    @Autowired
    private TestStatusRecordMapper testStatusRecordMapper;

    @RabbitListener(queues = TEST_STATUS_QUEUE )
    public void onMessage(TestStatusMessage message) {
        TestStatusRecord testStatusRecord = new TestStatusRecord();
        testStatusRecord.setUserId(message.getUserId());
        testStatusRecord.setStatus(message.getStatus());
        testStatusRecord.setMessage(message.getMessage());
        testStatusRecord.setTaskId(message.getTaskId());
        testStatusRecord.setRunId(message.getRunId());
        testStatusRecord.setCreatedAt(message.getTimestamp());
        log.info("Received test status message: {}", testStatusRecord);

        testStatusRecordMapper.insert(testStatusRecord);
    }
}
