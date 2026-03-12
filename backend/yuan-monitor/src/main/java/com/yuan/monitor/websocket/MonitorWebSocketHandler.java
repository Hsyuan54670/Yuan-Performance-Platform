package com.yuan.monitor.websocket;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MonitorWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    MonitorSessionManager monitorSessionManager;
    // 连接成立后
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long taskId = (Long) session.getAttributes().get("taskId");
        if(taskId != null){
            monitorSessionManager.addSession(taskId, session);
        }
    }

    // 连接关闭后
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long taskId = (Long) session.getAttributes().get("taskId");
        if(taskId != null){
            monitorSessionManager.removeSession(taskId, session);
        }
    }

    // 传输错误
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long taskId = (Long) session.getAttributes().get("taskId");
        if (taskId != null) {
            monitorSessionManager.removeSession(taskId, session);
        }
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}