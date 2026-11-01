package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.project.cybershop.pcbuild.internal.PcBuildTestData.part;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateRepositoryFacadeImpl - snapshot nạp tối đa 1 lần mỗi 60 giây, bất biến, chịu lỗi DB")
class CandidateRepositoryFacadeImplTest {

    @Mock
    private CatalogPublicApi catalogPublicApi;

    @Mock
    private CompatibilityRuleRepository ruleRepository;

    private MutableClock clock;
    private CandidateRepositoryFacadeImpl facade;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-30T08:00:00Z"));
        facade = new CandidateRepositoryFacadeImpl(catalogPublicApi, ruleRepository,
                new MetadataDictionaryServiceImpl(new com.fasterxml.jackson.databind.ObjectMapper()),
                new PartScorerImpl(), new ProfilePresetsImpl(), clock, 60);
    }

    private void stubCatalog() {
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(List.of(
                part(1, "cpu", "Ryzen 5", 5_000_000, Map.of(1, "AM5", 2, "65/88", 21, "AM4, am5")),
                part(2, "mainboard", "B650", 4_000_000, Map.of(1, "AM5")),
                ComponentCandidateDTO.builder().variantId(99).categoryId(20).categorySlug("monitor").build()));
        when(catalogPublicApi.getSpecificationCatalog()).thenReturn(List.of(
                CategorySpecDto.builder().categoryId(1).categorySlug("cpu").specificationId(1).specificationName("Socket")
                        .compatibility(true)
                        .options(List.of(new CategorySpecDto.Option("AM5", "AMD AM5"))).build(),
                CategorySpecDto.builder().categoryId(1).categorySlug("cpu").specificationId(22).specificationName("Core_Count")
                        .compatibility(false).build()));
        when(ruleRepository.findAll()).thenReturn(List.of(
                new CompatibilityRule(1, "CPU ↔ Bo mạch chủ (Socket)", 1, 1, 2, 1, "EQUALS")));
    }

    @Test
    @DisplayName("1.000 lần gọi trong 60 giây → mỗi nguồn dữ liệu chỉ được đọc đúng 1 lần")
    void cachesSnapshotForSixtySeconds() {
        stubCatalog();
        CompatibilitySnapshot first = facade.snapshot();
        for (int call = 0; call < 1000; call++) {
            clock.advance(Duration.ofMillis(50)); // tổng 50 giây
            assertThat(facade.snapshot()).isSameAs(first);
        }

        verify(catalogPublicApi, times(1)).getActiveCandidatesForBuild();
        verify(catalogPublicApi, times(1)).getSpecificationCatalog();
        verify(ruleRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Quá 60 giây → nạp lại đúng 1 lần")
    void reloadsAfterTtl() {
        stubCatalog();
        CompatibilitySnapshot first = facade.snapshot();
        clock.advance(Duration.ofSeconds(61));

        CompatibilitySnapshot second = facade.snapshot();
        facade.snapshot();

        assertThat(second).isNotSameAs(first);
        verify(ruleRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("10 luồng cùng gọi khi cache hết hạn → chỉ 1 lần nạp")
    void concurrentCallersShareOneReload() throws Exception {
        stubCatalog();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CompatibilitySnapshot>> results = new ArrayList<>();
        for (int thread = 0; thread < 10; thread++) {
            results.add(pool.submit(() -> {
                start.await();
                return facade.snapshot();
            }));
        }
        start.countDown();
        for (Future<CompatibilitySnapshot> result : results) {
            assertThat(result.get(5, TimeUnit.SECONDS)).isSameAs(results.get(0).get());
        }
        pool.shutdownNow();

        verify(catalogPublicApi, times(1)).getActiveCandidatesForBuild();
    }

    @Test
    @DisplayName("Nạp lại lỗi khi đã có bản cũ → trả bản cũ, không ném lỗi")
    void keepsOldSnapshotWhenReloadFails() {
        stubCatalog();
        CompatibilitySnapshot first = facade.snapshot();
        clock.advance(Duration.ofSeconds(61));
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenThrow(new IllegalStateException("DB down"));

        assertThat(facade.snapshot()).isSameAs(first);
    }

    @Test
    @DisplayName("Chưa có bản nào và nạp lỗi → BuildRuleException CATALOG_EMPTY (503)")
    void throwsCatalogEmptyWithoutSnapshot() {
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenThrow(new IllegalStateException("DB down"));

        assertThatThrownBy(() -> facade.snapshot())
                .isInstanceOf(BuildRuleException.class)
                .satisfies(error -> {
                    BuildRuleException exception = (BuildRuleException) error;
                    assertThat(exception.getCode()).isEqualTo("CATALOG_EMPTY");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(503);
                });
    }

    @Test
    @DisplayName("Snapshot chỉ chứa 8 vai trò, thông số bắt buộc và option đọc từ category_specifications")
    void buildsSnapshotFromCatalog() {
        stubCatalog();

        CompatibilitySnapshot snapshot = facade.snapshot();

        assertThat(snapshot.candidates()).extracting(ComponentCandidateDTO::getVariantId).containsExactly(1, 2);
        assertThat(snapshot.requiredSpecIds(1)).containsExactly(1);
        assertThat(snapshot.display(1, "AM5")).isEqualTo("AMD AM5");
        assertThat(snapshot.specName(22)).isEqualTo("Core_Count");
        assertThat(snapshot.rules()).hasSize(1);
        assertThat(snapshot.loadedAt()).isEqualTo(Instant.parse("2026-10-30T08:00:00Z"));
    }

    @Test
    @DisplayName("Collection trong snapshot bất biến; \"65/88\" không có key số và không ném lỗi")
    void snapshotIsImmutableAndParsesDirtyValues() {
        stubCatalog();
        CompatibilitySnapshot snapshot = facade.snapshot();

        assertThatThrownBy(() -> snapshot.candidates().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.rules().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.requiredSpecIdsByCategoryId().get(1).add(5))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.parsedSpecsByVariantId().get(1).ints().put(2, 65))
                .isInstanceOf(UnsupportedOperationException.class);

        var parsed = snapshot.parsedSpecsByVariantId().get(1);
        assertThat(parsed.ints()).doesNotContainKey(2);
        assertThat(parsed.raw()).containsEntry(2, "65/88");
        assertThat(parsed.lists().get(21)).containsExactlyInAnyOrder("AM4", "AM5");
    }

    /** Đồng hồ giả lập để kiểm thời gian cache mà không phải chờ thật. */
    private static final class MutableClock extends Clock {
        private volatile Instant now;

        private MutableClock(Instant start) {
            this.now = start;
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
