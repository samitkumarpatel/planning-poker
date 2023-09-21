package com.example.planningpoker.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomWebSocketHandler implements WebSocketHandler {
    final Sinks.Many<String> sink = Sinks.many().replay().latestOrDefault("Welcome!");

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var userId = session.getId();
        var roomId = session.getHandshakeInfo().getUri().getPath().split("/")[3];
        log.info("RoomId : {}", roomId);
        log.info("UserId : {} ", userId);
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("roomId", roomId);
        //map this id to a user

        return session
                .send(sink.asFlux().map(session::textMessage))
                .and(session
                        .receive()
                        .doOnNext(message -> {
                            log.info("RoomId {} , UserId {} sent an message", session.getAttributes().get("roomId"), session.getAttributes().get("userId"));
                            sink.tryEmitNext(message.getPayloadAsText());
                        })
                        .then())
                .and(session
                        .closeStatus()
                        .doOnTerminate(() -> {
                            //client disconnect can capture here
                            log.info("userId {} disconnected from RoomId {}", session.getAttributes().get("userId"), session.getAttributes().get("roomId"));
                        }).then());
    }
}
