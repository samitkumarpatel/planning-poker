package com.example.planningpoker.routers;

import com.example.planningpoker.models.Member;
import com.example.planningpoker.models.Room;
import com.example.planningpoker.repositories.RoomRepository;
import com.fasterxml.jackson.databind.ser.std.UUIDSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class PlanningPokerRouter {
    final RoomRepository roomRepository;
    @Bean
    public RouterFunction routerFunction() {
        return RouterFunctions
                .route()
                .path("/room", builder -> builder
                        .POST("", this::createRoom)
                        .GET("", this::allRoom)
                        .GET("/{id}/cards", this::cardByRoomId)
                        .POST("/{id}/member", this::addMemberToARoom)
                        .PUT("/{id}/member/{memberId}", this::updateARoomMember)
                )
                .build();
    }

    private Mono<ServerResponse> cardByRoomId(ServerRequest request) {
        var roomId = UUID.fromString(request.pathVariable("id"));
        return roomRepository
                .roomById(roomId)
                .map(Room::cards)
                .flatMap(ServerResponse.ok()::bodyValue);
    }

    private Mono<ServerResponse> updateARoomMember(ServerRequest request) {
        var roomId = UUID.fromString(request.pathVariable("id"));
        var memberId = request.pathVariable("memberId");
        return request
                .bodyToMono(Member.class)
                .flatMap(member -> roomRepository.updateMemberByRoomIdAndMemberId(roomId, memberId, member))
                .flatMap(ServerResponse.ok()::bodyValue);
    }

    private Mono<ServerResponse> addMemberToARoom(ServerRequest request) {
        var roomId = UUID.fromString(request.pathVariable("id"));
        return request
                .bodyToMono(Member.class)
                .flatMap(member -> roomRepository.addMemberToRoom(roomId, member))
                .flatMap(ServerResponse.ok()::bodyValue);
    }

    private Mono<ServerResponse> allRoom(ServerRequest request) {
        return ServerResponse.ok().body(roomRepository.findAll(), Room.class);
    }

    private Mono<ServerResponse> createRoom(ServerRequest request) {
        return request
                .bodyToMono(Room.class)
                .flatMap(roomRepository::create)
                .flatMap(ServerResponse.ok()::bodyValue)
                .log();
    }
}
