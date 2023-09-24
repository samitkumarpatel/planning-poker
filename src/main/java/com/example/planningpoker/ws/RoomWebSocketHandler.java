package com.example.planningpoker.ws;

import com.example.planningpoker.models.Member;
import com.example.planningpoker.repositories.RoomRepository;
import com.example.planningpoker.services.PlanningPokerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomWebSocketHandler implements WebSocketHandler {
    final RoomRepository roomRepository;
    final PlanningPokerService service;
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        var roomId = session.getHandshakeInfo().getUri().getPath().split("/")[3];
        log.info("RoomId {}", roomId);
        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("userId", session.getId());
        var roomUUID = UUID.fromString(roomId);
        //map this id to a user
        var sink = roomRepository.getRoomSinks().get(roomUUID);
        Objects.requireNonNull(sink);

        return session
                .send(sink.asFlux().map(session::textMessage))
                .and(session
                        .receive()
                        .doOnNext(message -> {
                            log.info("RoomId {} , UserId {} sent an message", session.getAttributes().get("roomId"), session.getAttributes().get("userId"));
                            //onopen event
                            try {
                                var map = objectMapper.readValue(message.getPayloadAsText(), Map.class);
                                roomRepository
                                        .updateMemberByRoomIdAndMemberId(roomUUID, (String)map.get("id"),new Member(session.getId(), (String)map.get("role"), true, "", null, false))
                                        .subscribe();
                            } catch (Exception e) {
                                log.error("{}", e.getMessage());
                            }
                            service.raiseWsEvent(roomUUID).subscribe();
                        })
                        .then())
                .and(session
                        .closeStatus()
                        .doOnTerminate(() -> {
                            //client disconnect can capture here
                            log.info("userId {} disconnected from RoomId {}", session.getAttributes().get("userId"), session.getAttributes().get("roomId"));

                            roomRepository
                                    .updateMemberByRoomIdAndMemberId(roomUUID, session.getId(),new Member(session.getId(), null, false, "", null, false))
                                    .subscribe();
                            service.raiseWsEvent(roomUUID).subscribe();
                        }).then());
    }
}
