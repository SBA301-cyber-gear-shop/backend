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
 * Nạp và kiểm khuôn bộ PC (build_template_slots, budget_profiles). Nguồn metadata duy nhất cho cả
 * /requirements/analyze và /builds/auto, nên hai API luôn cùng min_budget và cùng maxBuildPrice.
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
                log.warn("Tổng share_pct của purpose {} là {}%, khác 100%.", purpose, totalShare);
            }
            if (minBudgets.size() > 1) {
                log.warn("Purpose {} có nhiều mức min_budget {}, sẽ sử dụng mức cao nhất.", purpose, minBudgets);
            }
            purposes.put(purpose, PurposeMeta.builder()
                    .purpose(purpose)
                    .label(purposeLabel(purpose))
                    .description(purposeDescription(purpose))
                    .minBudget(minBudgets.isEmpty() ? 0L : minBudgets.last().longValue())
                    .sharePctBySlotId(sharePctBySlot)
                    .build());
        }
        return purposes;
    }

    /** Nhãn khách đọc, dùng trong câu trả lời của /requirements/analyze và gửi AI làm danh mục purpose hợp lệ. */
    private static String purposeLabel(String purpose) {
        return switch (purpose.trim().toUpperCase(Locale.ROOT)) {
            case "GAMING" -> "PC chơi game";
            case "CREATIVE" -> "PC đồ họa, dựng phim";
            case "AI_DATA" -> "PC AI, dữ liệu";
            case "OFFICE" -> "PC văn phòng";
            case "DEV" -> "PC lập trình";
            case "GENERAL" -> "PC đa dụng";
            case "GAMING_HIGH" -> "PC chơi game cao cấp";
            case "GAMING_ULTRA" -> "PC chơi game đỉnh cao";
            case "CREATIVE_HIGH" -> "PC đồ họa, dựng phim cao cấp";
            case "CREATIVE_ULTRA" -> "PC đồ họa, dựng phim đỉnh cao";
            case "AI_DATA_HIGH" -> "PC AI, dữ liệu cao cấp";
            case "AI_DATA_ULTRA" -> "PC AI, dữ liệu đỉnh cao";
            case "DEV_HIGH" -> "PC lập trình cao cấp";
            default -> "PC " + purpose.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }

    /** Mô tả giúp AI chọn đúng purpose từ nhu cầu tự nhiên của khách. */
    private static String purposeDescription(String purpose) {
        return switch (purpose.trim().toUpperCase(Locale.ROOT)) {
            // Tiêu chí lấy theo bảng đo min_budget ở seed mục 13.2; AI dựa vào đây để chọn tier
            case "GAMING" -> "Chơi game phổ thông, eSports (LOL, Valorant, CS2, FO4), game AAA ở 1080p thiết lập thấp/trung bình, livestream nhẹ";
            case "GAMING_HIGH" -> "Chơi game AAA nặng ở 1080p Ultra hoặc 2K High, chiến game căng/max setting mà không nói rõ 4K; card đồ họa lớp RTX 5060 trở lên";
            case "GAMING_ULTRA" -> "Chơi game AAA ở 4K, hoặc 2K/1440p FPS rất cao (144–240fps), màn hình tần số quét cao; card đồ họa từ RTX 5070 Ti 16GB trở lên";
            case "CREATIVE" -> "Thiết kế đồ họa 2D, Photoshop, dựng phim 1080p cơ bản; cần CPU nhiều nhân, RAM lớn và card đồ họa";
            case "CREATIVE_HIGH" -> "Dựng phim 4K, render 3D, motion graphics chuyên nghiệp; CPU từ Ryzen 9 9900X và card đồ họa tầm trung cao";
            case "CREATIVE_ULTRA" -> "Dựng phim 6K–8K, render 3D nặng/cảnh lớn, làm studio chuyên nghiệp; CPU từ Ryzen 9 9950X và card đồ họa từ RTX 5070 Ti";
            case "AI_DATA" -> "Học AI/Data cơ bản, chạy model nhỏ, xử lý dữ liệu, Jupyter, học máy truyền thống; cần card đồ họa rời";
            case "AI_DATA_HIGH" -> "Huấn luyện/fine-tune model vừa, chạy LLM local cỡ vừa, deep learning căng; cần card đồ họa 16GB VRAM";
            case "AI_DATA_ULTRA" -> "Huấn luyện model lớn, LLM lớn, nghiên cứu deep learning chuyên sâu; cần card đồ họa 24GB VRAM chuyên dụng";
            case "OFFICE" -> "Văn phòng, học tập, Excel, họp online, xem phim; không cần card đồ họa rời";
            case "DEV" -> "Lập trình, chạy Docker, máy ảo, biên dịch code mức thông thường; cần CPU nhiều nhân và RAM lớn";
            case "DEV_HIGH" -> "Chạy nhiều máy ảo, Docker/Kubernetes, microservices nặng, build dự án lớn; CPU cao cấp và RAM DDR5 dung lượng lớn";
            case "GENERAL" -> "Dùng cho nhiều việc hoặc nhu cầu chưa rõ; có card đồ họa";
            default -> "Cấu hình tối ưu cho nhu cầu " + purpose.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }

    /** Mỗi slot lấy min(max_qty, 2) món đắt nhất; /requirements/analyze và /builds/auto dùng chung cận trên này. */
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
