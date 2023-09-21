package com.example.planningpoker.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfiguration {
    final RoomWebSocketHandler roomWebSocketHandler;

    @Bean
    public HandlerMapping webSocketMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(1);

        // Map WebSocket URLs to handlers
        mapping.setUrlMap(Collections.singletonMap("/ws/room/*", roomWebSocketHandler));

        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
