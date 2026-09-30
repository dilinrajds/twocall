package com.twocall.chat.config;

import com.twocall.chat.controller.WsChatHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

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
}
