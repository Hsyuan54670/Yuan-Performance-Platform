package com.yuan.monitor.mq;

import com.yuan.api.test.mq.TestStatusMessage;
import com.yuan.monitor.collector.SystemMetricsPersistTask;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.mapper.TestStatusRecordMapper;
import com.yuan.monitor.runtime.ActiveRunRegistry;
import com.yuan.monitor.service.AlertEvaluateService;
import com.yuan.monitor.vo.TaskStatusPushVO;
import com.yuan.monitor.websocket.MonitorSessionManager;
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

    @Autowired
    private MonitorSessionManager monitorSessionManager;

    @Autowired
    private ActiveRunRegistry activeRunRegistry;

    @Autowired
    private SystemMetricsPersistTask systemMetricsPersistTask;

    @Autowired
    private AlertEvaluateService alertEvaluateService;

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
        activeRunRegistry.onStatusChanged(testStatusRecord);
        systemMetricsPersistTask.trackRunAtCurrentSecond(testStatusRecord);
        alertEvaluateService.recoverAlertsOnRunFinished(testStatusRecord);

        if (message.getTaskId() != null) {
            TaskStatusPushVO pushVO = new TaskStatusPushVO();
            pushVO.setMessageType("TASK_STATUS");
            pushVO.setTaskId(message.getTaskId());
            pushVO.setRunId(message.getRunId());
            pushVO.setStatus(message.getStatus());
            pushVO.setMessage(message.getMessage());
            pushVO.setTimestamp(message.getTimestamp());
            monitorSessionManager.broadcastTaskStatus(message.getTaskId(), pushVO);
        }
    }
}
