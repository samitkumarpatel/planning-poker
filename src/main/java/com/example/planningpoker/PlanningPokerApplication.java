package com.example.planningpoker;

import com.example.planningpoker.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@SpringBootApplication
public class PlanningPokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanningPokerApplication.class, args);
	}

}

@Controller
@RequiredArgsConstructor
class PlanningPokerController {
	final RoomRepository roomRepository;

	@GetMapping("/server/{roomId}")
	public Mono<String> joiningForm(@PathVariable String roomId) {
		return roomRepository
				.roomById(UUID.fromString(roomId))
				.map(room -> "room");
	}
}

