package com.yuan.monitor.websocket;

import com.yuan.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static com.yuan.common.constant.CommonConstant.REDIS_BLACKLIST_TOKEN;

@Component
public class MonitorHandshakeInterceptor implements HandshakeInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        try {
            String token = UriComponentsBuilder.fromUri(request.getURI()).build()
                    .getQueryParams()
                    .getFirst("token");
            if (token == null || token.isBlank()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(REDIS_BLACKLIST_TOKEN + token))) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            Map<String, Object> claims = JwtUtil.parseToken(token);
            if (claims == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // 获取taskId
            String path = request.getURI().getPath();
            String taskIdText = path.substring(path.lastIndexOf('/') + 1);
            Long taskId = Long.parseLong(taskIdText);

            Long userId = Long.parseLong(claims.get("userId").toString());

            // 将taskId和userId存储在attributes中，供WebSocketHandler使用
            attributes.put("taskId", taskId);
            attributes.put("userId", userId);

            return true;
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }
}
