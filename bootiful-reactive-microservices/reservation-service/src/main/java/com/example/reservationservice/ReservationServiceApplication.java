package com.example.reservationservice;

import brave.sampler.Sampler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@EnableBinding(Sink.class)
@SpringBootApplication
@Slf4j
public class ReservationServiceApplication {

	public ReservationServiceApplication(ReservationRepository reservationRepository) {
		this.reservationRepository = reservationRepository;
	}

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}

/*	@Bean
	IntegrationFlow flow (Sink sink,
	                      ReservationRepository rr ) {
		return IntegrationFlows
				.from(sink.input())
				.handle((GenericHandler<String>) (payload, headers) -> {
					rr.save(new Reservation(null, payload)).subscribe(System.out::println );
					return null;
				})
				.get();
	}
	*/

	/*
	@StreamListener(Sink.INPUT)
	public void accept(String name) {
		// write to DB
	}
	*/


	private final ReservationRepository reservationRepository;

	@StreamListener
	public void accept(@Input(Sink.INPUT) Flux<String> names) {
		names
				.map(name -> new Reservation(null, name))
				.flatMap(this.reservationRepository::save)
				.subscribe(r -> System.out.println("new message saved! '" + r.getReservationName() + "'"));
	}

	@EventListener(RefreshScopeRefreshedEvent.class)
	public void refresh(RefreshScopeRefreshedEvent event) {
		log.info("refresh event noticed!");
	}

	@Bean
	RouterFunction<ServerResponse> routes(
			ReservationRepository rr,
			Environment environment
			/*MeterRegistry mr*/) {

		return route(GET("/reservations"), req -> ok().body(rr.findAll(), Reservation.class));
		//.andRoute(GET("/message"), req -> ok().body(Flux.just(environment.getProperty("message")), String.class));

				/*.andRoute(GET("/my-counter"), r -> {
					mr.counter("my-simple-counter").increment();
					return ServerResponse.ok().body(Flux.just("hello, world"), String.class);
				});*/
	}

}


@RestController
@RefreshScope
class MessageRestController {

	private final String msg;

	MessageRestController(@Value("${message}") String msg) {
		this.msg = msg;
	}

	@GetMapping("/message")
	String msg() {
		return this.msg;
	}
}


interface ReservationRepository extends ReactiveMongoRepository<Reservation, String> {

	Flux<Reservation> findByReservationName(String rn);
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
class Reservation {

	@Id
	private String id;
	private String reservationName;
}

@Component
class Initializer implements ApplicationRunner {

	private final ReservationRepository reservationRepository;

	Initializer(ReservationRepository reservationRepository) {
		this.reservationRepository = reservationRepository;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {

		this.reservationRepository
				.deleteAll()
				.thenMany(Flux.just("Josh", "Krishna", "Naresha", "Bhavin",
						"Naresh", "Jimmy", "Srini", "Hari")
						.map(x -> new Reservation(null, x))
						.flatMap(this.reservationRepository::save))
				.thenMany(this.reservationRepository.findAll())
				.subscribe(System.out::println);
	}
}