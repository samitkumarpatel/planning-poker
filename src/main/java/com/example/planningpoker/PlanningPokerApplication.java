package com.example.planningpoker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@SpringBootApplication
public class PlanningPokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanningPokerApplication.class, args);
	}

}

@Controller
class PlanningPokerController {
	@GetMapping("/{roomId}")
	public String joiningForm(@PathVariable String roomId) {
		return "joining-form";
	}

	@PostMapping("/{roomId}/join")
	public String join() {
		return "room";
	}
}

