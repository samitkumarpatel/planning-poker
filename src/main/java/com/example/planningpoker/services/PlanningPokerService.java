package com.example.planningpoker.services;

import com.example.planningpoker.models.Member;
import com.example.planningpoker.models.Room;
import com.example.planningpoker.repositories.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanningPokerService {
    final RoomRepository roomRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    public Mono<Void> raiseWsEvent(UUID roomId) {
        return roomRepository
                .roomById(roomId)
                .doOnNext(room -> log.info("Members : {}", room.members()))
                .mapNotNull(room -> membersAsString(room.members()))
                .map(eventPayload -> roomRepository.getRoomSinks().get(roomId).tryEmitNext(eventPayload).isSuccess())
                .then();
    }

    private String membersAsString(List<Member> members) {
        try {
            return objectMapper.writeValueAsString(members);
        } catch (Exception e) {
            return "[]";
        }
    }
}
