package com.example.planningpoker.ws;

import com.example.planningpoker.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomWebSocketHandler implements WebSocketHandler {
    final RoomRepository roomRepository;
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var roomId = session.getHandshakeInfo().getUri().getPath().split("/")[3];
        session.getAttributes().put("roomId", roomId);

        //map this id to a user
        var sink = roomRepository.getRoomSinks().get(UUID.fromString(roomId));
        Objects.requireNonNull(sink);

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
