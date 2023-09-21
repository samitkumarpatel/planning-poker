package com.example.planningpoker.repositories;

import com.example.planningpoker.models.Member;
import com.example.planningpoker.models.Room;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RoomRepository {
    private final List<Room> db = new ArrayList<>();

    public Mono<Room> create(Room room) {
        return Mono
                .fromCallable(() -> {
                    var newRoom = new Room(UUID.randomUUID(),room.cards(),new ArrayList<>());
                    db.add(newRoom);
                    return newRoom;
                });
    }

    public Mono<Room> byId(UUID id) {
        return Mono
                .fromCallable(() -> {
                    return db
                            .stream()
                            .filter(room -> Objects.equals(room.id(), id))
                            .findFirst()
                            .orElseThrow();
                });
    }

    public Flux<Room> findAll() {
        return Flux.fromIterable(db);
    }

    //member
    public Mono<Member> findMemberByRoomIdAndMemberId(UUID roomId, String memberId) {
        return byId(roomId)
                .flatMap(room -> Mono.justOrEmpty(room.members()
                        .stream()
                        .filter(member -> member.id().equals(memberId))
                        .findFirst()
                        .orElseThrow()));
    }
    public Mono<Void> addMemberToRoom(UUID roomId, Member member) {
        return byId(roomId)
                .map(room -> room.members().add(member))
                .then();
    }

    public Mono<Void> updateMemberByRoomIdAndMemberId(UUID roomId, String memberId, Member member) {
        return byId(roomId)
                .map(room -> {
                    return room.members().stream().map(m -> {
                        if(m.id().equals(memberId)) {
                            return new Member(memberId, member.status(), m.name(), member.vote(), member.voted());
                        } else {
                            return m;
                        }
                    });
                })
                .then();

    }
}
