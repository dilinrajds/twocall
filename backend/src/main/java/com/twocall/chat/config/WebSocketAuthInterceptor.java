package com.twocall.chat.config;

import com.twocall.chat.security.DevicePrincipal;
import com.twocall.chat.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private final JwtTokenProvider tokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (!StringUtils.hasText(token)) {
                String authHeader = servletRequest.getServletRequest().getHeader("Authorization");
                if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                UUID deviceId = tokenProvider.getDeviceIdFromToken(token);
                UUID pairId = tokenProvider.getPairIdFromToken(token);
                DevicePrincipal principal = new DevicePrincipal(deviceId, pairId, "");
                attributes.put("devicePrincipal", principal);
                attributes.put("deviceId", deviceId);
                attributes.put("pairId", pairId);
                return true;
            }
        }
        log.warn("WebSocket handshake rejected: Missing or invalid token");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }
}
