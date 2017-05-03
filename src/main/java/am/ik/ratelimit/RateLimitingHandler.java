package am.ik.ratelimit;

import java.net.URI;
import java.util.Collections;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.*;

import reactor.core.publisher.Mono;

public class RateLimitingHandler {
	private static final String FORWARDED_URL = "X-CF-Forwarded-Url";
	private static final String PROXY_METADATA = "X-CF-Proxy-Metadata";
	private static final String PROXY_SIGNATURE = "X-CF-Proxy-Signature";
	private final WebClient webClient = WebClient.create();
	private final RateLimitStore store = new RateLimitStore();

	public RouterFunction<ServerResponse> route() {
		return RouterFunctions.route(incoming(), this::rateLimit);
	}

	private RequestPredicate incoming() {
		return req -> {
			final HttpHeaders h = req.headers().asHttpHeaders();
			return h.containsKey(FORWARDED_URL) && h.containsKey(PROXY_METADATA)
					&& h.containsKey(PROXY_SIGNATURE);
		};
	}

	private Mono<ServerResponse> rateLimit(ServerRequest req) {
		String remoteAddr = req.headers()
				.header(com.google.common.net.HttpHeaders.X_FORWARDED_FOR).get(0);

		if (store.isLimited(remoteAddr)) {
			return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
					.contentType(MediaType.TEXT_PLAIN).syncBody("Too Many Requests :-(");
		}

		final HttpHeaders headers = headers(req.headers().asHttpHeaders());
		final URI uri = retrieveUri(headers);
		final WebClient.RequestHeadersSpec<?> spec = webClient.method(req.method())
				.uri(uri).headers(headers);

		return req
				.bodyToMono(String.class).<WebClient.RequestHeadersSpec<?>>map(
						((WebClient.RequestBodySpec) spec)::syncBody)
				.switchIfEmpty(Mono.just(spec))
				.flatMap(s -> s.exchange()
						.flatMap(res -> ServerResponse.status(res.statusCode())
								.headers(res.headers().asHttpHeaders()).body(
										res.bodyToMono(String.class), String.class)));
	}

	private URI retrieveUri(HttpHeaders headers) {
		return headers.remove(FORWARDED_URL).stream().findFirst().map(URI::create)
				.orElseThrow(() -> new IllegalStateException(
						String.format("No %s header present", FORWARDED_URL)));
	}

	private HttpHeaders headers(HttpHeaders incomingHeaders) {
		final HttpHeaders headers = new HttpHeaders();
		headers.putAll(incomingHeaders);
		final String host = URI.create(incomingHeaders.getFirst(FORWARDED_URL)).getHost();
		headers.put(HttpHeaders.HOST, Collections.singletonList(host));
		return headers;
	}
}
