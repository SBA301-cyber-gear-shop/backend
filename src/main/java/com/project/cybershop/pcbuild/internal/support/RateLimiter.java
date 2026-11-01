package com.project.cybershop.pcbuild.internal.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn tần suất các endpoint PC Build (DD 11.1): cửa sổ trượt 60 giây, mỗi endpoint một bộ đếm cho mỗi client.
 * Client là user đã đăng nhập, hoặc IP kết nối khi chưa đăng nhập (không đọc X-Forwarded-For, U13).
 * Mục đích là bảo vệ hạn mức gói miễn phí của Gemini; vượt hạn mức trả 429 RATE_LIMITED kèm Retry-After.
 * shortcut: bộ đếm nằm trong bộ nhớ một instance (K8); chạy nhiều instance thì chuyển sang Redis/Bucket4j.
 */
@Component
public class RateLimiter {

    public enum Bucket { AI_SUGGEST, VALIDATE, REPLACEMENTS, OPTIONS }

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Clock clock;
    private final int aiSuggestPerMinute;
    private final int checkPerMinute;
    private final Map<String, Deque<Instant>> hitsByClient = new ConcurrentHashMap<>();
    private volatile Instant nextEviction = Instant.MIN;

    public RateLimiter(Clock clock,
                       @Value("${app.pcbuild.rate-limit-per-minute:6}") int aiSuggestPerMinute,
                       @Value("${app.pcbuild.rate-limit-check-per-minute:30}") int checkPerMinute) {
        this.clock = clock;
        this.aiSuggestPerMinute = aiSuggestPerMinute;
        this.checkPerMinute = checkPerMinute;
    }

    /** Ghi một lượt gọi; đã đủ hạn mức trong 60 giây gần nhất thì ném 429 và không tính lượt này. */
    public void acquire(Bucket bucket, String clientKey) {
        Instant now = clock.instant();
        evictIdleClients(now);
        Instant windowStart = now.minus(WINDOW);
        Deque<Instant> hits = hitsByClient.computeIfAbsent(bucket + "|" + clientKey, key -> new ArrayDeque<>());
        long retryAfterSeconds;
        synchronized (hits) {
            while (!hits.isEmpty() && !hits.peekFirst().isAfter(windowStart)) {
                hits.pollFirst();
            }
            if (hits.size() < limitOf(bucket)) {
                hits.addLast(now);
                return;
            }
            retryAfterSeconds = secondsUntil(hits.peekFirst().plus(WINDOW), now);
        }
        throw new BuildRuleException("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                "Bạn thao tác quá nhanh. Vui lòng thử lại sau " + retryAfterSeconds + " giây.",
                Map.of("retryAfterSeconds", retryAfterSeconds), retryAfterSeconds);
    }

    /** Số bộ đếm đang giữ trong bộ nhớ (dùng cho test). */
    int trackedClients() {
        return hitsByClient.size();
    }

    private int limitOf(Bucket bucket) {
        return bucket == Bucket.AI_SUGGEST ? aiSuggestPerMinute : checkPerMinute;
    }

    /**
     * Mỗi phút dọn một lần các client không gọi trong 60 giây qua, để map không phình theo số IP.
     * shortcut: lượt gọi đang chen giữa lúc dọn có thể không được đếm (lệch tối đa 1 lượt), chấp nhận cho giới hạn mềm.
     */
    private void evictIdleClients(Instant now) {
        if (now.isBefore(nextEviction)) {
            return;
        }
        synchronized (this) {
            if (now.isBefore(nextEviction)) {
                return;
            }
            nextEviction = now.plus(WINDOW);
        }
        Instant windowStart = now.minus(WINDOW);
        hitsByClient.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Instant lastHit = entry.getValue().peekLast();
                return lastHit == null || !lastHit.isAfter(windowStart);
            }
        });
    }

    private static long secondsUntil(Instant moment, Instant now) {
        long millis = Duration.between(now, moment).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }
}
