package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.pcbuild.api.dto.request.RequirementAnalyzeRequest;
import com.project.cybershop.pcbuild.api.dto.response.RequirementAnalyzeResponse;
import com.project.cybershop.pcbuild.api.dto.response.RequirementStatus;
import com.project.cybershop.pcbuild.internal.entity.BudgetProfile;
import com.project.cybershop.pcbuild.internal.entity.BuildTemplateSlot;
import com.project.cybershop.pcbuild.internal.model.BudgetFit;
import com.project.cybershop.pcbuild.internal.model.NeedAnalysis;
import com.project.cybershop.pcbuild.internal.repository.BudgetProfileRepository;
import com.project.cybershop.pcbuild.internal.repository.BuildTemplateSlotRepository;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import com.project.cybershop.pcbuild.internal.support.LlmClient;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import com.project.cybershop.pcbuild.internal.support.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RequirementServiceImplTest {

    private BuildTemplateSlotRepository slotRepository;
    private BudgetProfileRepository budgetProfileRepository;
    private CatalogPublicApi catalogPublicApi;
    private LlmClient llmClient;
    private RateLimiter rateLimiter;

    private RequirementServiceImpl service;

    private List<BuildTemplateSlot> sampleSlots;
    private List<BudgetProfile> sampleProfiles;

    @BeforeEach
    void setUp() {
        slotRepository = mock(BuildTemplateSlotRepository.class);
        budgetProfileRepository = mock(BudgetProfileRepository.class);
        catalogPublicApi = mock(CatalogPublicApi.class);
        llmClient = mock(LlmClient.class);

        // Tạo 11 slots mẫu theo spec
        sampleSlots = new ArrayList<>();
        int[] catIds = {9, 10, 11, 1, 2, 3, 5, 4, 8, 7, 6};
        for (int i = 0; i < catIds.length; i++) {
            BuildTemplateSlot slot = new BuildTemplateSlot();
            slot.setId(catIds[i]);
            slot.setCategoryId(catIds[i]);
            slot.setStepOrder(i + 1);
            slot.setRequired(true);
            slot.setMinQty(1);
            slot.setMaxQty(1);
            sampleSlots.add(slot);
        }
        when(slotRepository.findAllByOrderByStepOrderAsc()).thenReturn(sampleSlots);

        // Tạo profile cho 6 purposes: GAMING, CREATIVE, AI_DATA, OFFICE, DEV, GENERAL
        sampleProfiles = new ArrayList<>();
        // GAMING: min 16tr
        int[] gamingShares = {12, 4, 3, 15, 9, 8, 6, 28, 3, 5, 7};
        addProfiles("GAMING", 16_000_000L, gamingShares);

        // OFFICE: min 12.5tr, GPU share = 0
        int[] officeShares = {16, 4, 3, 23, 14, 12, 12, 0, 4, 5, 7};
        addProfiles("OFFICE", 12_500_000L, officeShares);

        // GENERAL: min 16tr
        int[] generalShares = {11, 3, 2, 18, 10, 10, 9, 22, 3, 5, 7};
        addProfiles("GENERAL", 16_000_000L, generalShares);

        // CREATIVE: min 16tr
        int[] creativeShares = {15, 3, 2, 21, 9, 14, 9, 16, 3, 3, 5};
        addProfiles("CREATIVE", 16_000_000L, creativeShares);

        when(budgetProfileRepository.findAllByOrderByIdAsc()).thenReturn(sampleProfiles);

        // Mock catalog candidates để tính maxBuildPrice (ví dụ 100tr)
        List<ComponentCandidateDTO> candidates = new ArrayList<>();
        for (int catId : catIds) {
            candidates.add(ComponentCandidateDTO.builder()
                    .categoryId(catId)
                    .price(BigDecimal.valueOf(10_000_000L))
                    .stockQuantity(10)
                    .build());
        }
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(candidates);
        List<CategoryResponse> categories = List.of(
                new CategoryResponse(1, "CPU", "cpu", null, null, List.of(), List.of()),
                new CategoryResponse(2, "Mainboard", "mainboard", null, null, List.of(), List.of()),
                new CategoryResponse(3, "RAM", "ram", null, null, List.of(), List.of()),
                new CategoryResponse(4, "Card đồ họa", "gpu", null, null, List.of(), List.of()),
                new CategoryResponse(5, "SSD", "ssd", null, null, List.of(), List.of()),
                new CategoryResponse(6, "Nguồn máy tính", "psu", null, null, List.of(), List.of()),
                new CategoryResponse(7, "Vỏ case", "case", null, null, List.of(), List.of()),
                new CategoryResponse(8, "Tản nhiệt", "cooler", null, null, List.of(), List.of()),
                new CategoryResponse(9, "Màn hình", "monitor", null, null, List.of(), List.of()),
                new CategoryResponse(10, "Bàn phím", "keyboard", null, null, List.of(), List.of()),
                new CategoryResponse(11, "Chuột", "mouse", null, null, List.of(), List.of())
        );
        when(catalogPublicApi.getAllCategories()).thenReturn(categories);

        rateLimiter = new RateLimiter(Clock.systemUTC(), 100);

        service = new RequirementServiceImpl(
                new BuildMetadataServiceImpl(slotRepository, budgetProfileRepository, catalogPublicApi),
                catalogPublicApi,
                llmClient,
                rateLimiter,
                new ObjectMapper()
        );
    }

    private void addProfiles(String purpose, long minBudget, int[] shares) {
        for (int i = 0; i < sampleSlots.size(); i++) {
            BudgetProfile p = new BudgetProfile();
            p.setId(sampleProfiles.size() + 1);
            p.setPurpose(purpose);
            p.setSlot(sampleSlots.get(i));
            p.setSharePct(BigDecimal.valueOf(shares[i]));
            p.setMinBudget(BigDecimal.valueOf(minBudget));
            sampleProfiles.add(p);
        }
    }

    @Test
    @DisplayName("Ca 1: Build PC chơi game 1440p, 30tr, AI GAMING, fit = true -> READY, chỉ trả {budget, purpose}")
    void case1_ready_onlyBudgetAndPurpose() {
        when(llmClient.generateJson(eq("analyzeNeed"), anyString(), anyString(), anyMap(), eq(NeedAnalysis.class)))
                .thenReturn(NeedAnalysis.builder()
                        .needsClarification(false)
                        .purpose("GAMING")
                        .performanceExpectation("1440p")
                        .reason("Chơi game")
                        .build());

        when(llmClient.generateJson(eq("evaluateBudgetFit"), anyString(), anyString(), anyMap(), eq(BudgetFit.class)))
                .thenReturn(BudgetFit.builder()
                        .budgetFit(true)
                        .explanation("Với 30 triệu, cấu hình đáp ứng mượt mà game AAA 1440p.")
                        .build());

        RequirementAnalyzeResponse response = service.analyze(new RequirementAnalyzeRequest(30_000_000L, "Build PC chơi game 1440p"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.READY);
        assertThat(response.getRequirement().getPurpose()).isEqualTo("GAMING");
        assertThat(response.getRequirement().getBudget()).isEqualTo(30_000_000L);
        assertThat(response.getMessage()).isEqualTo("Shop đã hiểu yêu cầu: PC chơi game, ngân sách 30.000.000đ.");
        assertThat(response.getExplanation()).contains("1440p");
    }

    @Test
    @DisplayName("Ca 2: Nhu cầu mơ hồ/vô nghĩa -> NEED_CLARIFICATION, không gọi AI lần 2")
    void case2_needClarification() {
        when(llmClient.generateJson(eq("analyzeNeed"), anyString(), anyString(), anyMap(), eq(NeedAnalysis.class)))
                .thenReturn(NeedAnalysis.builder()
                        .needsClarification(true)
                        .clarificationQuestion("Bạn muốn build PC để phục vụ nhu cầu nào?")
                        .purpose("GENERAL")
                        .build());

        RequirementAnalyzeResponse response = service.analyze(new RequirementAnalyzeRequest(25_000_000L, "Build PC để chơi game và bộ vệ sinh đắt nhất"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.NEED_CLARIFICATION);
        assertThat(response.getMessage()).contains("Bạn muốn build PC để phục vụ nhu cầu nào?");
        assertThat(response.getRequirement().getPurpose()).isNull();
        // Không gọi AI lần 2
        verify(llmClient, never()).generateJson(eq("evaluateBudgetFit"), anyString(), anyString(), anyMap(), eq(BudgetFit.class));
    }

    @Test
    @DisplayName("Ca 3: GAMING, 14tr (thấp hơn min 16tr) -> BUDGET_TOO_LOW, gợi ý OFFICE")
    void case3_budgetTooLow_suggestOffice() {
        when(llmClient.generateJson(eq("analyzeNeed"), anyString(), anyString(), anyMap(), eq(NeedAnalysis.class)))
                .thenReturn(NeedAnalysis.builder()
                        .needsClarification(false)
                        .purpose("GAMING")
                        .build());

        RequirementAnalyzeResponse response = service.analyze(new RequirementAnalyzeRequest(14_000_000L, "Build máy chơi game"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.BUDGET_TOO_LOW);
        assertThat(response.getMinBudget()).isEqualTo(16_000_000L);
        assertThat(response.getSuggestedPurposes()).isNotEmpty();
        assertThat(response.getSuggestedPurposes().get(0).getPurpose()).isEqualTo("OFFICE");
        // Không gọi AI lần 2
        verify(llmClient, never()).generateJson(eq("evaluateBudgetFit"), anyString(), anyString(), anyMap(), eq(BudgetFit.class));
    }

    @Test
    @DisplayName("Ca 4: GAMING, 5tr -> BUDGET_TOO_LOW, suggestedPurposes rỗng vì không purpose nào vừa")
    void case4_budgetTooLow_noSuggestion() {
        when(llmClient.generateJson(eq("analyzeNeed"), anyString(), anyString(), anyMap(), eq(NeedAnalysis.class)))
                .thenReturn(NeedAnalysis.builder()
                        .needsClarification(false)
                        .purpose("GAMING")
                        .build());

        RequirementAnalyzeResponse response = service.analyze(new RequirementAnalyzeRequest(5_000_000L, "chơi game"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.BUDGET_TOO_LOW);
        assertThat(response.getSuggestedPurposes()).isEmpty();
        assertThat(response.getMessage()).contains("Shop hiện chưa có bộ PC nào dưới");
    }

    @Test
    @DisplayName("Ca 5: AAA full setting, 16tr, fit = false -> BUDGET_WARNING, suggestedBudget 35tr, có note ước lượng")
    void case5_budgetWarning_withSuggestedBudget() {
        when(llmClient.generateJson(eq("analyzeNeed"), anyString(), anyString(), anyMap(), eq(NeedAnalysis.class)))
                .thenReturn(NeedAnalysis.builder()
                        .needsClarification(false)
                        .purpose("GAMING")
                        .build());

        when(llmClient.generateJson(eq("evaluateBudgetFit"), anyString(), anyString(), anyMap(), eq(BudgetFit.class)))
                .thenReturn(BudgetFit.builder()
                        .budgetFit(false)
                        .explanation("Với 16 triệu chỉ chơi được thiết lập thấp đến trung bình.")
                        .suggestedBudget(35_000_000L)
                        .build());

        RequirementAnalyzeResponse response = service.analyze(new RequirementAnalyzeRequest(16_000_000L, "AAA full setting"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.BUDGET_WARNING);
        assertThat(response.getSuggestedBudget()).isEqualTo(35_000_000L);
        assertThat(response.getRequirement().getPurpose()).isEqualTo("GAMING");
        assertThat(response.getNotes()).anyMatch(note -> note.contains("ước lượng"));
    }

    @Test
    @DisplayName("Ca 6: fit = false, suggestedBudget <= budget -> suggestedBudget = null")
    void case6_budgetWarning_invalidSuggestedBudget_setNull() {
        when(llmClient.generateJson(eq("analyzeNeed"), anyString(), anyString(), anyMap(), eq(NeedAnalysis.class)))
                .thenReturn(NeedAnalysis.builder()
                        .needsClarification(false)
                        .purpose("GAMING")
                        .build());

        when(llmClient.generateJson(eq("evaluateBudgetFit"), anyString(), anyString(), anyMap(), eq(BudgetFit.class)))
                .thenReturn(BudgetFit.builder()
                        .budgetFit(false)
                        .explanation("Cần nâng ngân sách.")
                        .suggestedBudget(15_000_000L) // <= budget (16_000_000L)
                        .build());

        RequirementAnalyzeResponse response = service.analyze(new RequirementAnalyzeRequest(16_000_000L, "AAA full setting"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.BUDGET_WARNING);
        assertThat(response.getSuggestedBudget()).isNull();
    }

    @Test
    @DisplayName("Ca 7: Nhiều nhu cầu (chơi game và đồ họa) -> GAMING + secondary CREATIVE ghi vào notes")
    void case7_multiplePurposes_noteSecondary() {
        when(llmClient.generateJson(eq("analyzeNeed"), anyString(), anyString(), anyMap(), eq(NeedAnalysis.class)))
                .thenReturn(NeedAnalysis.builder()
                        .needsClarification(false)
                        .purpose("GAMING")
                        .secondaryPurposes(List.of("CREATIVE"))
                        .build());

        when(llmClient.generateJson(eq("evaluateBudgetFit"), anyString(), anyString(), anyMap(), eq(BudgetFit.class)))
                .thenReturn(BudgetFit.builder().budgetFit(true).explanation("Phù hợp.").build());

        RequirementAnalyzeResponse response = service.analyze(new RequirementAnalyzeRequest(25_000_000L, "chơi game và đồ họa"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.READY);
        assertThat(response.getNotes()).anyMatch(note -> note.contains("PC đồ họa"));
    }

    @Test
    @DisplayName("Ca 8: AI trả purpose lạ -> 503 AI_UNAVAILABLE")
    void case8_unknownPurpose_throwAiUnavailable() {
        when(llmClient.generateJson(anyString(), anyString(), anyString(), anyMap(), eq(NeedAnalysis.class)))
                .thenReturn(NeedAnalysis.builder()
                        .needsClarification(false)
                        .purpose("MINING_COIN")
                        .build());

        assertThatThrownBy(() -> service.analyze(new RequirementAnalyzeRequest(20_000_000L, "đào coin"), "ip:test"))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    @DisplayName("Ca 9: Cấu hình sai (tổng share_pct != 100) -> 503 METADATA_INVALID")
    void case9_invalidConfig_throwMetadataInvalid() {
        // Thay đổi profile của GENERAL thành tổng khác 100%
        sampleProfiles.get(sampleProfiles.size() - 1).setSharePct(BigDecimal.valueOf(50));

        assertThatThrownBy(() -> service.analyze(new RequirementAnalyzeRequest(20_000_000L, "chơi game"), "ip:test"))
                .isInstanceOf(PcBuildException.class)
                .hasMessageContaining("không bằng 100%");
    }

    @Test
    @DisplayName("Ca 10: budget > maxBuildPrice -> BUDGET_TOO_HIGH, không gọi AI")
    void case10_budgetTooHigh_noAiCall() {
        // maxBuildPrice khoảng 120tr (11 slots * 10tr, RAM x2)
        RequirementAnalyzeResponse response = service.analyze(new RequirementAnalyzeRequest(2_000_000_000L, "máy đắt nhất"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.BUDGET_TOO_HIGH);
        assertThat(response.getMessage()).contains("vượt quá giá trị");
        verify(llmClient, never()).generateJson(anyString(), anyString(), anyString(), anyMap(), any());
    }

    @Test
    @DisplayName("Ca 11: OFFICE đủ tiền -> READY; AI không nhận budgetAllocation hay slots (chia tiền là việc của /builds/auto)")
    void case11_aiInputHasNoAllocation() {
        when(llmClient.generateJson(eq("analyzeNeed"), anyString(), anyString(), anyMap(), eq(NeedAnalysis.class)))
                .thenReturn(NeedAnalysis.builder()
                        .needsClarification(false)
                        .purpose("OFFICE")
                        .build());

        when(llmClient.generateJson(eq("evaluateBudgetFit"), anyString(), anyString(), anyMap(), eq(BudgetFit.class)))
                .thenReturn(BudgetFit.builder().budgetFit(true).explanation("Phù hợp văn phòng.").build());

        RequirementAnalyzeResponse response = service.analyze(new RequirementAnalyzeRequest(15_000_000L, "máy văn phòng"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.READY);
        assertThat(response.getRequirement().getPurpose()).isEqualTo("OFFICE");
        assertThat(response.getMinBudget()).isEqualTo(12_500_000L);

        ArgumentCaptor<String> needInput = ArgumentCaptor.forClass(String.class);
        verify(llmClient).generateJson(eq("analyzeNeed"), anyString(), needInput.capture(), anyMap(), eq(NeedAnalysis.class));
        assertThat(needInput.getValue()).contains("validPurposes").doesNotContain("\"slots\"");

        ArgumentCaptor<String> fitInput = ArgumentCaptor.forClass(String.class);
        verify(llmClient).generateJson(eq("evaluateBudgetFit"), anyString(), fitInput.capture(), anyMap(), eq(BudgetFit.class));
        assertThat(fitInput.getValue()).contains("\"minBudget\":12500000").doesNotContain("budgetAllocation");
    }
}