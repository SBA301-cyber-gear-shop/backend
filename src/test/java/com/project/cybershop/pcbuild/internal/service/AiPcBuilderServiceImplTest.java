package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.BuildTier;
import com.project.cybershop.pcbuild.internal.model.CandidatePool;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.EngineReport;
import com.project.cybershop.pcbuild.internal.model.SolverResult;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.BuildExplainer;
import com.project.cybershop.pcbuild.internal.service.BuildSolver;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.service.ProfileExtractor;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import com.project.cybershop.pcbuild.internal.service.ProfileValidator;
import com.project.cybershop.pcbuild.internal.service.RequestValidator;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static com.project.cybershop.pcbuild.internal.PcBuildTestData.lines;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.snapshot;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.validDesktop;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiPcBuilderServiceImpl - orchestrator /ai-suggest (DD 3.3)")
class AiPcBuilderServiceImplTest {

    @Mock private CandidateRepositoryFacade facade;
    @Mock private RequestValidator requestValidator;
    @Mock private ProfileExtractor extractor;
    @Mock private ProfilePresets presets;
    @Mock private ProfileValidator validator;
    @Mock private BuildSolver solver;
    @Mock private BuildExplainer explainer;
    @Mock private CompatibilityEngine engine;

    private AiPcBuilderServiceImpl service;
    private final CompatibilitySnapshot snapshot = snapshot();
    private final BigDecimal budget = BigDecimal.valueOf(30_000_000);

    @BeforeEach
    void setUp() {
        service = new AiPcBuilderServiceImpl(facade, requestValidator, extractor, presets, validator, solver, explainer, engine,
                Clock.systemUTC(), 14_000, 0.4, "gemini", "gemini-3.5-flash-lite");
        when(facade.snapshot()).thenReturn(snapshot);
        when(facade.buildPool(any(), any(), any())).thenReturn(new CandidatePool(Map.of(), Map.of()));
        when(presets.fillMissing(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("options: trả tổng 3 món rẻ nhất, đắt nhất; từ điển không đổi vì nằm ngoài dictionary")
    void optionsExposePeripheralRange() {
        when(facade.snapshot()).thenReturn(new CompatibilitySnapshot(List.of(), Map.of(), List.of(), Map.of(), Map.of(), Map.of(),
                null, null, java.time.Instant.parse("2026-11-02T00:00:00Z"), List.of(
                com.project.cybershop.catalog.api.dto.ComponentCandidateDTO.builder().variantId(1).categorySlug("monitor").price(BigDecimal.valueOf(2_000_000)).build(),
                com.project.cybershop.catalog.api.dto.ComponentCandidateDTO.builder().variantId(2).categorySlug("monitor").price(BigDecimal.valueOf(9_000_000)).build(),
                com.project.cybershop.catalog.api.dto.ComponentCandidateDTO.builder().variantId(3).categorySlug("keyboard").price(BigDecimal.valueOf(300_000)).build(),
                com.project.cybershop.catalog.api.dto.ComponentCandidateDTO.builder().variantId(4).categorySlug("mouse").price(BigDecimal.valueOf(100_000)).build())));

        var options = service.options();

        org.assertj.core.api.Assertions.assertThat(options.getPeripheralMinTotal()).isEqualByComparingTo("2400000");
        org.assertj.core.api.Assertions.assertThat(options.getPeripheralMaxTotal()).isEqualByComparingTo("9400000");
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("options: kho thiếu một loại phụ kiện -> hai mức null để FE ẩn lựa chọn Trọn bộ")
    void optionsHidePeripheralRangeWhenCatalogIncomplete() {
        var options = service.options();

        org.assertj.core.api.Assertions.assertThat(options.getPeripheralMinTotal()).isNull();
        org.assertj.core.api.Assertions.assertThat(options.getPeripheralMaxTotal()).isNull();
    }

    private ValidatedProfile validated(RequirementProfileDto dto) {
        return new ValidatedProfile(dto, Archetype.GAMING, List.of());
    }

    private void stubHappyPath(RequirementProfileDto dto) {
        when(validator.validate(any(), any(), anyString(), any())).thenReturn(validated(dto));
        EngineReport report = EngineReport.of(List.of(), 650, null, BigDecimal.valueOf(27_000_000));
        when(solver.solve(any())).thenReturn(new SolverResult(lines(validDesktop()), BigDecimal.valueOf(27_000_000),
                BigDecimal.valueOf(3_000_000), 0.7, report, null, List.of(), BigDecimal.valueOf(20_000_000), 4, List.of(), validated(dto)));
        when(explainer.explain(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BuildExplainer.Explanation("Máy game", "Cân bằng.", "Chơi tốt 2K.", Map.of(1, "CPU"), "OK"));
        when(engine.evaluate(any(), any())).thenReturn(report);
    }

    private static RequirementProfileDto profile(double confidence) {
        return RequirementProfileDto.builder().summary("Chơi game").archetype("GAMING").confidence(confidence)
                .preferences(new java.util.ArrayList<>(List.of(com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto.builder()
                        .role("gpu").spec("Perf_Index_1440p").direction("MAX").weight(1.0).build())))
                .build();
    }

    @Test
    @DisplayName("Luồng đầy đủ: AI trích hồ sơ → solver → giải thích; response có budgetFit, metadata, lý do từng món")
    void fullFlow() {
        RequirementProfileDto dto = profile(0.9);
        when(extractor.extract(anyString(), any(), any(), eq(BuildTier.T3), any(), any()))
                .thenReturn(new ProfileExtractor.Extraction(dto, "OK"));
        stubHappyPath(dto);

        var response = service.processAiBuild(AiBuildRequest.builder().budget(budget).purpose("Chơi game 2K").build(), 7);

        assertThat(response.isCompatible()).isTrue();
        assertThat(response.getTotalPrice()).isEqualByComparingTo("27000000");
        assertThat(response.getBudgetFit().getStatus()).isEqualTo("IN_WINDOW");
        assertThat(response.getBudgetFit().getLeftover()).isEqualByComparingTo("3000000");
        assertThat(response.getMetadata().getExtractStatus()).isEqualTo("OK");
        assertThat(response.getMetadata().getPromptVersions()).containsEntry("profile", "PROFILE_V2");
        assertThat(response.getItems()).filteredOn(item -> item.getVariantId() == 1).singleElement()
                .satisfies(item -> assertThat(item.getAiReason()).isEqualTo("CPU"));
        assertThat(response.getItems()).filteredOn(item -> item.getVariantId() == 2).singleElement()
                .satisfies(item -> assertThat(item.getAiReason()).isEqualTo("—"));
        assertThat(response.getFloorPrice()).isEqualByComparingTo("20000000");
        assertThat(response.getWarnings()).doesNotContain("PSU_HIGH_WATTAGE");
    }

    @Test
    @DisplayName("DD 6.5: engine báo J4b (nguồn cần > 1600 W) → warnings có mã PSU_HIGH_WATTAGE cho FE")
    void highWattageBuildAddsWarningCode() {
        RequirementProfileDto dto = profile(0.9);
        when(extractor.extract(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new ProfileExtractor.Extraction(dto, "OK"));
        stubHappyPath(dto);
        EngineReport heavy = EngineReport.of(List.of(com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse
                .of("J4b", "Nguồn công suất rất lớn", "warning", "Hệ thống cần nguồn trên 1600 W")), 1750, null,
                BigDecimal.valueOf(27_000_000));
        when(engine.evaluate(any(), any())).thenReturn(heavy);

        var response = service.processAiBuild(AiBuildRequest.builder().budget(budget).purpose("Train AI 4 GPU").build(), 7);

        assertThat(response.getWarnings()).containsOnlyOnce("PSU_HIGH_WATTAGE");
        assertThat(response.getRequiredPsuW()).isEqualTo(1750);
    }

    @Test
    @DisplayName("Khách gửi profile → không gọi extractProfile, extractStatus = USER_PROFILE, source = USER_EDITED")
    void userProfileSkipsExtraction() {
        RequirementProfileDto dto = profile(0.1);
        stubHappyPath(dto);

        var response = service.processAiBuild(AiBuildRequest.builder().budget(budget).profile(dto).build(), null);

        verify(extractor, never()).extract(any(), any(), any(), any(), any(), any());
        verify(validator).validate(any(), any(), eq(ProfileValidator.SOURCE_USER_EDITED), any());
        assertThat(response.getMetadata().getExtractStatus()).isEqualTo("USER_PROFILE");
    }

    @Test
    @DisplayName("AI trích hồ sơ lỗi → AiUnavailableException, không gọi solver")
    void extractionFailureStopsBeforeSolver() {
        when(extractor.extract(any(), any(), any(), any(), any(), any()))
                .thenThrow(new AiUnavailableException(AiUnavailableException.Detail.TIMEOUT));

        assertThatThrownBy(() -> service.processAiBuild(AiBuildRequest.builder().budget(budget).purpose("Game").build(), null))
                .isInstanceOf(AiUnavailableException.class);
        verify(solver, never()).solve(any());
    }

    @Test
    @DisplayName("confidence 0,2 + câu hỏi → 422 NEEDS_UNCLEAR có clarifyingQuestion, không gọi solver")
    void unclearNeedsAskBack() {
        RequirementProfileDto dto = profile(0.2);
        dto.setClarifyingQuestion("Bạn dùng máy để làm gì?");
        when(extractor.extract(any(), any(), any(), any(), any(), any())).thenReturn(new ProfileExtractor.Extraction(dto, "OK"));
        when(validator.validate(any(), any(), anyString(), any())).thenReturn(validated(dto));

        assertThatThrownBy(() -> service.processAiBuild(AiBuildRequest.builder().budget(budget).purpose("Máy tốt").build(), null))
                .isInstanceOfSatisfying(BuildRuleException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("NEEDS_UNCLEAR");
                    assertThat(error.getData()).isEqualTo(Map.of("clarifyingQuestion", "Bạn dùng máy để làm gì?"));
                });
        verify(solver, never()).solve(any());
    }

    @Test
    @DisplayName("AI giải thích lỗi → ném lỗi, không trả bộ máy chưa có lời giải thích")
    void explanationFailureReturnsNoBuild() {
        RequirementProfileDto dto = profile(0.9);
        when(extractor.extract(any(), any(), any(), any(), any(), any())).thenReturn(new ProfileExtractor.Extraction(dto, "CACHED"));
        stubHappyPath(dto);
        when(explainer.explain(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new AiUnavailableException(AiUnavailableException.Detail.DEADLINE_EXCEEDED));

        assertThatThrownBy(() -> service.processAiBuild(AiBuildRequest.builder().budget(budget).purpose("Game").build(), null))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    @DisplayName("Mỗi request lấy snapshot đúng 1 lần và kiểm đầu vào với floorPrice của snapshot đó")
    void snapshotOncePerRequest() {
        RequirementProfileDto dto = profile(0.9);
        when(extractor.extract(any(), any(), any(), any(), any(), any())).thenReturn(new ProfileExtractor.Extraction(dto, "OK"));
        stubHappyPath(dto);
        when(solver.floorPrice(snapshot)).thenReturn(BigDecimal.valueOf(6_778_000));

        service.processAiBuild(AiBuildRequest.builder().budget(budget).purpose("Game").build(), null);

        verify(facade).snapshot();
        verify(requestValidator).validate(any(), eq(BigDecimal.valueOf(6_778_000)));
    }
}
