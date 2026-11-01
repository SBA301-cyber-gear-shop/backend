package com.project.cybershop.pcbuild.internal.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Ngắt mạch khi Gemini lỗi liên tiếp (DD 8.5.1). Một instance dùng chung cho cả trích hồ sơ và giải thích.
 * CLOSED → (đủ ngưỡng lỗi liên tiếp) → OPEN → (hết thời gian ngắt) → HALF_OPEN cho đúng một lần thử.
 * Chỉ tính lỗi nhà cung cấp: timeout, lỗi kết nối, HTTP 5xx, HTTP 429.
 * Shortcut: trạng thái nằm trong bộ nhớ một instance; chạy nhiều instance thì mỗi instance có mạch riêng (K8).
 */
@Slf4j
@Component
public class AiCircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final Clock clock;
    private final int failureThreshold;
    private final Duration openDuration;

    private State state = State.CLOSED;
    private int consecutiveFailures;
    private Instant openedAt;
    private boolean trialInProgress;

    public AiCircuitBreaker(Clock clock,
                            @Value("${app.ai.circuit-failure-threshold:5}") int failureThreshold,
                            @Value("${app.ai.circuit-open-seconds:30}") long openSeconds) {
        this.clock = clock;
        this.failureThreshold = failureThreshold;
        this.openDuration = Duration.ofSeconds(openSeconds);
    }

    /** Gọi trước mỗi lần gọi nhà cung cấp; mạch đang ngắt thì ném CIRCUIT_OPEN kèm số giây còn lại. */
    public synchronized void beforeCall() {
        if (state == State.OPEN) {
            Duration elapsed = Duration.between(openedAt, clock.instant());
            if (elapsed.compareTo(openDuration) < 0) {
                throw new AiUnavailableException(AiUnavailableException.Detail.CIRCUIT_OPEN, remainingSeconds());
            }
            transition(State.HALF_OPEN, "hết thời gian ngắt");
        }
        if (state == State.HALF_OPEN) {
            if (trialInProgress) {
                throw new AiUnavailableException(AiUnavailableException.Detail.CIRCUIT_OPEN, Math.max(1, openDuration.toSeconds()));
            }
            trialInProgress = true;
        }
    }

    public synchronized void onSuccess() {
        consecutiveFailures = 0;
        trialInProgress = false;
        if (state != State.CLOSED) {
            transition(State.CLOSED, "lần gọi thành công");
        }
    }

    /** Lỗi nhà cung cấp (timeout, kết nối, 5xx, 429). */
    public synchronized void onProviderFailure(String failureType) {
        consecutiveFailures++;
        if (state == State.HALF_OPEN) {
            trialInProgress = false;
            open("lần thử lại lỗi: " + failureType);
        } else if (state == State.CLOSED && consecutiveFailures >= failureThreshold) {
            open(consecutiveFailures + " lỗi liên tiếp, lỗi cuối: " + failureType);
        }
    }

    /** Lời gọi kết thúc mà không phải lỗi nhà cung cấp (JSON sai, HTTP 400, hết deadline): nhả lượt thử nếu đang giữ. */
    public synchronized void onNeutralOutcome() {
        trialInProgress = false;
    }

    public synchronized State state() {
        if (state == State.OPEN && Duration.between(openedAt, clock.instant()).compareTo(openDuration) >= 0) {
            return State.HALF_OPEN;
        }
        return state;
    }

    private void open(String reason) {
        openedAt = clock.instant();
        transition(State.OPEN, reason);
    }

    private long remainingSeconds() {
        Duration remaining = openDuration.minus(Duration.between(openedAt, clock.instant()));
        return Math.max(1, (remaining.toMillis() + 999) / 1000);
    }

    private void transition(State next, String reason) {
        log.warn("AiCircuitBreaker {} → {} ({}, số lỗi liên tiếp {})", state, next, reason, consecutiveFailures);
        state = next;
    }
}
