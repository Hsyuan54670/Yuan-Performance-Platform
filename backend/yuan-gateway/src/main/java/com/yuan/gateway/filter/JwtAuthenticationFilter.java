package com.yuan.gateway.filter;

import cn.hutool.json.JSONUtil;
import com.yuan.common.result.R;
import com.yuan.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.yuan.common.constant.CommonConstant.REDIS_BLACKLIST_TOKEN;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelistedPath(path)) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange, "未登录");
        }
        token = token.substring(7);
        Map<String, Object> claims = isValidToken(token);
        if (claims == null) {
            return unauthorized(exchange, "Token无效");
        }

        try {
            claims = JwtUtil.parseToken(token);
        } catch (Exception e) {
            return unauthorized(exchange, "Token无效");
        }
        String userId = claims.get("userId").toString();

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", userId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        return chain.filter(mutatedExchange);
    }

    private boolean isWhitelistedPath(String path) {
        return "/auth/login".equals(path)
                || "/auth/refresh".equals(path)
                || "/test/mock/ping".equals(path)
                || "/test/mock/order".equals(path)
                || path.startsWith("/ws/");
    }

    public Map<String,Object> isValidToken(String token) {
        Map<String,Object> claims = null;
        try {
           claims=JwtUtil.parseToken(token);
        } catch (Exception e) {
            return null;
        }
        if(Boolean.TRUE.equals(stringRedisTemplate.hasKey(REDIS_BLACKLIST_TOKEN + token))) {
            return null;
        }
        return claims;

    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        R<Object> result = R.fail(HttpStatus.UNAUTHORIZED.value(), message);
        byte[] bytes = JSONUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
