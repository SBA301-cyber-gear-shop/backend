package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.pcbuild.internal.entity.BudgetProfile;
import com.project.cybershop.pcbuild.internal.entity.BuildTemplateSlot;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata;
import com.project.cybershop.pcbuild.internal.repository.BudgetProfileRepository;
import com.project.cybershop.pcbuild.internal.repository.BuildTemplateSlotRepository;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.project.cybershop.pcbuild.internal.service.impl.BuildFixtures.CATEGORY_BY_STEP;
import static com.project.cybershop.pcbuild.internal.service.impl.BuildFixtures.CODE_BY_STEP;
import static com.project.cybershop.pcbuild.internal.service.impl.BuildFixtures.GAMING_SHARES;
import static com.project.cybershop.pcbuild.internal.service.impl.BuildFixtures.MAX_QTY_BY_STEP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Nạp khuôn bộ PC: nguồn metadata chung của /requirements/analyze và /builds/auto. */
class BuildMetadataServiceImplTest {

    private final List<BuildTemplateSlot> slots = new ArrayList<>();
    private final List<BudgetProfile> profiles = new ArrayList<>();
    private BuildMetadataServiceImpl service;

    @BeforeEach
    void setUp() {
        for (int step = 0; step < CATEGORY_BY_STEP.length; step++) {
            boolean gpu = CATEGORY_BY_STEP[step] == 4;
            BuildTemplateSlot slot = new BuildTemplateSlot();
            slot.setId(CATEGORY_BY_STEP[step]);
            slot.setCategoryId(CATEGORY_BY_STEP[step]);
            slot.setStepOrder(step + 1);
            slot.setRequired(!gpu);
            slot.setMinQty(gpu ? 0 : 1);
            slot.setMaxQty(MAX_QTY_BY_STEP[step]);
            slots.add(slot);
        }
        addProfiles("GENERAL", GAMING_SHARES);

        BuildTemplateSlotRepository slotRepository = mock(BuildTemplateSlotRepository.class);
        when(slotRepository.findAllByOrderByStepOrderAsc()).thenReturn(slots);
        BudgetProfileRepository profileRepository = mock(BudgetProfileRepository.class);
        when(profileRepository.findAllByOrderByIdAsc()).thenReturn(profiles);
        CatalogPublicApi catalogPublicApi = mock(CatalogPublicApi.class);
        List<CategoryResponse> categories = new ArrayList<>();
        for (int step = 0; step < CATEGORY_BY_STEP.length; step++) {
            categories.add(new CategoryResponse(CATEGORY_BY_STEP[step], CODE_BY_STEP[step],
                    CODE_BY_STEP[step].toLowerCase(Locale.ROOT), null, null, List.of(), List.of()));
        }
        when(catalogPublicApi.getAllCategories()).thenReturn(categories);
        service = new BuildMetadataServiceImpl(slotRepository, profileRepository, catalogPublicApi);
    }

    @Test
    @DisplayName("Slot lấy mã từ slug danh mục; maxBuildPrice = Σ giá đắt nhất × min(max_qty, 2)")
    void loadsSlotsAndMaxBuildPrice() {
        List<ComponentCandidateDTO> candidates = new ArrayList<>();
        for (int categoryId : CATEGORY_BY_STEP) {
            candidates.add(ComponentCandidateDTO.builder().categoryId(categoryId).price(BigDecimal.valueOf(1_000_000)).build());
        }

        BuildMetadata metadata = service.loadMetadata(candidates);

        assertThat(metadata.getSlots()).extracting(BuildMetadata.SlotMeta::getComponentCode).containsExactly(CODE_BY_STEP);
        // max_qty 1,1,1,2,1,8,6,4,2,1,1 → số lượng 1,1,1,2,1,2,2,2,2,1,1 = 16 món × 1.000.000
        assertThat(metadata.getMaxBuildPrice()).isEqualTo(16_000_000L);
        assertThat(metadata.minBudget("GENERAL")).isEqualTo(16_000_000L);
    }

    @Test
    @DisplayName("Tổng share_pct khác 100% → 503 METADATA_INVALID")
    void shareNotHundred_isInvalid() {
        profiles.get(0).setSharePct(BigDecimal.valueOf(13));

        assertThatThrownBy(() -> service.loadMetadata(List.of()))
                .isInstanceOfSatisfying(PcBuildException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("METADATA_INVALID");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(503);
                });
    }

    @Test
    @DisplayName("Thiếu mục đích GENERAL → 503 METADATA_INVALID")
    void missingGeneral_isInvalid() {
        profiles.forEach(profile -> profile.setPurpose("GAMING"));

        assertThatThrownBy(() -> service.loadMetadata(List.of()))
                .isInstanceOfSatisfying(PcBuildException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("METADATA_INVALID"));
    }

    private void addProfiles(String purpose, int[] shares) {
        for (int index = 0; index < slots.size(); index++) {
            BudgetProfile profile = new BudgetProfile();
            profile.setId(profiles.size() + 1);
            profile.setPurpose(purpose);
            profile.setSlot(slots.get(index));
            profile.setSharePct(BigDecimal.valueOf(shares[index]));
            profile.setMinBudget(BigDecimal.valueOf(16_000_000L));
            profiles.add(profile);
        }
    }
}
