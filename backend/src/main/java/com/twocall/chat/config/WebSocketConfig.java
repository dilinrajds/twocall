package com.twocall.chat.config;

import com.twocall.chat.controller.WsChatHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WsChatHandler wsChatHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(WsChatHandler wsChatHandler, WebSocketAuthInterceptor authInterceptor) {
        this.wsChatHandler = wsChatHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(wsChatHandler, "/ws")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }

    /**
     * Configure the underlying Tomcat/Jetty WebSocket container.
     * - asyncSendTimeout: drop stale sends after 10 s instead of hanging
     * - maxSessionIdleTimeout: server-side idle timeout 90 s (load-balancer is 60 s,
     *   but the OkHttp pingInterval=15 s means we never actually hit this)
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setAsyncSendTimeout(10_000L);       // 10 s async send timeout
        container.setMaxSessionIdleTimeout(90_000L);  // 90 s server-side idle timeout
        return container;
    }
}
