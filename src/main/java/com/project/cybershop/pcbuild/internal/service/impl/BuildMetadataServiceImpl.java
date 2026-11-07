package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.pcbuild.internal.entity.BudgetProfile;
import com.project.cybershop.pcbuild.internal.entity.BuildTemplateSlot;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.PurposeMeta;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.SlotMeta;
import com.project.cybershop.pcbuild.internal.repository.BudgetProfileRepository;
import com.project.cybershop.pcbuild.internal.repository.BuildTemplateSlotRepository;
import com.project.cybershop.pcbuild.internal.service.BuildMetadataService;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Nạp khuôn bộ PC và chia ngân sách với cùng các bước kiểm như /requirements/analyze,
 * để hai luồng đọc ra một cấu hình (cùng tỉ lệ, cùng min_budget, cùng maxBuildPrice).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildMetadataServiceImpl implements BuildMetadataService {

    private static final String GENERAL = "GENERAL";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BuildTemplateSlotRepository slotRepository;
    private final BudgetProfileRepository budgetProfileRepository;
    private final CatalogPublicApi catalogPublicApi;

    @Override
    @Transactional(readOnly = true)
    public BuildMetadata loadMetadata(List<ComponentCandidateDTO> candidates) {
        List<SlotMeta> slots = loadSlots();
        Map<String, PurposeMeta> purposes = loadPurposes(slots);
        return BuildMetadata.builder()
                .slots(slots)
                .purposes(purposes)
                .maxBuildPrice(maxBuildPrice(slots, candidates))
                .build();
    }

    private List<SlotMeta> loadSlots() {
        List<BuildTemplateSlot> rawSlots = slotRepository.findAllByOrderByStepOrderAsc();
        if (rawSlots.isEmpty()) {
            throw metadataInvalid("Bảng build_template_slots bị trống.");
        }
        Map<Integer, CategoryResponse> categories = new HashMap<>();
        for (CategoryResponse category : catalogPublicApi.getAllCategories()) {
            if (category != null && category.id() != null) {
                categories.put(category.id(), category);
            }
        }
        List<SlotMeta> slots = new ArrayList<>();
        for (BuildTemplateSlot slot : rawSlots) {
            CategoryResponse category = categories.get(slot.getCategoryId());
            String slug = category != null && category.slug() != null && !category.slug().isBlank()
                    ? category.slug().trim()
                    : "slot_" + slot.getId();
            String name = category != null && category.name() != null && !category.name().isBlank()
                    ? category.name().trim()
                    : "Linh kiện " + slot.getId();
            slots.add(SlotMeta.builder()
                    .slotId(slot.getId())
                    .categoryId(slot.getCategoryId())
                    .componentCode(slug.toUpperCase(Locale.ROOT))
                    .label(name)
                    .stepOrder(slot.getStepOrder())
                    .required(Boolean.TRUE.equals(slot.getRequired()))
                    .minQty(slot.getMinQty())
                    .maxQty(slot.getMaxQty())
                    .requiredIfCategoryId(slot.getRequiredIfCategoryId())
                    .requiredIfSpecId(slot.getRequiredIfSpecId())
                    .requiredIfValue(slot.getRequiredIfValue())
                    .build());
        }
        return slots;
    }

    private Map<String, PurposeMeta> loadPurposes(List<SlotMeta> slots) {
        List<BudgetProfile> rawProfiles = budgetProfileRepository.findAllByOrderByIdAsc();
        if (rawProfiles.isEmpty()) {
            throw metadataInvalid("Bảng budget_profiles bị trống.");
        }
        Map<String, List<BudgetProfile>> profilesByPurpose = new LinkedHashMap<>();
        for (BudgetProfile profile : rawProfiles) {
            profilesByPurpose.computeIfAbsent(profile.getPurpose(), key -> new ArrayList<>()).add(profile);
        }
        if (!profilesByPurpose.containsKey(GENERAL)) {
            throw metadataInvalid("Cấu hình budget_profiles thiếu mục đích bắt buộc " + GENERAL + ".");
        }
        Set<Integer> slotIds = new LinkedHashSet<>();
        for (SlotMeta slot : slots) {
            slotIds.add(slot.getSlotId());
        }

        Map<String, PurposeMeta> purposes = new LinkedHashMap<>();
        for (Map.Entry<String, List<BudgetProfile>> entry : profilesByPurpose.entrySet()) {
            String purpose = entry.getKey();
            Map<Integer, BigDecimal> sharePctBySlot = new HashMap<>();
            TreeSet<BigDecimal> minBudgets = new TreeSet<>();
            BigDecimal totalShare = BigDecimal.ZERO;
            for (BudgetProfile profile : entry.getValue()) {
                BigDecimal sharePct = profile.getSharePct() != null ? profile.getSharePct() : BigDecimal.ZERO;
                sharePctBySlot.put(profile.getSlot().getId(), sharePct);
                totalShare = totalShare.add(sharePct);
                if (profile.getMinBudget() != null) {
                    minBudgets.add(profile.getMinBudget());
                }
            }
            if (!sharePctBySlot.keySet().containsAll(slotIds)) {
                throw metadataInvalid("Mục đích " + purpose + " không có đủ cấu hình cho toàn bộ các slot.");
            }
            if (totalShare.compareTo(ONE_HUNDRED) != 0) {
                throw metadataInvalid("Tổng share_pct của purpose " + purpose + " là " + totalShare + "%, không bằng 100%.");
            }
            if (minBudgets.size() > 1) {
                log.warn("Purpose {} có nhiều mức min_budget {}, sẽ sử dụng mức cao nhất.", purpose, minBudgets);
            }
            purposes.put(purpose, PurposeMeta.builder()
                    .purpose(purpose)
                    .label(purpose)
                    .minBudget(minBudgets.isEmpty() ? 0L : minBudgets.last().longValue())
                    .sharePctBySlotId(sharePctBySlot)
                    .build());
        }
        return purposes;
    }

    /** Cùng công thức với RequirementServiceImpl (mỗi slot lấy min(max_qty, 2) món đắt nhất) để hai API cùng một cận trên. */
    static long maxBuildPrice(List<SlotMeta> slots, List<ComponentCandidateDTO> candidates) {
        long total = 0L;
        for (SlotMeta slot : slots) {
            int quantity = slot.getMaxQty() != null && slot.getMaxQty() > 0 ? Math.min(slot.getMaxQty(), 2) : 1;
            long highestPrice = candidates.stream()
                    .filter(candidate -> Objects.equals(candidate.getCategoryId(), slot.getCategoryId())
                            && candidate.getPrice() != null)
                    .mapToLong(candidate -> candidate.getPrice().longValue())
                    .max()
                    .orElse(0L);
            total += highestPrice * quantity;
        }
        return total;
    }

    private static PcBuildException metadataInvalid(String detail) {
        return new PcBuildException("METADATA_INVALID", HttpStatus.SERVICE_UNAVAILABLE,
                "Cấu hình dữ liệu AI Build PC không hợp lệ: " + detail);
    }
}
