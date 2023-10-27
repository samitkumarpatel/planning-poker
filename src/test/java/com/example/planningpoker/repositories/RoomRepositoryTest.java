package com.example.planningpoker.repositories;

import com.example.planningpoker.models.Member;
import com.example.planningpoker.models.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class RoomRepositoryTest {
    private RoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        roomRepository = new RoomRepository();
    }

    @Test
    void testCRUD() {

        // roomRepository.create()
        var room = new Room(null, List.of("a","b","c"), null);
        StepVerifier
                .create(roomRepository.create(room))
                .consumeNextWith(r -> {
                    assertNotNull(r.id());
                    assertEquals(List.of("a","b","c"), r.cards());
                    assertEquals(0, r.members().size());
                })
                .verifyComplete();
        var roomId = roomRepository.findAll().map(r -> r.id()).blockFirst();

        var member1 = new Member("1", "Participant", true,"User One",null,false);
        var member1ToBeUpdated = new Member(member1.id(),"Participant", true, member1.name(), "3", true);
        var member2 = new Member("2" , "Participant",true,"User Two",null,false);

        assertAll(
                "CRUD OPERATION",
                () -> {
                    //findAll
                    StepVerifier
                            .create(roomRepository.findAll())
                            .expectNextCount(1)
                            .verifyComplete();
                },
                () -> {
                    //roomById()
                    StepVerifier
                            .create(roomRepository.roomById(roomId))
                            .consumeNextWith(r -> {
                                assertNotNull(r.id());
                                assertEquals(List.of("a","b","c"), r.cards());
                                assertEquals(0, r.members().size());
                            })
                            .verifyComplete();

                    StepVerifier
                            .create(roomRepository.roomById(UUID.fromString("a6a24419-4b2d-42db-bf02-f36da1ce877b")))
                            .verifyErrorMessage("Room not found");
                },
                () -> {
                    //addMemberToRoom()
                    StepVerifier
                            .create(roomRepository.addMemberToRoom(roomId,member1))
                            .consumeNextWith(r -> {
                                assertTrue(r.booleanValue());
                            })
                            .verifyComplete();

                    StepVerifier
                            .create(roomRepository.addMemberToRoom(roomId,member2))
                            .consumeNextWith(r -> {
                                assertTrue(r.booleanValue());
                            })
                            .verifyComplete();
                },
                () -> {
                    //updateMemberByRoomIdAndMemberId
                    StepVerifier
                            .create(roomRepository.updateMemberByRoomIdAndMemberId(roomId,member1.id(),member1ToBeUpdated))
                            .verifyComplete();
                },
                () -> {
                    //verify that It's updated
                    StepVerifier
                            .create(roomRepository.findMemberByRoomIdAndMemberId(roomId, member1.id()))
                            .consumeNextWith(r -> {
                                assertEquals(member1ToBeUpdated, r);
                            })
                            .verifyComplete();
                },
                () -> {
                    //verify that it has still 2 member after update
                    StepVerifier
                            .create(roomRepository.roomById(roomId))
                            .consumeNextWith(r -> {
                                assertEquals(2, r.members().size());
                            })
                            .verifyComplete();
                }
        );
    }
}