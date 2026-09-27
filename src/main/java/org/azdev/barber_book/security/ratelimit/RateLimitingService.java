package org.azdev.barber_book.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.capacity:5}")
    private int capacity;

    @Value("${app.rate-limit.refill-duration-minutes:60}")
    private long refillDurationMinutes;

    @Value("${app.rate-limit.login.capacity:5}")
    private int loginCapacity;

    @Value("${app.rate-limit.login.refill-duration-minutes:15}")
    private long loginRefillDurationMinutes;

    public Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent("appt:" + ip,
                k -> newBucket(capacity, Duration.ofMinutes(refillDurationMinutes)));
    }

    public Bucket resolveLoginBucket(String key) {
        return cache.computeIfAbsent("auth:" + key,
                k -> newBucket(loginCapacity, Duration.ofMinutes(loginRefillDurationMinutes)));
    }

    private Bucket newBucket(int capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, refillDuration)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
