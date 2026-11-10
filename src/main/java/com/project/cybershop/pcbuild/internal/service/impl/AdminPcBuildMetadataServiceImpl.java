package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.pcbuild.internal.dto.admin.*;
import com.project.cybershop.pcbuild.internal.entity.BudgetProfile;
import com.project.cybershop.pcbuild.internal.entity.BuildTemplateSlot;
import com.project.cybershop.pcbuild.internal.repository.BudgetProfileRepository;
import com.project.cybershop.pcbuild.internal.repository.BuildTemplateSlotRepository;
import com.project.cybershop.pcbuild.internal.service.AdminPcBuildMetadataService;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPcBuildMetadataServiceImpl implements AdminPcBuildMetadataService {

    private static final String GENERAL = "GENERAL";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private final BuildTemplateSlotRepository slotRepository;
    private final BudgetProfileRepository budgetProfileRepository;
    private final CatalogPublicApi catalogPublicApi;

    // =========================================================================
    // TEMPLATE SLOTS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<TemplateSlotDto> getAllSlots() {
        List<BuildTemplateSlot> rawSlots = slotRepository.findAllByOrderByStepOrderAsc();
        Map<Integer, CategoryResponse> categoryMap = getCategoryMap();
        Map<Integer, String> specNameMap = getSpecNameMap();

        List<TemplateSlotDto> dtoList = new ArrayList<>();
        for (BuildTemplateSlot slot : rawSlots) {
            CategoryResponse cat = categoryMap.get(slot.getCategoryId());
            CategoryResponse reqCat = slot.getRequiredIfCategoryId() != null
                    ? categoryMap.get(slot.getRequiredIfCategoryId()) : null;

            dtoList.add(TemplateSlotDto.builder()
                    .id(slot.getId())
                    .categoryId(slot.getCategoryId())
                    .categoryName(cat != null ? cat.name() : "Danh mục #" + slot.getCategoryId())
                    .categorySlug(cat != null ? cat.slug() : "slot_" + slot.getId())
                    .stepOrder(slot.getStepOrder())
                    .required(Boolean.TRUE.equals(slot.getRequired()))
                    .minQty(slot.getMinQty())
                    .maxQty(slot.getMaxQty())
                    .requiredIfCategoryId(slot.getRequiredIfCategoryId())
                    .requiredIfCategoryName(reqCat != null ? reqCat.name() : null)
                    .requiredIfSpecId(slot.getRequiredIfSpecId())
                    .requiredIfSpecName(slot.getRequiredIfSpecId() != null
                            ? specNameMap.getOrDefault(slot.getRequiredIfSpecId(), "Thông số #" + slot.getRequiredIfSpecId())
                            : null)
                    .requiredIfValue(slot.getRequiredIfValue())
                    .build());
        }
        return dtoList;
    }

    @Override
    @Transactional
    public TemplateSlotDto createSlot(TemplateSlotRequest request) {
        validateSlotRequest(request);

        if (slotRepository.existsByCategoryId(request.getCategoryId())) {
            throw badRequest("Danh mục này đã được gán cho một slot khác trong hệ thống.");
        }
        if (slotRepository.existsByStepOrder(request.getStepOrder())) {
            throw badRequest("Thứ tự bước (step_order) " + request.getStepOrder() + " đã tồn tại.");
        }

        BuildTemplateSlot slot = new BuildTemplateSlot();
        slot.setCategoryId(request.getCategoryId());
        slot.setStepOrder(request.getStepOrder());
        slot.setRequired(request.isRequired());
        slot.setMinQty(request.getMinQty());
        slot.setMaxQty(request.getMaxQty());
        slot.setRequiredIfCategoryId(request.getRequiredIfCategoryId());
        slot.setRequiredIfSpecId(request.getRequiredIfSpecId());
        slot.setRequiredIfValue(normalizeValue(request.getRequiredIfValue()));

        BuildTemplateSlot savedSlot = slotRepository.save(slot);

        // Tự động thêm dòng budget_profiles cho slot mới ở tất cả các purpose hiện có (với share_pct = 0%)
        // để không làm vỡ logic solver của AI PC Build
        List<BudgetProfile> existingProfiles = budgetProfileRepository.findAllByOrderByIdAsc();
        Map<String, BigDecimal> purposeMinBudgets = new HashMap<>();
        for (BudgetProfile bp : existingProfiles) {
            purposeMinBudgets.putIfAbsent(bp.getPurpose(), bp.getMinBudget());
        }

        for (Map.Entry<String, BigDecimal> entry : purposeMinBudgets.entrySet()) {
            BudgetProfile newBp = new BudgetProfile();
            newBp.setPurpose(entry.getKey());
            newBp.setSlot(savedSlot);
            newBp.setSharePct(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            newBp.setMinBudget(entry.getValue());
            budgetProfileRepository.save(newBp);
        }

        return toSlotDto(savedSlot);
    }

    @Override
    @Transactional
    public TemplateSlotDto updateSlot(Integer id, TemplateSlotRequest request) {
        validateSlotRequest(request);

        BuildTemplateSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy slot ID: " + id));

        // Kiểm tra trùng categoryId với slot khác
        slotRepository.findByCategoryId(request.getCategoryId()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw badRequest("Danh mục này đã được gán cho một slot khác.");
            }
        });

        // Kiểm tra trùng stepOrder với slot khác
        slotRepository.findByStepOrder(request.getStepOrder()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw badRequest("Thứ tự bước " + request.getStepOrder() + " đã tồn tại.");
            }
        });

        slot.setCategoryId(request.getCategoryId());
        slot.setStepOrder(request.getStepOrder());
        slot.setRequired(request.isRequired());
        slot.setMinQty(request.getMinQty());
        slot.setMaxQty(request.getMaxQty());
        slot.setRequiredIfCategoryId(request.getRequiredIfCategoryId());
        slot.setRequiredIfSpecId(request.getRequiredIfSpecId());
        slot.setRequiredIfValue(normalizeValue(request.getRequiredIfValue()));

        BuildTemplateSlot updatedSlot = slotRepository.save(slot);
        return toSlotDto(updatedSlot);
    }

    @Override
    @Transactional
    public void deleteSlot(Integer id) {
        BuildTemplateSlot slot = slotRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy slot ID: " + id));

        // Kiểm tra nếu có slot khác phụ thuộc điều kiện vào categoryId của slot này
        if (slotRepository.existsByRequiredIfCategoryId(slot.getCategoryId())) {
            throw badRequest("Không thể xóa slot này vì đang có slot khác phụ thuộc điều kiện kích hoạt vào danh mục của nó.");
        }

        // Xóa các dòng budget_profiles tham chiếu tới slot này
        budgetProfileRepository.deleteBySlotId(id);

        slotRepository.delete(slot);
    }

    @Override
    @Transactional
    public List<TemplateSlotDto> reorderSlots(List<Integer> orderedSlotIds) {
        if (orderedSlotIds == null || orderedSlotIds.isEmpty()) {
            throw badRequest("Danh sách ID slot không được để trống.");
        }
        List<BuildTemplateSlot> allSlots = slotRepository.findAll();
        Map<Integer, BuildTemplateSlot> slotMap = allSlots.stream()
                .collect(Collectors.toMap(BuildTemplateSlot::getId, s -> s));

        if (!slotMap.keySet().containsAll(orderedSlotIds) || slotMap.size() != orderedSlotIds.size()) {
            throw badRequest("Danh sách ID slot không khớp với toàn bộ các slot trong hệ thống.");
        }

        // Bước 1: Đổi stepOrder sang giá trị tạm thời tránh vi phạm uk_build_slot_step UNIQUE constraint
        int tempOffset = 10000;
        for (int i = 0; i < orderedSlotIds.size(); i++) {
            BuildTemplateSlot slot = slotMap.get(orderedSlotIds.get(i));
            slot.setStepOrder(tempOffset + i);
        }
        slotRepository.saveAllAndFlush(allSlots);

        // Bước 2: Đổi stepOrder chuẩn từ 1 đến N
        for (int i = 0; i < orderedSlotIds.size(); i++) {
            BuildTemplateSlot slot = slotMap.get(orderedSlotIds.get(i));
            slot.setStepOrder(i + 1);
        }
        slotRepository.saveAllAndFlush(allSlots);

        return getAllSlots();
    }

    @Override
    @Transactional(readOnly = true)
    public MetadataCategoryOptionDto getAvailableCategoryOptions() {
        List<CategoryResponse> allCategories = catalogPublicApi.getAllCategories();
        Set<Integer> assignedCategoryIds = slotRepository.findAll().stream()
                .map(BuildTemplateSlot::getCategoryId)
                .collect(Collectors.toSet());

        List<MetadataCategoryOptionDto.CategoryItem> categoryItems = allCategories.stream()
                .map(cat -> MetadataCategoryOptionDto.CategoryItem.builder()
                        .id(cat.id())
                        .name(cat.name())
                        .slug(cat.slug())
                        .alreadyHasSlot(assignedCategoryIds.contains(cat.id()))
                        .build())
                .toList();

        List<CategorySpecDto> specCatalog = catalogPublicApi.getSpecificationCatalog();
        List<MetadataCategoryOptionDto.SpecOptionItem> specOptionItems = new ArrayList<>();
        for (CategorySpecDto spec : specCatalog) {
            if (spec.getOptions() != null) {
                for (CategorySpecDto.Option opt : spec.getOptions()) {
                    specOptionItems.add(MetadataCategoryOptionDto.SpecOptionItem.builder()
                            .categoryId(spec.getCategoryId())
                            .specId(spec.getSpecificationId())
                            .specName(spec.getSpecificationName())
                            .code(opt.getCode())
                            .displayValue(opt.getDisplayValue())
                            .build());
                }
            }
        }

        return MetadataCategoryOptionDto.builder()
                .categories(categoryItems)
                .specOptions(specOptionItems)
                .build();
    }

    // =========================================================================
    // BUDGET PROFILES
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<BudgetProfilePurposeDto> getAllBudgetProfilesGrouped() {
        List<BuildTemplateSlot> slots = slotRepository.findAllByOrderByStepOrderAsc();
        Map<Integer, CategoryResponse> categoryMap = getCategoryMap();
        List<BudgetProfile> rawProfiles = budgetProfileRepository.findAllByOrderByIdAsc();

        Map<String, List<BudgetProfile>> profilesByPurpose = new LinkedHashMap<>();
        for (BudgetProfile bp : rawProfiles) {
            profilesByPurpose.computeIfAbsent(bp.getPurpose(), k -> new ArrayList<>()).add(bp);
        }

        List<BudgetProfilePurposeDto> result = new ArrayList<>();
        for (Map.Entry<String, List<BudgetProfile>> entry : profilesByPurpose.entrySet()) {
            String purpose = entry.getKey();
            List<BudgetProfile> purposeList = entry.getValue();

            Map<Integer, BudgetProfile> profileBySlotId = new HashMap<>();
            BigDecimal minBudget = BigDecimal.ZERO;
            for (BudgetProfile bp : purposeList) {
                if (bp.getSlot() != null && bp.getSlot().getId() != null) {
                    profileBySlotId.put(bp.getSlot().getId(), bp);
                }
                if (bp.getMinBudget() != null && bp.getMinBudget().compareTo(minBudget) > 0) {
                    minBudget = bp.getMinBudget();
                }
            }

            BigDecimal totalShare = BigDecimal.ZERO;
            List<BudgetProfilePurposeDto.SlotShareItem> slotShares = new ArrayList<>();
            for (BuildTemplateSlot slot : slots) {
                BudgetProfile bp = profileBySlotId.get(slot.getId());
                BigDecimal share = bp != null && bp.getSharePct() != null ? bp.getSharePct() : BigDecimal.ZERO;
                totalShare = totalShare.add(share);

                CategoryResponse cat = categoryMap.get(slot.getCategoryId());
                slotShares.add(BudgetProfilePurposeDto.SlotShareItem.builder()
                        .profileId(bp != null ? bp.getId() : null)
                        .slotId(slot.getId())
                        .categoryId(slot.getCategoryId())
                        .categoryName(cat != null ? cat.name() : "Slot #" + slot.getId())
                        .stepOrder(slot.getStepOrder())
                        .sharePct(share)
                        .build());
            }

            result.add(BudgetProfilePurposeDto.builder()
                    .purpose(purpose)
                    .label(purposeLabel(purpose))
                    .description(purposeDescription(purpose))
                    .minBudget(minBudget)
                    .totalSharePct(totalShare)
                    .slots(slotShares)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional
    public BudgetProfilePurposeDto createPurpose(BudgetProfileCreateRequest request) {
        String purpose = request.getPurpose().trim().toUpperCase(Locale.ROOT);
        if (budgetProfileRepository.existsByPurpose(purpose)) {
            throw badRequest("Mục đích (purpose) '" + purpose + "' đã tồn tại.");
        }

        List<BuildTemplateSlot> slots = slotRepository.findAllByOrderByStepOrderAsc();
        if (slots.isEmpty()) {
            throw badRequest("Chưa có slot linh kiện nào trong hệ thống.");
        }

        Map<Integer, BigDecimal> shares = request.getSlotShares();
        validateShares(slots, shares);

        BigDecimal minBudget = request.getMinBudget();
        for (BuildTemplateSlot slot : slots) {
            BudgetProfile bp = new BudgetProfile();
            bp.setPurpose(purpose);
            bp.setSlot(slot);
            bp.setSharePct(shares.get(slot.getId()).setScale(2, RoundingMode.HALF_UP));
            bp.setMinBudget(minBudget);
            budgetProfileRepository.save(bp);
        }

        return getPurposeDto(purpose);
    }

    @Override
    @Transactional
    public BudgetProfilePurposeDto updatePurpose(String purposeKey, BudgetProfileUpdateRequest request) {
        String purpose = purposeKey.trim().toUpperCase(Locale.ROOT);
        if (!budgetProfileRepository.existsByPurpose(purpose)) {
            throw notFound("Không tìm thấy mục đích: " + purpose);
        }

        List<BuildTemplateSlot> slots = slotRepository.findAllByOrderByStepOrderAsc();
        Map<Integer, BigDecimal> shares = request.getSlotShares();
        validateShares(slots, shares);

        BigDecimal minBudget = request.getMinBudget();

        // Cập nhật hoặc lưu lại các dòng
        List<BudgetProfile> existing = budgetProfileRepository.findByPurposeOrderByIdAsc(purpose);
        Map<Integer, BudgetProfile> existingMap = new HashMap<>();
        for (BudgetProfile bp : existing) {
            if (bp.getSlot() != null) {
                existingMap.put(bp.getSlot().getId(), bp);
            }
        }

        for (BuildTemplateSlot slot : slots) {
            BudgetProfile bp = existingMap.get(slot.getId());
            if (bp == null) {
                bp = new BudgetProfile();
                bp.setPurpose(purpose);
                bp.setSlot(slot);
            }
            bp.setMinBudget(minBudget);
            bp.setSharePct(shares.get(slot.getId()).setScale(2, RoundingMode.HALF_UP));
            budgetProfileRepository.save(bp);
        }

        return getPurposeDto(purpose);
    }

    @Override
    @Transactional
    public void deletePurpose(String purposeKey) {
        String purpose = purposeKey.trim().toUpperCase(Locale.ROOT);
        if (GENERAL.equalsIgnoreCase(purpose)) {
            throw badRequest("Không thể xóa mục đích mặc định bắt buộc GENERAL.");
        }
        if (!budgetProfileRepository.existsByPurpose(purpose)) {
            throw notFound("Không tìm thấy mục đích: " + purpose);
        }
        budgetProfileRepository.deleteByPurpose(purpose);
    }

    // =========================================================================
    // HELPER & VALIDATION METHODS
    // =========================================================================

    private void validateSlotRequest(TemplateSlotRequest req) {
        // ck_build_slot_qty_range: min_qty >= 0 AND max_qty >= 1 AND max_qty >= min_qty
        if (req.getMinQty() == null || req.getMinQty() < 0) {
            throw badRequest("Số lượng tối thiểu (min_qty) phải >= 0.");
        }
        if (req.getMaxQty() == null || req.getMaxQty() < 1) {
            throw badRequest("Số lượng tối đa (max_qty) phải >= 1.");
        }
        if (req.getMaxQty() < req.getMinQty()) {
            throw badRequest("Số lượng tối đa (max_qty) phải >= số lượng tối thiểu (min_qty).");
        }

        // ck_build_slot_required_min:
        // (is_required AND min_qty >= 1) OR (NOT is_required AND min_qty = 0)
        if (req.isRequired() && req.getMinQty() < 1) {
            throw badRequest("Linh kiện bắt buộc phải có số lượng tối thiểu (min_qty) >= 1.");
        }
        if (!req.isRequired() && req.getMinQty() != 0) {
            throw badRequest("Linh kiện tùy chọn phải có số lượng tối thiểu (min_qty) = 0.");
        }

        // Điều kiện phụ thuộc
        boolean hasReqCat = req.getRequiredIfCategoryId() != null;
        boolean hasReqSpec = req.getRequiredIfSpecId() != null;
        boolean hasReqVal = req.getRequiredIfValue() != null && !req.getRequiredIfValue().isBlank();

        // ck_build_slot_required_if_complete: hoặc cả 3 cùng null, hoặc cả 3 cùng có giá trị
        boolean allNull = !hasReqCat && !hasReqSpec && !hasReqVal;
        boolean allSet = hasReqCat && hasReqSpec && hasReqVal;
        if (!allNull && !allSet) {
            throw badRequest("Điều kiện phụ thuộc yêu cầu điền đầy đủ cả 3 thông tin: Danh mục, Thông số và Giá trị kích hoạt.");
        }

        if (allSet) {
            // ck_build_slot_required_if_optional: chỉ được cấu hình khi is_required = false
            if (req.isRequired()) {
                throw badRequest("Chỉ được thiết lập điều kiện kích hoạt khi linh kiện là tùy chọn (is_required = false).");
            }
            // ck_build_slot_required_if_other: không thể tự phụ thuộc chính danh mục của mình
            if (Objects.equals(req.getRequiredIfCategoryId(), req.getCategoryId())) {
                throw badRequest("Linh kiện không thể tự phụ thuộc điều kiện vào chính danh mục của nó.");
            }
        }
    }

    private void validateShares(List<BuildTemplateSlot> slots, Map<Integer, BigDecimal> shares) {
        if (shares == null) {
            throw badRequest("Danh sách tỷ lệ ngân sách không được rỗng.");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BuildTemplateSlot slot : slots) {
            BigDecimal share = shares.get(slot.getId());
            if (share == null) {
                throw badRequest("Thiếu tỷ lệ phần trăm phân bổ cho slot ID: " + slot.getId());
            }
            if (share.compareTo(BigDecimal.ZERO) < 0 || share.compareTo(ONE_HUNDRED) > 0) {
                throw badRequest("Tỷ lệ phân bổ từng slot phải từ 0% đến 100%.");
            }
            total = total.add(share);
        }

        if (total.compareTo(new BigDecimal("100")) != 0 && total.compareTo(ONE_HUNDRED) != 0) {
            throw badRequest("Tổng tỷ lệ phần trăm phân bổ của tất cả các slot phải đúng bằng 100.00%. Hiện tại: " + total + "%.");
        }
    }

    private TemplateSlotDto toSlotDto(BuildTemplateSlot slot) {
        Map<Integer, CategoryResponse> categoryMap = getCategoryMap();
        Map<Integer, String> specNameMap = getSpecNameMap();

        CategoryResponse cat = categoryMap.get(slot.getCategoryId());
        CategoryResponse reqCat = slot.getRequiredIfCategoryId() != null
                ? categoryMap.get(slot.getRequiredIfCategoryId()) : null;

        return TemplateSlotDto.builder()
                .id(slot.getId())
                .categoryId(slot.getCategoryId())
                .categoryName(cat != null ? cat.name() : "Danh mục #" + slot.getCategoryId())
                .categorySlug(cat != null ? cat.slug() : "slot_" + slot.getId())
                .stepOrder(slot.getStepOrder())
                .required(Boolean.TRUE.equals(slot.getRequired()))
                .minQty(slot.getMinQty())
                .maxQty(slot.getMaxQty())
                .requiredIfCategoryId(slot.getRequiredIfCategoryId())
                .requiredIfCategoryName(reqCat != null ? reqCat.name() : null)
                .requiredIfSpecId(slot.getRequiredIfSpecId())
                .requiredIfSpecName(slot.getRequiredIfSpecId() != null
                        ? specNameMap.getOrDefault(slot.getRequiredIfSpecId(), "Thông số #" + slot.getRequiredIfSpecId())
                        : null)
                .requiredIfValue(slot.getRequiredIfValue())
                .build();
    }

    private BudgetProfilePurposeDto getPurposeDto(String purpose) {
        return getAllBudgetProfilesGrouped().stream()
                .filter(p -> p.getPurpose().equalsIgnoreCase(purpose))
                .findFirst()
                .orElse(null);
    }

    private Map<Integer, CategoryResponse> getCategoryMap() {
        Map<Integer, CategoryResponse> map = new HashMap<>();
        for (CategoryResponse cat : catalogPublicApi.getAllCategories()) {
            if (cat != null && cat.id() != null) {
                map.put(cat.id(), cat);
            }
        }
        return map;
    }

    private Map<Integer, String> getSpecNameMap() {
        Map<Integer, String> map = new HashMap<>();
        for (CategorySpecDto spec : catalogPublicApi.getSpecificationCatalog()) {
            if (spec != null && spec.getSpecificationId() != null) {
                map.put(spec.getSpecificationId(), spec.getSpecificationName());
            }
        }
        return map;
    }

    private String normalizeValue(String val) {
        return (val != null && !val.isBlank()) ? val.trim() : null;
    }

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

    private static String purposeDescription(String purpose) {
        return switch (purpose.trim().toUpperCase(Locale.ROOT)) {
            case "GAMING" -> "Chơi game phổ thông, eSports (LOL, Valorant, CS2, FO4), game AAA ở 1080p";
            case "GAMING_HIGH" -> "Chơi game AAA nặng ở 1080p Ultra hoặc 2K High; card đồ họa RTX 5060 trở lên";
            case "GAMING_ULTRA" -> "Chơi game AAA ở 4K, hoặc 2K/1440p FPS rất cao; card đồ họa RTX 5070 Ti trở lên";
            case "CREATIVE" -> "Thiết kế đồ họa 2D, Photoshop, dựng phim 1080p cơ bản";
            case "CREATIVE_HIGH" -> "Dựng phim 4K, render 3D chuyên nghiệp; CPU Ryzen 9 và GPU tầm trung cao";
            case "CREATIVE_ULTRA" -> "Dựng phim 6K–8K, render 3D cảnh lớn; CPU Ryzen 9 9950X và GPU RTX 5070 Ti";
            case "AI_DATA" -> "Học AI/Data cơ bản, chạy model nhỏ, xử lý dữ liệu, học máy truyền thống";
            case "AI_DATA_HIGH" -> "Huấn luyện model vừa, deep learning; card đồ họa 16GB VRAM";
            case "AI_DATA_ULTRA" -> "Huấn luyện model lớn, nghiên cứu deep learning chuyên sâu; card đồ họa 24GB VRAM";
            case "OFFICE" -> "Văn phòng, học tập, Excel, họp online; không cần card đồ họa rời";
            case "DEV" -> "Lập trình, Docker, máy ảo, biên dịch code mức thông thường";
            case "DEV_HIGH" -> "Chạy nhiều Docker/Kubernetes, microservices, biên dịch dự án lớn";
            case "GENERAL" -> "Dùng cho nhiều việc hoặc nhu cầu chưa rõ; có card đồ họa";
            default -> "Cấu hình tối ưu cho nhu cầu " + purpose.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }

    private static PcBuildException badRequest(String message) {
        return new PcBuildException("METADATA_BAD_REQUEST", HttpStatus.BAD_REQUEST, message);
    }

    private static PcBuildException notFound(String message) {
        return new PcBuildException("METADATA_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}

