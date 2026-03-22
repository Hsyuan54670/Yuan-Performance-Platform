package com.yuan.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayGlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayGlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        GatewayError error = resolveError(ex);
        logBySeverity(error.statusCode(), exchange.getRequest().getURI().getPath(), ex);

        response.setStatusCode(org.springframework.http.HttpStatus.valueOf(error.statusCode()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = serialize(error);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private GatewayError resolveError(Throwable ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);

        if (ex instanceof NotFoundException notFoundException) {
            return resolveGatewayNotFound(notFoundException);
        }
        if (ex instanceof MethodNotAllowedException) {
            return new GatewayError(HttpStatus.METHOD_NOT_ALLOWED, "请求方法不被支持");
        }
        if (ex instanceof ServerWebInputException) {
            return new GatewayError(HttpStatus.BAD_REQUEST, "请求参数不合法");
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return new GatewayError(
                    responseStatusException.getStatusCode().value(),
                    resolveResponseStatusMessage(responseStatusException)
            );
        }
        if (root instanceof TimeoutException || root instanceof SocketTimeoutException) {
            return new GatewayError(HttpStatus.GATEWAY_TIMEOUT, "网关请求超时");
        }
        if (root instanceof ConnectException) {
            return new GatewayError(HttpStatus.SERVICE_UNAVAILABLE, "下游服务不可用");
        }
        return new GatewayError(HttpStatus.INTERNAL_SERVER_ERROR, "网关内部异常");
    }

    private GatewayError resolveGatewayNotFound(NotFoundException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("Unable to find instance for")) {
            return new GatewayError(HttpStatus.SERVICE_UNAVAILABLE, "下游服务不可用");
        }
        return new GatewayError(HttpStatus.NOT_FOUND, "请求路径不存在");
    }

    private String resolveResponseStatusMessage(ResponseStatusException ex) {
        if (ex.getReason() != null && !ex.getReason().isBlank()) {
            return ex.getReason();
        }
        int status = ex.getStatusCode().value();
        return switch (status) {
            case HttpStatus.BAD_REQUEST -> "请求参数不合法";
            case HttpStatus.UNAUTHORIZED -> "未登录或登录已过期";
            case HttpStatus.FORBIDDEN -> "没有访问权限";
            case HttpStatus.NOT_FOUND -> "请求路径不存在";
            case HttpStatus.METHOD_NOT_ALLOWED -> "请求方法不被支持";
            case HttpStatus.SERVICE_UNAVAILABLE -> "下游服务不可用";
            case HttpStatus.GATEWAY_TIMEOUT -> "网关请求超时";
            default -> "网关处理请求时发生异常";
        };
    }

    private void logBySeverity(int statusCode, String path, Throwable ex) {
        if (statusCode >= HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("Gateway request failed, path={}, status={}", path, statusCode, ex);
            return;
        }
        log.warn("Gateway request rejected, path={}, status={}, message={}", path, statusCode, ex.getMessage());
    }

    private byte[] serialize(GatewayError error) {
        try {
            return objectMapper.writeValueAsBytes(R.fail(error.statusCode(), error.message()));
        } catch (JsonProcessingException jsonProcessingException) {
            String fallback = String.format(
                    "{\"code\":%d,\"message\":\"%s\",\"data\":null}",
                    error.statusCode(),
                    error.message()
            );
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

    private record GatewayError(int statusCode, String message) {
    }
}
