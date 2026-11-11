package com.project.cybershop.pcbuild.internal.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Dữ liệu cấu hình nạp từ bảng build_template_slots và budget_profiles, đã qua kiểm tra tính hợp lệ. */
@Value
@Builder
public class BuildMetadata {
    List<SlotMeta> slots;
    Map<String, PurposeMeta> purposes;
    long maxBuildPrice;

    public Long minBudget(String purpose) {
        PurposeMeta meta = purposes.get(purpose);
        return meta != null ? meta.getMinBudget() : null;
    }

    public Long lowestMinBudget() {
        return purposes.values().stream()
                .map(PurposeMeta::getMinBudget)
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElse(0L);
    }

    public Set<String> purposeCodes() {
        return purposes.keySet();
    }

    public String getPurposeLabel(String purpose) {
        PurposeMeta meta = purposes.get(purpose);
        return meta != null ? meta.getLabel() : purpose;
    }

    @Value
    @Builder
    public static class SlotMeta {
        Integer slotId;
        Integer categoryId;
        String componentCode;
        String label;
        Integer stepOrder;
        boolean required;
        Integer minQty;
        Integer maxQty;
        Integer requiredIfCategoryId;
        Integer requiredIfSpecId;
        String requiredIfValue;
    }

    @Value
    @Builder
    public static class PurposeMeta {
        String purpose;
        String label;
        String description;
        Long minBudget;
        Map<Integer, BigDecimal> sharePctBySlotId;

        public BigDecimal getSharePct(Integer slotId) {
            return sharePctBySlotId.getOrDefault(slotId, BigDecimal.ZERO);
        }
    }
}

