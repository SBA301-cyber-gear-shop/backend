package com.project.cybershop.pcbuild.internal.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AiCircuitBreaker - CLOSED / OPEN / HALF_OPEN (DD 8.5.1)")
class AiCircuitBreakerTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-30T08:00:00Z"));
    private final AiCircuitBreaker breaker = new AiCircuitBreaker(clock, 5, 30);

    private void fail(int times) {
        for (int index = 0; index < times; index++) {
            breaker.beforeCall();
            breaker.onProviderFailure("TIMEOUT");
        }
    }

    @Test
    @DisplayName("4 lỗi liên tiếp vẫn CLOSED, lỗi thứ 5 → OPEN; một lần thành công giữa chừng đặt đếm về 0")
    void opensAfterThresholdOfConsecutiveFailures() {
        fail(4);
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.CLOSED);
        breaker.beforeCall();
        breaker.onSuccess();
        fail(4);
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.CLOSED);
        fail(1);
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("OPEN từ chối mọi lần gọi và trả số giây còn lại đúng")
    void openRejectsWithRemainingSeconds() {
        fail(5);
        clock.advance(Duration.ofSeconds(12));

        assertThatThrownBy(breaker::beforeCall).isInstanceOfSatisfying(AiUnavailableException.class, error -> {
            assertThat(error.detail()).isEqualTo(AiUnavailableException.Detail.CIRCUIT_OPEN);
            assertThat(error.getRetryAfterSeconds()).isEqualTo(18);
        });
    }

    @Test
    @DisplayName("Sau 30 s → HALF_OPEN cho đúng 1 lần thử; thử thành công → CLOSED, thử lỗi → OPEN thêm 30 s")
    void halfOpenAllowsSingleTrial() {
        fail(5);
        clock.advance(Duration.ofSeconds(30));

        assertThatCode(breaker::beforeCall).doesNotThrowAnyException();
        assertThatThrownBy(breaker::beforeCall).isInstanceOf(AiUnavailableException.class);
        breaker.onProviderFailure("HTTP 503");
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.OPEN);

        clock.advance(Duration.ofSeconds(30));
        breaker.beforeCall();
        breaker.onSuccess();
        assertThat(breaker.state()).isEqualTo(AiCircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Kết quả trung tính (JSON sai, HTTP 400, hết deadline) không tính lỗi nhưng nhả lượt thử HALF_OPEN")
    void neutralOutcomeReleasesTrial() {
        fail(5);
        clock.advance(Duration.ofSeconds(31));
        breaker.beforeCall();
        breaker.onNeutralOutcome();

        assertThatCode(breaker::beforeCall).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("20 luồng ghi lỗi đồng thời không làm sai bộ đếm (mạch mở đúng 1 lần)")
    void threadSafe() throws Exception {
        AiCircuitBreaker shared = new AiCircuitBreaker(clock, 20, 30);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int thread = 0; thread < 20; thread++) {
            futures.add(pool.submit(() -> {
                start.await();
                shared.beforeCall();
                shared.onProviderFailure("TIMEOUT");
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) future.get(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(shared.state()).isEqualTo(AiCircuitBreaker.State.OPEN);
    }

    static final class MutableClock extends Clock {
        private volatile Instant now;

        MutableClock(Instant start) {
            now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
