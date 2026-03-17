package com.yuan.monitor.mq;

import com.yuan.api.test.mq.TestMetricMessage;
import com.yuan.monitor.entity.MonitorMetricsRecord;
import com.yuan.monitor.mapper.MonitorMetricsRecordMapper;
import com.yuan.monitor.service.AlertEvaluateService;
import com.yuan.monitor.vo.RealtimeMetricPushVO;
import com.yuan.monitor.websocket.MonitorSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.yuan.api.mq.MqConstants.TEST_METRIC_QUEUE;

@Slf4j
@Component
public class TestMetricsConsumer {

    @Autowired
    MonitorMetricsRecordMapper mmrMapper;
    @Autowired
    AlertEvaluateService alertEvaluateService;
    @Autowired
    MonitorSessionManager monitorSessionManager;

    @RabbitListener(queues = TEST_METRIC_QUEUE)
    public void onMessage(TestMetricMessage message) {
        log.info("Received Test metric message: {}", message);
        MonitorMetricsRecord mmr = new MonitorMetricsRecord();
        mmr.setTaskId(message.getTaskId());
        mmr.setRunId(message.getRunId());
        mmr.setCreatedAt(message.getTimestamp());
        mmr.setQps(message.getQps());
        mmr.setP50(message.getP50());
        mmr.setP90(message.getP90());
        mmr.setP99(message.getP99());
        mmr.setErrorRate(message.getErrorRate());
        mmrMapper.insert(mmr);

        RealtimeMetricPushVO pushVO = new RealtimeMetricPushVO();
        pushVO.setMessageType("METRIC");
        pushVO.setTaskId(message.getTaskId());
        pushVO.setRunId(message.getRunId());
        pushVO.setQps(message.getQps());
        pushVO.setP50(message.getP50());
        pushVO.setP90(message.getP90());
        pushVO.setP99(message.getP99());
        pushVO.setErrorRate(message.getErrorRate());
        pushVO.setTimestamp(message.getTimestamp());
        monitorSessionManager.broadcastMetric(message.getTaskId(), pushVO);

        try {
            alertEvaluateService.evaluateTestMetric(message);
        } catch (Exception e) {
            log.error("Failed to evaluate alert rules for metric message, runId={}, taskId={}", message.getRunId(), message.getTaskId(), e);
        }
    }
}
