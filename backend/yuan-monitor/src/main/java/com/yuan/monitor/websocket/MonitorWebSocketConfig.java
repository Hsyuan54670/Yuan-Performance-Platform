package com.yuan.monitor.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class MonitorWebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private MonitorWebSocketHandler monitorWebSocketHandler;

    @Autowired
    private MonitorHandshakeInterceptor monitorHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(
                        monitorWebSocketHandler,
                        "/ws/monitor/{taskId}")
                .addInterceptors(monitorHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
