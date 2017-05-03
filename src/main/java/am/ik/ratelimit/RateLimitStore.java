package am.ik.ratelimit;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;

public class RateLimitStore {
	private static final String RATE_LIMITER_QPS = "RATE_LIMITER_QPS";
	private static final Logger log = LoggerFactory.getLogger(RateLimitStore.class);
	private final Cache<String, RateLimiter> cache = CacheBuilder.newBuilder()
			.maximumSize(1024).expireAfterAccess(3, TimeUnit.MINUTES)
			.removalListener(
					notification -> log.info("Removed {}", notification.getCause()))
			.build();
	private final double qps;

	public RateLimitStore() {
		this.qps = Optional.ofNullable(System.getenv(RATE_LIMITER_QPS))
				.map(Double::valueOf).orElse(3.0);

	}

	public boolean isLimited(String remoteAddr) {
		try {
			RateLimiter rateLimiter = cache.get(remoteAddr,
					() -> RateLimiter.create(qps));
			boolean isLimited = !rateLimiter.tryAcquire();
			if (isLimited) {
				log.info("RateLimited: {}", remoteAddr);
			}
			return isLimited;
		}
		catch (ExecutionException e) {
			log.error("cannot create RateLimiter", e);
			return true;
		}
	}
}
