package am.ik.ratelimit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootApplication
public class RateLimitingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RateLimitingServiceApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes() {
		return new RateLimitingHandler().route();
	}
}
