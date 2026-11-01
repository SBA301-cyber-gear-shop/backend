package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.model.BuildTier;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import com.project.cybershop.pcbuild.internal.service.ProfileExtractor;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProfileExtractor - cache 24 giờ và gộp lời gọi trùng khóa (DD 5.6)")
class ProfileExtractorTest {

    private final LlmClientService llm = mock(LlmClientService.class);
    private final ProfileExtractorImpl extractor = new ProfileExtractorImpl(llm, new ProfilePresetsImpl(), new ObjectMapper(),
            Clock.systemUTC(), 24, 5000);
    private final CompatibilitySnapshot snapshot = new CompatibilitySnapshot(List.of(), Map.of(), List.of(), Map.of(), Map.of(),
            Map.of(), MetadataDictionaryResponse.builder().dictionaryVersion("abc123").build(), null, Instant.now());
    private final BigDecimal budget = BigDecimal.valueOf(30_000_000);

    private static RequirementProfileDto profile(String summary) {
        return RequirementProfileDto.builder().summary(summary).archetype("GAMING").confidence(0.9).build();
    }

    private ProfileExtractor.Extraction extract(String purpose) {
        return extractor.extract(purpose, "Case trắng", budget, BuildTier.T3, snapshot, Instant.now().plusSeconds(14));
    }

    @Test
    @DisplayName("Lần đầu gọi AI (OK); lần 2 cùng câu khác hoa thường, dấu, khoảng trắng → CACHED, không gọi AI")
    void cachesByNormalizedText() {
        when(llm.extractProfile(anyString(), anyString(), any())).thenReturn(profile("Chơi game 2K"));

        ProfileExtractor.Extraction first = extract("Chơi game 2K");
        ProfileExtractor.Extraction second = extractor.extract("  CHOI   GAME 2k ", "case TRẮNG", budget, BuildTier.T3, snapshot,
                Instant.now().plusSeconds(14));

        assertThat(first.status()).isEqualTo("OK");
        assertThat(second.status()).isEqualTo("CACHED");
        assertThat(second.draft().getSummary()).isEqualTo("Chơi game 2K");
        verify(llm, times(1)).extractProfile(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Mỗi request nhận bản sao: sửa hồ sơ trả về không làm hỏng cache")
    void returnsCopies() {
        when(llm.extractProfile(anyString(), anyString(), any())).thenReturn(profile("Gốc"));
        extract("Gaming").draft().setSummary("đã sửa");

        assertThat(extract("Gaming").draft().getSummary()).isEqualTo("Gốc");
    }

    @Test
    @DisplayName("Nội dung gửi AI có từ điển, mức ngân sách và 6 hồ sơ mẫu ở đúng mức")
    void sendsDictionaryAndPresets() {
        when(llm.extractProfile(anyString(), anyString(), any())).thenAnswer(invocation -> {
            String userJson = invocation.getArgument(1);
            assertThat(userJson).contains("\"budgetTier\":\"T3\"").contains("\"dictionaryVersion\":\"abc123\"")
                    .contains("Hồ sơ mẫu GAMING T3").contains("Hồ sơ mẫu OFFICE T3");
            assertThat((String) invocation.getArgument(0)).contains("CHỈ dùng các role, spec, metric");
            return profile("x");
        });
        extract("Gaming");
    }

    @Test
    @DisplayName("2 luồng cùng khóa gọi đồng thời (AI chậm 1 s) → AI được gọi đúng 1 lần, luồng sau nhận CACHED")
    void singleFlightForSameKey() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        when(llm.extractProfile(anyString(), anyString(), any())).thenAnswer(invocation -> {
            calls.incrementAndGet();
            entered.countDown();
            Thread.sleep(1000);
            return profile("Dùng chung");
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CompletableFuture<ProfileExtractor.Extraction> first = CompletableFuture.supplyAsync(() -> extract("Gaming"), pool);
        entered.await(5, TimeUnit.SECONDS);
        CompletableFuture<ProfileExtractor.Extraction> second = CompletableFuture.supplyAsync(() -> extract("Gaming"), pool);

        assertThat(first.get(5, TimeUnit.SECONDS).status()).isEqualTo("OK");
        assertThat(second.get(5, TimeUnit.SECONDS).status()).isEqualTo("CACHED");
        assertThat(second.get().draft().getSummary()).isEqualTo("Dùng chung");
        assertThat(calls.get()).isEqualTo(1);
        pool.shutdownNow();
    }

    @Test
    @DisplayName("Lời gọi gốc lỗi → luồng chờ nhận cùng lỗi; khóa được gỡ nên lần sau gọi AI lại từ đầu")
    void failurePropagatesAndClearsInFlight() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        when(llm.extractProfile(anyString(), anyString(), any())).thenAnswer(invocation -> {
            entered.countDown();
            Thread.sleep(500);
            throw new AiUnavailableException(AiUnavailableException.Detail.TIMEOUT);
        }).thenReturn(profile("Lần sau"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CompletableFuture<ProfileExtractor.Extraction> first = CompletableFuture.supplyAsync(() -> extract("Gaming"), pool);
        entered.await(5, TimeUnit.SECONDS);
        CompletableFuture<ProfileExtractor.Extraction> second = CompletableFuture.supplyAsync(() -> extract("Gaming"), pool);

        for (CompletableFuture<ProfileExtractor.Extraction> future : List.of(first, second)) {
            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .cause().isInstanceOfSatisfying(AiUnavailableException.class,
                            error -> assertThat(error.detail()).isEqualTo(AiUnavailableException.Detail.TIMEOUT));
        }
        assertThat(extract("Gaming").status()).isEqualTo("OK");
        pool.shutdownNow();
    }

    @Test
    @DisplayName("Luồng chờ hết deadline riêng → DEADLINE_EXCEEDED; lời gọi gốc vẫn hoàn tất và ghi cache")
    void waiterHonoursOwnDeadline() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        when(llm.extractProfile(anyString(), anyString(), any())).thenAnswer(invocation -> {
            entered.countDown();
            Thread.sleep(800);
            return profile("Xong");
        });
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CompletableFuture<ProfileExtractor.Extraction> owner = CompletableFuture.supplyAsync(() -> extract("Gaming"), pool);
        entered.await(5, TimeUnit.SECONDS);

        assertThatThrownBy(() -> extractor.extract("Gaming", "Case trắng", budget, BuildTier.T3, snapshot, Instant.now().plusMillis(100)))
                .isInstanceOfSatisfying(AiUnavailableException.class,
                        error -> assertThat(error.detail()).isEqualTo(AiUnavailableException.Detail.DEADLINE_EXCEEDED));
        assertThat(owner.get(5, TimeUnit.SECONDS).status()).isEqualTo("OK");
        assertThat(extract("Gaming").status()).isEqualTo("CACHED");
        pool.shutdownNow();
    }

    @Test
    @DisplayName("Khóa khác nhau (khác mức ngân sách) → mỗi khóa một lần gọi AI")
    void differentKeysCallSeparately() {
        when(llm.extractProfile(anyString(), anyString(), any())).thenReturn(profile("x"));
        extract("Gaming");
        extractor.extract("Gaming", "Case trắng", BigDecimal.valueOf(60_000_000), BuildTier.T4, snapshot, Instant.now().plusSeconds(14));

        verify(llm, times(2)).extractProfile(anyString(), anyString(), any());
        verify(llm, never()).explainBuild(anyString(), anyString(), any());
    }
}
