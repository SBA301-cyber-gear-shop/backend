package com.project.cybershop.pcbuild.internal.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RateLimiter - cửa sổ trượt 60 giây theo user/IP (DD 11.1)")
class RateLimiterTest {

    private final AiCircuitBreakerTest.MutableClock clock =
            new AiCircuitBreakerTest.MutableClock(Instant.parse("2026-10-30T08:00:00Z"));
    private final RateLimiter limiter = new RateLimiter(clock, 6, 30);

    private void acquireTimes(RateLimiter.Bucket bucket, String client, int times) {
        for (int index = 0; index < times; index++) {
            limiter.acquire(bucket, client);
        }
    }

    @Test
    @DisplayName("ai-suggest: 6 lần trong một phút được qua, lần thứ 7 → 429 RATE_LIMITED kèm Retry-After")
    void rejectsSeventhSuggestWithinOneMinute() {
        acquireTimes(RateLimiter.Bucket.AI_SUGGEST, "ip:10.0.0.1", 6);
        clock.advance(Duration.ofSeconds(20));

        assertThatThrownBy(() -> limiter.acquire(RateLimiter.Bucket.AI_SUGGEST, "ip:10.0.0.1"))
                .isInstanceOfSatisfying(BuildRuleException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("RATE_LIMITED");
                    assertThat(error.getHttpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    // Lượt cũ nhất hết hạn sau 60 − 20 = 40 giây
                    assertThat(error.getRetryAfterSeconds()).isEqualTo(40L);
                });
    }

    @Test
    @DisplayName("Lượt bị từ chối không được tính; hết 60 giây kể từ lượt cũ nhất thì gọi lại được")
    void slidingWindowFreesSlotsAfterOneMinute() {
        acquireTimes(RateLimiter.Bucket.AI_SUGGEST, "ip:10.0.0.1", 6);
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThatThrownBy(() -> limiter.acquire(RateLimiter.Bucket.AI_SUGGEST, "ip:10.0.0.1"))
                    .isInstanceOf(BuildRuleException.class);
        }

        clock.advance(Duration.ofSeconds(60));

        assertThatCode(() -> acquireTimes(RateLimiter.Bucket.AI_SUGGEST, "ip:10.0.0.1", 6)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Mỗi client và mỗi endpoint có bộ đếm riêng; validate/replacements dùng hạn mức 30")
    void bucketsAreIsolatedPerClientAndEndpoint() {
        acquireTimes(RateLimiter.Bucket.AI_SUGGEST, "ip:10.0.0.1", 6);

        assertThatCode(() -> limiter.acquire(RateLimiter.Bucket.AI_SUGGEST, "ip:10.0.0.2")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.acquire(RateLimiter.Bucket.AI_SUGGEST, "user:a@test.com")).doesNotThrowAnyException();
        assertThatCode(() -> acquireTimes(RateLimiter.Bucket.VALIDATE, "ip:10.0.0.1", 30)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.acquire(RateLimiter.Bucket.VALIDATE, "ip:10.0.0.1"))
                .isInstanceOf(BuildRuleException.class);
        assertThatCode(() -> limiter.acquire(RateLimiter.Bucket.REPLACEMENTS, "ip:10.0.0.1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Client im lặng quá một phút được dọn khỏi bộ nhớ")
    void idleClientsAreEvicted() {
        acquireTimes(RateLimiter.Bucket.VALIDATE, "ip:10.0.0.1", 3);
        acquireTimes(RateLimiter.Bucket.VALIDATE, "ip:10.0.0.2", 3);
        assertThat(limiter.trackedClients()).isEqualTo(2);

        clock.advance(Duration.ofSeconds(61));
        limiter.acquire(RateLimiter.Bucket.VALIDATE, "ip:10.0.0.3");

        assertThat(limiter.trackedClients()).isEqualTo(1);
    }

    @Test
    @DisplayName("20 luồng gọi đồng thời cùng một client: đúng 6 lượt được qua")
    void concurrentCallsNeverExceedLimit() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    limiter.acquire(RateLimiter.Bucket.AI_SUGGEST, "ip:10.0.0.9");
                    allowed.incrementAndGet();
                } catch (BuildRuleException expected) {
                    // bị chặn đúng như mong đợi
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(allowed.get()).isEqualTo(6);
    }
}
