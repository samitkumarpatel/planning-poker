package com.example.planningpoker.ws;

import com.example.planningpoker.models.Member;
import com.example.planningpoker.models.Room;
import com.example.planningpoker.repositories.RoomRepository;
import com.example.planningpoker.services.PlanningPokerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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
                            log.info("RoomId {} , UserId {} sent an message {}", session.getAttributes().get("roomId"), session.getAttributes().get("userId"), message.getPayloadAsText());
                            handleWebSockerMessage(session.getId(), message, roomUUID);
                        })
                        .then())
                .and(session
                        .closeStatus()
                        .doOnTerminate(() -> {
                            //client disconnect can capture here
                            log.info("userId {} disconnected from RoomId {}", session.getAttributes().get("userId"), session.getAttributes().get("roomId"));
                            roomRepository
                                    .updateMemberByRoomIdAndMemberId(
                                            roomUUID,
                                            session.getId(),
                                            new Member(session.getId(), null, false, "", null, false)
                                    ).subscribe();
                            service.raiseWsEvent(roomUUID).subscribe();
                        }).then());
    }

    private void handleWebSockerMessage(String sessionId, WebSocketMessage message, UUID roomUUID) {
        //onopen event
        try {
            var map = objectMapper.readValue(message.getPayloadAsText(), Map.class);
            switch ((String)map.get("event")) {
                case "JOIN":
                    //just update the customId to session
                    roomRepository
                            .updateMemberByRoomIdAndMemberId(
                                    roomUUID,
                                    (String)map.get("id"),
                                    new Member(sessionId, (String)map.get("role"), true, "", null, false)
                            ).subscribe();
                    break;
                case "VOTE":
                    //maintain vote in another map and mapped it when OBSERVER revel the score
                    roomRepository
                            .memberVotes.put(sessionId, String.valueOf(map.get("vote")));
                    roomRepository
                            .updateMemberByRoomIdAndMemberId(
                                    roomUUID,
                                    sessionId,
                                    new Member(sessionId, null, true, null, "HIDDEN", true)
                            ).subscribe();
                    break;
                case "RESULT":
                    roomRepository
                            .roomById(roomUUID)
                            .map(room -> {
                                var updateMembers = room.members()
                                        .stream()
                                        .map(member -> new Member(member.id(), member.role(), member.status(),member.name(),roomRepository.memberVotes.get(member.id()),member.voted()))
                                        .collect(Collectors.toList());
                                log.info("Update Members {}", updateMembers);
                                return new Room(roomUUID,room.cards(),updateMembers);
                            })
                            .map(roomRepository::create)
                            .subscribe();
                    break;
                case "RESET":
                    roomRepository
                            .roomById(roomUUID)
                            .map(room -> {
                                var updateMembers = room.members()
                                        .stream()
                                        .map(member -> new Member(member.id(), member.role(), member.status(),member.name(),"",false))
                                        .collect(Collectors.toList());
                                log.info("Reset Members {}", updateMembers);
                                return new Room(roomUUID,room.cards(),updateMembers);
                            })
                            .map(roomRepository::create)
                            .subscribe();
                    break;
                default:
                    log.info("UNKNOWN event- Ignored");
                    break;
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage());
        }
        service.raiseWsEvent(roomUUID).subscribe();
    }
}
