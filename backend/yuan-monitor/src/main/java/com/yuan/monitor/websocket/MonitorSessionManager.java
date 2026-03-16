package com.yuan.monitor.websocket;

import cn.hutool.json.JSONUtil;
import com.yuan.monitor.vo.RealtimeMetricPushVO;
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

    public void broadcast(Long taskId, RealtimeMetricPushVO payload) {
        CopyOnWriteArraySet<WebSocketSession> sessionSet = sessions.get(taskId);
        if (sessionSet == null || sessionSet.isEmpty()) {
            log.debug("Skip websocket broadcast because no subscribers, taskId={}, runId={}", taskId, payload.getRunId());
            return;
        }

        String payloadJson = JSONUtil.toJsonStr(payload);
        TextMessage message = new TextMessage(payloadJson);
        log.info("Broadcast realtime metric, taskId={}, runId={}, subscribers={}", taskId, payload.getRunId(), sessionSet.size());

        for (WebSocketSession session : sessionSet) {
            if (!session.isOpen()) {
                removeSession(taskId, session);
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (Exception e) {
                log.error("Failed to send websocket message, taskId={}, runId={}, sessionId={}", taskId, payload.getRunId(), session.getId(), e);
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
