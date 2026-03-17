package com.yuan.monitor.websocket;

import cn.hutool.json.JSONUtil;
import com.yuan.monitor.vo.RealtimeMetricPushVO;
import com.yuan.monitor.vo.TaskStatusPushVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class MonitorSessionManager {

    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void addSession(Long taskId, WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> sessionSet = sessions.computeIfAbsent(taskId, k -> new CopyOnWriteArraySet<>());
        sessionSet.add(session);
        log.info("Registered websocket session, taskId={}, sessionId={}, subscribers={}", taskId, session.getId(), sessionSet.size());
    }

    public void removeSession(Long taskId, WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> sessionSet = sessions.get(taskId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                sessions.remove(taskId);
            }
            log.info("Removed websocket session, taskId={}, sessionId={}, subscribers={}", taskId, session.getId(), sessionSet.size());
        }
    }

    public void broadcastMetric(Long taskId, RealtimeMetricPushVO payload) {
        broadcast(taskId, payload, payload.getRunId(), "realtime metric");
    }

    public void broadcastTaskStatus(Long taskId, TaskStatusPushVO payload) {
        broadcast(taskId, payload, payload.getRunId(), "task status");
    }

    private void broadcast(Long taskId, Object payload, Long runId, String payloadType) {
        CopyOnWriteArraySet<WebSocketSession> sessionSet = sessions.get(taskId);
        if (sessionSet == null || sessionSet.isEmpty()) {
            log.debug("Skip websocket broadcast because no subscribers, taskId={}, runId={}, payloadType={}", taskId, runId, payloadType);
            return;
        }

        String payloadJson = JSONUtil.toJsonStr(payload);
        TextMessage message = new TextMessage(payloadJson);
        log.info("Broadcast websocket payload, taskId={}, runId={}, payloadType={}, subscribers={}", taskId, runId, payloadType, sessionSet.size());

        for (WebSocketSession session : sessionSet) {
            if (!session.isOpen()) {
                removeSession(taskId, session);
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (Exception e) {
                log.error("Failed to send websocket message, taskId={}, runId={}, payloadType={}, sessionId={}", taskId, runId, payloadType, session.getId(), e);
                removeSession(taskId, session);

                try {
                    if (session.isOpen()) {
                        session.close(CloseStatus.SERVER_ERROR);
                    }
                } catch (Exception closeEx) {
                    log.warn("Failed to close broken websocket session, sessionId={}", session.getId(), closeEx);
                }
            }
        }
    }
}
