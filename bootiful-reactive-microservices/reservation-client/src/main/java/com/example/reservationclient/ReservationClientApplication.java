package com.example.reservationclient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.hystrix.HystrixCommands;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
@EnableCircuitBreaker
@EnableBinding(Source.class)
public class ReservationClientApplication {

	@Bean
	WebClient webClient(LoadBalancerExchangeFilterFunction lb) {
		return WebClient
				.builder()
				.filter(lb)
				.build();
	}

	@Bean
	RouterFunction<ServerResponse> routes(WebClient client,
	                                      Source source) {
		return route(GET("/reservations/names"), serverRequest -> {

			Publisher<String> stringFlux = client
					.get()
					.uri("http://reservation-service/reservations")
					.retrieve()
					.bodyToFlux(Reservation.class)
					.map(Reservation::getReservationName);

			Publisher<String> fallback = HystrixCommands
					.from(stringFlux)
					.commandName("reservation-names")
					.eager()
					.fallback(Flux.just("EEEK!"))
					.build();

			return ServerResponse.ok().body(fallback, String.class);
		})
		.andRoute(POST("/reservations"), req -> {
			Flux<Boolean> booleanFlux = req
					.bodyToFlux(Reservation.class)
					.map(Reservation::getReservationName)
					.map(r -> MessageBuilder.withPayload(r).build())
					.map(r -> source.output().send(r));
			return ServerResponse.ok().body(booleanFlux, Boolean.class);
		});
	}

	@Bean
	MapReactiveUserDetailsService authentication() {
		return new MapReactiveUserDetailsService(
				User.withDefaultPasswordEncoder()
						.username("jlong")
						.password("pw")
						.roles("USER")
						.build(),
				User.withDefaultPasswordEncoder()
						.username("rwinch")
						.password("pw")
						.roles("USER", "ADMIN")
						.build()
		);
	}

	@Bean
	SecurityWebFilterChain authorization(ServerHttpSecurity http) {
		//@formatter:off
		return
				http
				.httpBasic()
					.and()
				.csrf()
					.disable()
				.authorizeExchange()
					.pathMatchers("/rl").authenticated()
//					.pathMatchers("/foo").access((authenticationMono,authorizationContext)-> Mono.just( new AuthorizationDecision (true)))
					.anyExchange().permitAll()
				.and()
				.build();
		//@formatter:on
	}


	@Bean
	RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(4, 2);
	}

	@Bean
	RouteLocator routeLocator(RouteLocatorBuilder rlb,
	                          RedisRateLimiter rl) {
		return rlb
				.routes()
				.route(spec -> spec
						.path("/proxy")
						.filters(fs -> fs
								.setPath("/reservations")
						)
						.uri("lb://reservation-service/")
				)
				.route(spec -> spec
						.path("/rl")
						.filters(fs -> fs
								.setPath("/reservations")
								.requestRateLimiter(c -> c.setRateLimiter(rl))
						)
						.uri("lb://reservation-service/")
				)
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(ReservationClientApplication.class, args);
	}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {

	private String id;
	private String reservationName;
}
