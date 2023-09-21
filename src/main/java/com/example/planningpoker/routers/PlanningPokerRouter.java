package com.example.planningpoker.routers;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class PlanningPokerRouter {
    @Bean
    public RouterFunction routerFunction() {
        return RouterFunctions
                .route()
                .path("/room", builder -> builder
                        .POST("", request -> ServerResponse.noContent().build())
                        .POST("/{id}/member", request -> ServerResponse.noContent().build())
                        .PUT("/{id}/member/{memberId}", request -> ServerResponse.noContent().build())
                )
                .build();
    }
}
