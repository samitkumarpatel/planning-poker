package com.example.planningpoker.repositories;

import com.example.planningpoker.models.Member;
import com.example.planningpoker.models.Room;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Service
public class RoomRepository {
    private final static List<Room> db = new ArrayList<>();
    private final static Map<UUID, Sinks.Many<String>> roomSinks = new ConcurrentHashMap<>();
    public Map<UUID, Sinks.Many<String>> getRoomSinks() {
        return roomSinks;
    }

    public Mono<Room> create(Room room) {
        return Mono
                .fromCallable(() -> {
                    var newRoom = new Room(UUID.randomUUID(),room.cards(),new ArrayList<>());
                    db.add(newRoom);
                    return newRoom;
                })
                .doOnNext(room1 -> roomSinks.put(room1.id(), Sinks.many().replay().latestOrDefault("Welcome!")));
    }

    public Mono<Room> roomById(UUID id) {
        return Mono
                .fromCallable(() -> db
                            .stream()
                            .filter(room -> Objects.equals(room.id(), id))
                            .findFirst()
                            .orElseThrow());
    }

    public Flux<Room> findAll() {
        return Flux.fromIterable(db);
    }

    //member
    public Mono<Member> findMemberByRoomIdAndMemberId(UUID roomId, String memberId) {
        return roomById(roomId)
                .flatMap(room -> Mono.justOrEmpty(room.members()
                        .stream()
                        .filter(member -> member.id().equals(memberId))
                        .findFirst()
                        .orElseThrow()));
    }
    public Mono<Boolean> addMemberToRoom(UUID roomId, Member member) {
        return roomById(roomId)
                .map(room -> room.members().add(member));
    }

    public Mono<Void> updateMemberByRoomIdAndMemberId(UUID roomId, String memberId, Member member) {
        return roomById(roomId)
                .map(room -> {
                    var members = room.members()
                            .stream()
                            .map(m -> {
                                if(m.id().equals(memberId)) {
                                    return new Member(
                                            member.id(),
                                            nonNull(member.role()) ? member.role() : m.role(),
                                            member.status(),
                                            m.name(),
                                            nonNull(member.vote()) ? member.vote() : m.vote(),
                                            member.voted()
                                    );
                                } else {
                                    return m;
                                }
                            })
                            .collect(Collectors.toList());
                    db.remove(room);
                    db.add(new Room(room.id(),room.cards(),members));
                    return Void.class;
                })
                .then();

    }
}
