package org.azdev.barber_book.security.ratelimit;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingServiceTest {

    private final RateLimitingService rateLimitingService = new RateLimitingService();

    @Test
    void resolveBucketReturnsSameBucketForSameKey() {
        ReflectionTestUtils.setField(rateLimitingService, "capacity", 5);
        ReflectionTestUtils.setField(rateLimitingService, "refillDurationMinutes", 60L);

        Bucket first = rateLimitingService.resolveBucket("1.1.1.1");
        Bucket second = rateLimitingService.resolveBucket("1.1.1.1");

        assertThat(first).isSameAs(second);
    }

    @Test
    void resolveBucketReturnsDifferentBucketsForDifferentKeys() {
        ReflectionTestUtils.setField(rateLimitingService, "capacity", 5);
        ReflectionTestUtils.setField(rateLimitingService, "refillDurationMinutes", 60L);

        Bucket first = rateLimitingService.resolveBucket("1.1.1.1");
        Bucket second = rateLimitingService.resolveBucket("2.2.2.2");

        assertThat(first).isNotSameAs(second);
    }

    @Test
    void resolveBucketEnforcesCapacity() {
        ReflectionTestUtils.setField(rateLimitingService, "capacity", 1);
        ReflectionTestUtils.setField(rateLimitingService, "refillDurationMinutes", 60L);

        Bucket bucket = rateLimitingService.resolveBucket("3.3.3.3");

        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void resolveLoginBucketReturnsSameBucketForSameKey() {
        ReflectionTestUtils.setField(rateLimitingService, "loginCapacity", 5);
        ReflectionTestUtils.setField(rateLimitingService, "loginRefillDurationMinutes", 15L);

        Bucket first = rateLimitingService.resolveLoginBucket("user@test.com");
        Bucket second = rateLimitingService.resolveLoginBucket("user@test.com");

        assertThat(first).isSameAs(second);
    }

    @Test
    void resolveLoginBucketEnforcesCapacity() {
        ReflectionTestUtils.setField(rateLimitingService, "loginCapacity", 1);
        ReflectionTestUtils.setField(rateLimitingService, "loginRefillDurationMinutes", 15L);

        Bucket bucket = rateLimitingService.resolveLoginBucket("locked@test.com");

        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void appointmentAndLoginBucketsAreIndependentForSameKey() {
        ReflectionTestUtils.setField(rateLimitingService, "capacity", 5);
        ReflectionTestUtils.setField(rateLimitingService, "refillDurationMinutes", 60L);
        ReflectionTestUtils.setField(rateLimitingService, "loginCapacity", 5);
        ReflectionTestUtils.setField(rateLimitingService, "loginRefillDurationMinutes", 15L);

        Bucket appointmentBucket = rateLimitingService.resolveBucket("shared-key");
        Bucket loginBucket = rateLimitingService.resolveLoginBucket("shared-key");

        assertThat(appointmentBucket).isNotSameAs(loginBucket);
    }
}
