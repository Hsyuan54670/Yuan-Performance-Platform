package com.yuan.monitor.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MonitorWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    MonitorSessionManager monitorSessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long taskId = (Long) session.getAttributes().get("taskId");
        if (taskId != null) {
            monitorSessionManager.addSession(taskId, session);
            log.info("WebSocket connected, taskId={}, sessionId={}", taskId, session.getId());
        } else {
            log.warn("WebSocket connected without taskId, sessionId={}", session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long taskId = (Long) session.getAttributes().get("taskId");
        if (taskId != null) {
            monitorSessionManager.removeSession(taskId, session);
            log.info("WebSocket closed, taskId={}, sessionId={}, status={}", taskId, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long taskId = (Long) session.getAttributes().get("taskId");
        if (taskId != null) {
            monitorSessionManager.removeSession(taskId, session);
        }
        log.error("WebSocket transport error, taskId={}, sessionId={}", taskId, session.getId(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}
