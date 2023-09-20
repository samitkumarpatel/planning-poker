package com.example.planningpoker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

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

record Member(String id, String name, String vote, boolean voted){}
record Room(UUID uuid, List<String> cards, List<Member> members){};

@Service
@RequiredArgsConstructor
//TODO everything from here to RoomRepository
class MemberRepository {

	final RoomRepository roomRepository;
	public Mono<Member> addMemberToTheRoom(UUID roomId, Member member) {
		return roomRepository
				.read(roomId)
				.map(room -> {
					var members = room.members();
					members.add(member);
					return new Room(room.uuid(), room.cards(), members);
				})
				.map(room -> member)
				.flatMap(Mono::just);
	}

	public Flux<Member> allMemberFromRoom(UUID roomId) {
		return roomRepository
				.read(roomId)
				.flatMapIterable(Room::members);
	}

}

@Component
@RequiredArgsConstructor
class RoomRepository {
	private static final List<Room> rooms = new ArrayList<>();
	//TODO move this to a centralise place
	private static final Map<UUID, Sinks.Many<String>> roomSinks = new ConcurrentHashMap<>();
	//TODO move this to a centralise place
	public Sinks.Many<String> roomSink(UUID roomId) {
		return roomSinks.get(roomId);
	}

	public Mono<Room> read(UUID uuid) {
		return rooms
				.stream()
				.filter(room -> uuid.equals(room.uuid()))
				.findFirst()
				.map(Mono::just)
				.orElseThrow(RuntimeException::new);
	}
	public Flux<Room> readAll() {
		return Flux.fromIterable(rooms);
	}
	public Mono<Room> create(Room room) {
		return Mono
				.fromCallable(() -> {
					rooms.add(room);
					roomSinks.put(room.uuid(), Sinks.many().replay().latestOrDefault("no one yet in the room"));
					return room;
				});
	}

	public Mono<Boolean> notifyMemberDetailsToTheRoom(UUID roomId) {
		ObjectMapper objectMapper = new ObjectMapper();
		return read(roomId)
				.map(Room::members)
				.<String>handle((members, sink) -> {
					try {
						sink.next(objectMapper.writeValueAsString(members));
					} catch (JsonProcessingException e) {
						sink.error(new RuntimeException(e));
					}
				})
				.map(membersJSON -> roomSink(roomId).tryEmitNext(membersJSON))
				.map(Sinks.EmitResult::isSuccess);
	}
}

@Controller
class Controllers {

	@GetMapping("/")
	public Mono<String> index() {
		return Mono.just("index");
	}
}

@Component
@RequiredArgsConstructor
class RestControllers {
	final Map<UUID, Sinks.Many<String>> roomSinks;
	final RoomRepository roomRepository;
	final MemberRepository memberRepository;

	@Bean
	public RouterFunction routerFunction() {
		return RouterFunctions
				.route()
				.path("/room", builder -> builder
						.GET("", this::getAllRooms)
						.POST("", this::createRoom)
						.GET("/{id}", this::roomById)
						.PUT("/{id}", request -> ServerResponse.noContent().build())
						.DELETE("/{id}", request -> ServerResponse.noContent().build())
						.POST("/{id}/member", this::joinMember)
						.GET("/{id}/member", this::allMemberFromTheRoom)
						.PUT("/{id}/member/{memberId}", this::updateMember)
				)
				.build();
	}

	private Mono<ServerResponse> updateMember(ServerRequest request) {
		var roomId = UUID.fromString(request.pathVariable("id"));
		var memberId = request.pathVariable("memberId");
		return null;
	}

	private Mono<ServerResponse> roomById(ServerRequest request) {
		var roomId = UUID.fromString(request.pathVariable("id"));
		return roomRepository
				.read(roomId)
				.flatMap(room -> ServerResponse.ok().bodyValue(room));
	}

	private Mono<ServerResponse> allMemberFromTheRoom(ServerRequest request) {
		var roomId = UUID.fromString(request.pathVariable("id"));
		return memberRepository
				.allMemberFromRoom(roomId)
				.collectList()
				.flatMap(members -> ServerResponse.ok().bodyValue(members));
	}

	private Mono<ServerResponse> joinMember(ServerRequest request) {
		var roomId = UUID.fromString(request.pathVariable("id"));
		return request
				.bodyToMono(Member.class)
				.flatMap(member -> memberRepository.addMemberToTheRoom(roomId, member))
				.doOnNext(member -> roomRepository.notifyMemberDetailsToTheRoom(roomId).subscribe())
				.flatMap(member -> ServerResponse.ok().bodyValue(member));
	}

	private Mono<ServerResponse> createRoom(ServerRequest request) {
		return request
				.bodyToMono(Room.class)
				.map(room -> new Room(UUID.randomUUID(), room.cards(), new ArrayList<>()))
				.flatMap(roomRepository::create)
				.flatMap(room -> ServerResponse.ok().bodyValue(room));
	}

	private Mono<ServerResponse> getAllRooms(ServerRequest request) {
		return roomRepository
				.readAll()
				.collectList()
				.flatMap(rooms -> ServerResponse.ok().bodyValue(rooms));
	}

}

@Configuration
@RequiredArgsConstructor
class WebSocketConfiguration {
	final RoomWebSocketHandler roomWebSocketHandler;

	@Bean
	public HandlerMapping webSocketMapping() {
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setOrder(1);

		// Map WebSocket URLs to handlers
		mapping.setUrlMap(Collections.singletonMap("/room/{roomId}/ws", roomWebSocketHandler));

		return mapping;
	}

	@Bean
	public WebSocketHandlerAdapter handlerAdapter() {
		return new WebSocketHandlerAdapter();
	}
}

@Component
@RequiredArgsConstructor
@Slf4j
class RoomWebSocketHandler implements WebSocketHandler {
	final RoomRepository roomRepository;

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		var roomId = session.getHandshakeInfo().getUri().getPath().split("/")[2];
		log.info("RoomId: {}", roomId);
		var roomIdUUID = UUID.fromString(roomId);
		var sink = roomRepository.roomSink(roomIdUUID);

		if(isNull(sink)) {
			throw new RuntimeException("User is not connected");
		}

		return session.send(sink.asFlux().map(session::textMessage))
				.and(session.receive()
						.doOnNext(message -> {
							sink.tryEmitNext(message.getPayloadAsText());
						})
						.then());
	}
}