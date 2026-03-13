package com.yuan.monitor.websocket;


import cn.hutool.json.JSONUtil;
import com.yuan.monitor.vo.RealtimeMetricPushVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class MonitorSessionManager {

    private final ConcurrentHashMap<Long,
            CopyOnWriteArraySet<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void addSession(Long taskId,WebSocketSession session) {
        sessions.computeIfAbsent(taskId,
                k -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void removeSession(Long taskId,WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> sessionSet = sessions.get(taskId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                sessions.remove(taskId);
            }
        }
    }

    // 广播
    public void broadcast(Long taskId, RealtimeMetricPushVO payload) {

        CopyOnWriteArraySet<WebSocketSession> sessionSet = sessions.get(taskId);
        if (sessionSet == null ||  sessionSet.isEmpty()) {
            return;
        }
        // 转json
        String payloadJson = JSONUtil.toJsonStr(payload);
        // 发TextMessage
        TextMessage message = new TextMessage(payloadJson);



        for (WebSocketSession session : sessionSet) {
            if(!session.isOpen()) {
                removeSession(taskId, session);
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.error("发送WebSocket消息失败: {}", e.getMessage());
                removeSession(taskId, session);

                try{
                    if(session.isOpen()){
                        session.close(CloseStatus.SERVER_ERROR);
                    }
                }catch (IOException ex){
                    log.warn("关闭WebSocket连接失败: {}", ex.getMessage());
                }
            }
        }

    }

}
