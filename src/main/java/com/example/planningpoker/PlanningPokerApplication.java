package com.example.planningpoker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class PlanningPokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanningPokerApplication.class, args);
	}

	@Bean
	Map<UUID, Sinks.Many<String>> roomSinks() {
		return new ConcurrentHashMap<>();
	}
}

