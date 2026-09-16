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

    public Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofMinutes(refillDurationMinutes))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
