package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.RequirementAnalyzeRequest;
import com.project.cybershop.pcbuild.api.dto.response.PurposeOptionResponse;
import com.project.cybershop.pcbuild.api.dto.response.RequirementAnalyzeResponse;
import com.project.cybershop.pcbuild.api.dto.response.RequirementStatus;
import com.project.cybershop.pcbuild.internal.entity.BudgetProfile;
import com.project.cybershop.pcbuild.internal.entity.BuildTemplateSlot;
import com.project.cybershop.pcbuild.internal.model.RequirementDraft;
import com.project.cybershop.pcbuild.internal.repository.BudgetProfileRepository;
import com.project.cybershop.pcbuild.internal.repository.BuildTemplateSlotRepository;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import com.project.cybershop.pcbuild.internal.support.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Bước 1–3 không cần DB và AI thật: metadata giả, AI giả. Cận trên của kho giả = 21.500.000đ. */
class RequirementServiceImplTest {

    private final LlmClientService llm = mock(LlmClientService.class);
    private RequirementServiceImpl service;

    @BeforeEach
    void setUp() {
        BudgetProfileRepository profiles = mock(BudgetProfileRepository.class);
        BuildTemplateSlotRepository slots = mock(BuildTemplateSlotRepository.class);
        CatalogPublicApi catalog = mock(CatalogPublicApi.class);

        List<BuildTemplateSlot> slotRows = new ArrayList<>();
        for (int categoryId = 1; categoryId <= 8; categoryId++) {
            BuildTemplateSlot slot = new BuildTemplateSlot();
            slot.setId(categoryId);
            slot.setCategoryId(categoryId);
            slotRows.add(slot);
        }
        when(slots.findAllByOrderByStepOrderAsc()).thenReturn(slotRows);
        when(profiles.findAllByOrderByIdAsc()).thenReturn(List.of(
                profile("GAMING", 14_500_000), profile("GAMING", 14_500_000),
                profile("OFFICE", 10_500_000), profile("GENERAL", 14_500_000)));
        when(catalog.getActiveCandidatesForBuild()).thenReturn(List.of(
                part(1, "AMD", 5_000_000), part(1, "Intel", 4_000_000), part(2, "ASUS", 3_000_000),
                part(3, "Kingston", 1_000_000), part(4, "MSI", 8_000_000), part(5, "Samsung", 1_000_000),
                part(6, "Corsair", 1_000_000), part(7, "NZXT", 1_000_000), part(8, "Deepcool", 500_000)));
        when(catalog.getSpecificationCatalog()).thenReturn(List.of(CategorySpecDto.builder()
                .categoryId(2).specificationName("Form_Factor")
                .options(List.of(new CategorySpecDto.Option("MICRO_ATX", "Micro-ATX"),
                        new CategorySpecDto.Option("STD_ATX", "ATX")))
                .build()));

        service = new RequirementServiceImpl(profiles, slots, catalog, llm,
                new RateLimiter(Clock.systemUTC(), 100), new ObjectMapper());
    }

    @Test
    void aiReadsNeed_ready() {
        aiReturns(new RequirementDraft("GAMING", null, null, "amd"));

        RequirementAnalyzeResponse response = service.analyze(request(15_000_000L, "Muốn build máy chơi game, thích AMD"), "ip:1");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.READY);
        assertThat(response.getRequirement().getPurpose()).isEqualTo("GAMING");
        assertThat(response.getRequirement().getBrandPreference()).isEqualTo("AMD");
        assertThat(response.getRequirement().getBudget()).isEqualTo(15_000_000L);
        assertThat(response.getMaxBuildPrice()).isEqualTo(21_500_000L);
        assertThat(response.getNotes()).isEmpty();
    }

    @Test
    void noNeed_generalWithoutAi() {
        RequirementAnalyzeResponse response = service.analyze(request(15_000_000L, "  "), "ip:1");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.READY);
        assertThat(response.getRequirement().getPurpose()).isEqualTo("GENERAL");
        verify(llm, never()).generateJson(anyString(), anyString(), anyString(), anyMap(), eq(RequirementDraft.class));
    }

    @Test
    void budgetChecks() {
        aiReturns(new RequirementDraft("GAMING", null, null, null));
        RequirementAnalyzeResponse below = service.analyze(request(12_000_000L, "chơi game"), "ip:1");
        assertThat(below.getStatus()).isEqualTo(RequirementStatus.BELOW_MIN_BUDGET);
        assertThat(below.getMessage()).contains("14.500.000đ").contains("12.000.000đ");
        assertThat(below.getSuggestedPurposes()).extracting(PurposeOptionResponse::getPurpose).containsExactly("OFFICE");

        assertThat(service.analyze(request(9_000_000L, null), "ip:1").getStatus())
                .isEqualTo(RequirementStatus.NO_AFFORDABLE_PURPOSE);
        assertThat(service.analyze(request(30_000_000L, null), "ip:1").getStatus())
                .isEqualTo(RequirementStatus.BUDGET_TOO_HIGH);
        assertThat(service.analyze(request(21_500_000L, null), "ip:1").getStatus())
                .isEqualTo(RequirementStatus.READY);
    }

    @Test
    void aiOddValues_normalizedWithNotes() {
        aiReturns(new RequirementDraft("MINING", "AAA_FULL", "TINY", "Apple"));

        RequirementAnalyzeResponse response = service.analyze(request(15_000_000L, "đào coin"), "ip:1");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.READY);
        assertThat(response.getRequirement().getPurpose()).isEqualTo("GENERAL");
        assertThat(response.getRequirement().getLevel()).isNull();
        assertThat(response.getRequirement().getFormFactor()).isNull();
        assertThat(response.getRequirement().getBrandPreference()).isNull();
        assertThat(response.getNotes()).hasSize(4);
    }

    @Test
    void aiDown_generalWithNote() {
        when(llm.generateJson(anyString(), anyString(), anyString(), anyMap(), eq(RequirementDraft.class)))
                .thenThrow(new AiUnavailableException("hết giờ"));

        RequirementAnalyzeResponse response = service.analyze(request(20_000_000L, "chơi game"), "ip:1");

        assertThat(response.getStatus()).isEqualTo(RequirementStatus.READY);
        assertThat(response.getRequirement().getPurpose()).isEqualTo("GENERAL");
        assertThat(response.getNotes()).hasSize(1);
    }

    @Test
    void options_listsMetadata() {
        var options = service.options();
        assertThat(options.getPurposes()).extracting(PurposeOptionResponse::getPurpose)
                .containsExactly("GAMING", "OFFICE", "GENERAL");
        assertThat(options.getBrands()).containsExactly("AMD", "Intel");
        assertThat(options.getFormFactors()).hasSize(2);
        assertThat(RequirementServiceImpl.vnd(14_500_000L)).isEqualTo("14.500.000đ");
    }

    private void aiReturns(RequirementDraft draft) {
        when(llm.generateJson(anyString(), anyString(), anyString(), anyMap(), eq(RequirementDraft.class))).thenReturn(draft);
    }

    private static RequirementAnalyzeRequest request(Long budget, String message) {
        return RequirementAnalyzeRequest.builder().budget(budget).message(message).build();
    }

    private static BudgetProfile profile(String purpose, long minBudget) {
        BudgetProfile profile = new BudgetProfile();
        profile.setPurpose(purpose);
        profile.setMinBudget(BigDecimal.valueOf(minBudget));
        return profile;
    }

    private static ComponentCandidateDTO part(int categoryId, String brand, long price) {
        ComponentCandidateDTO candidate = new ComponentCandidateDTO();
        candidate.setCategoryId(categoryId);
        candidate.setBrandName(brand);
        candidate.setPrice(BigDecimal.valueOf(price));
        return candidate;
    }
}
