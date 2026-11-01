package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.ReplacementQueryRequest;
import com.project.cybershop.pcbuild.api.dto.request.SavePcBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BudgetFitResponse;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildDetailResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildSummaryResponse;
import com.project.cybershop.pcbuild.api.dto.response.SelectedPartResponse;
import com.project.cybershop.pcbuild.api.dto.response.ValidateBuildResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.entity.PcCustomItem;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.EngineReport;
import com.project.cybershop.pcbuild.internal.model.EvaluationContext;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.service.PcCustomService;
import com.project.cybershop.pcbuild.internal.service.ProfileValidator;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PcCustomServiceImpl implements PcCustomService {

    private static final BigDecimal BUDGET_WINDOW_MIN = new BigDecimal("0.90"); // [ĐÃ CHỐT] cận dưới cửa sổ ngân sách
    private static final int DEFAULT_REPLACEMENT_LIMIT = 30;

    private final PcCustomRepository pcCustomRepository;
    private final CatalogPublicApi catalogPublicApi;
    private final CandidateRepositoryFacade candidateRepositoryFacade;
    private final CompatibilityEngine compatibilityEngine;
    private final ProfileValidator profileValidator;

    // Hồ sơ client gửi là dữ liệu không tin cậy: luôn qua ProfileValidator (USER_EDITED) trước khi dùng
    private ValidatedProfile validatedProfile(RequirementProfileDto profile, CompatibilitySnapshot snapshot) {
        return profile == null ? null
                : profileValidator.validate(profile, snapshot.dictionary(), ProfileValidator.SOURCE_USER_EDITED, null);
    }

    @Override
    @Transactional
    public Integer saveNewBuild(SavePcBuildRequest request, Integer userId) {
        List<SavePcBuildRequest.SavePartItem> items = request.getItems() != null ? request.getItems() : List.of();
        // Giá đọc mới từ DB theo id (không qua cache), bỏ qua totalPrice/unitPrice client gửi (B17, DD 11.3)
        Map<Integer, ComponentCandidateDTO> catalog = items.isEmpty()
                ? Collections.emptyMap()
                : catalogPublicApi.getCandidatesByIds(items.stream()
                        .map(SavePcBuildRequest.SavePartItem::getVariantId).distinct().toList())
                        .stream().collect(Collectors.toMap(ComponentCandidateDTO::getVariantId, Function.identity()));

        List<BuildLine> lines = new ArrayList<>();
        for (SavePcBuildRequest.SavePartItem itemDto : items) {
            ComponentCandidateDTO candidate = catalog.get(itemDto.getVariantId());
            if (candidate == null || candidate.getPrice() == null) {
                throw new BusinessException("Linh kiện #" + itemDto.getVariantId() + " không tồn tại hoặc chưa có giá");
            }
            lines.add(new BuildLine(itemDto.getRole(), candidate, itemDto.getQuantity()));
        }

        // Luật và thông số bắt buộc lấy từ snapshot; không kiểm ngân sách khi lưu (DD 9.4)
        EngineReport report = compatibilityEngine.evaluate(lines,
                new EvaluationContext(null, EvaluationContext.Mode.USER, candidateRepositoryFacade.snapshot()));
        if (!report.compatible()) {
            throw new BuildRuleException("BUILD_INCOMPATIBLE", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cấu hình có linh kiện không tương thích, chưa thể lưu", Map.of("checks", report.checks()));
        }

        PcCustom custom = new PcCustom();
        custom.setUserId(userId);
        BigDecimal total = BigDecimal.ZERO;

        for (SavePcBuildRequest.SavePartItem itemDto : items) {
            ComponentCandidateDTO candidate = catalog.get(itemDto.getVariantId());
            PcCustomItem item = new PcCustomItem();
            item.setPcCustom(custom);
            item.setProductVariantId(itemDto.getVariantId());
            item.setComponentRole(itemDto.getRole());
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(candidate.getPrice());
            custom.getItems().add(item);
            total = total.add(candidate.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
        }
        custom.setTotalPrice(total);

        PcCustom saved = pcCustomRepository.save(custom);
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedPcBuildSummaryResponse> getMySavedBuilds(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<PcCustom> customs = pcCustomRepository.findByUserIdOrderByIdDesc(userId);
        if (customs == null || customs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> allVariantIds = customs.stream()
                .filter(c -> c.getItems() != null)
                .flatMap(c -> c.getItems().stream())
                .map(PcCustomItem::getProductVariantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, ComponentCandidateDTO> candidateMap = allVariantIds.isEmpty()
                ? Collections.emptyMap()
                : catalogPublicApi.getCandidateMapByIds(allVariantIds);

        return customs.stream().map(custom -> {
            String cpuName = null;
            String gpuName = null;
            String mainboardName = null;
            String ramSummary = null;

            if (custom.getItems() != null) {
                for (PcCustomItem item : custom.getItems()) {
                    String role = item.getComponentRole() != null ? item.getComponentRole().trim().toLowerCase() : "";
                    ComponentCandidateDTO candidate = candidateMap.get(item.getProductVariantId());
                    String pName = candidate != null ? candidate.getProductName() : ("Linh kiện #" + item.getProductVariantId());

                    switch (role) {
                        case "cpu" -> cpuName = pName;
                        case "gpu", "vga" -> gpuName = pName;
                        case "mainboard", "motherboard" -> mainboardName = pName;
                        case "ram" -> {
                            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                            ramSummary = pName + (qty > 1 ? " (x" + qty + ")" : "");
                        }
                        default -> {}
                    }
                }
            }

            String buildName = generateBuildName(custom.getId(), cpuName, gpuName);
            int itemCount = custom.getItems() != null ? custom.getItems().size() : 0;

            return SavedPcBuildSummaryResponse.builder()
                    .buildId(custom.getId())
                    .buildName(buildName)
                    .totalPrice(custom.getTotalPrice())
                    .orderId(custom.getOrderId())
                    .isOrdered(custom.getOrderId() != null)
                    .itemCount(itemCount)
                    .cpuName(cpuName)
                    .gpuName(gpuName)
                    .mainboardName(mainboardName)
                    .ramSummary(ramSummary)
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SavedPcBuildDetailResponse getSavedBuildDetail(Integer buildId, Integer userId) {
        PcCustom custom = pcCustomRepository.findById(buildId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình #" + buildId));

        if (!Objects.equals(custom.getUserId(), userId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập cấu hình này");
        }

        List<Integer> variantIds = custom.getItems() != null
                ? custom.getItems().stream().map(PcCustomItem::getProductVariantId).filter(Objects::nonNull).distinct().toList()
                : Collections.emptyList();

        Map<Integer, ComponentCandidateDTO> candidateMap = variantIds.isEmpty()
                ? Collections.emptyMap()
                : catalogPublicApi.getCandidateMapByIds(variantIds);

        String cpuName = null;
        String gpuName = null;
        List<SelectedPartResponse> items = new ArrayList<>();

        if (custom.getItems() != null) {
            for (PcCustomItem item : custom.getItems()) {
                String role = item.getComponentRole() != null ? item.getComponentRole().trim().toLowerCase() : "";
                ComponentCandidateDTO candidate = candidateMap.get(item.getProductVariantId());
                String productName = candidate != null ? candidate.getProductName() : ("Linh kiện #" + item.getProductVariantId());
                String variantName = candidate != null ? candidate.getVariantName() : null;
                String imageUrl = candidate != null ? candidate.getImageUrl() : null;
                Map<String, String> specs = candidate != null && candidate.getSpecs() != null ? candidate.getSpecs() : Collections.emptyMap();
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : (candidate != null && candidate.getPrice() != null ? candidate.getPrice() : BigDecimal.ZERO);
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));

                if ("cpu".equals(role)) {
                    cpuName = productName;
                } else if ("gpu".equals(role) || "vga".equals(role)) {
                    gpuName = productName;
                }

                items.add(SelectedPartResponse.builder()
                        .role(item.getComponentRole())
                        .variantId(item.getProductVariantId())
                        .productName(productName)
                        .variantName(variantName)
                        .imageUrl(imageUrl)
                        .unitPrice(unitPrice)
                        .quantity(qty)
                        .subtotal(subtotal)
                        .specs(specs)
                        .aiReason(null)
                        .build());
            }
        }

        String buildName = generateBuildName(custom.getId(), cpuName, gpuName);

        return SavedPcBuildDetailResponse.builder()
                .buildId(custom.getId())
                .buildName(buildName)
                .totalPrice(custom.getTotalPrice())
                .orderId(custom.getOrderId())
                .isOrdered(custom.getOrderId() != null)
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public void deleteSavedBuild(Integer buildId, Integer userId) {
        PcCustom custom = pcCustomRepository.findById(buildId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình #" + buildId));

        if (!Objects.equals(custom.getUserId(), userId)) {
            throw new AccessDeniedException("Bạn không có quyền xóa cấu hình này");
        }

        if (custom.getOrderId() != null) {
            throw new BusinessException("Không thể xóa cấu hình đã gắn liền với đơn hàng #" + custom.getOrderId());
        }

        pcCustomRepository.delete(custom);
    }

    private String generateBuildName(Integer buildId, String cpuName, String gpuName) {
        if (cpuName != null && !cpuName.isBlank()) {
            if (gpuName != null && !gpuName.isBlank()) {
                return "Bộ PC " + cpuName + " & " + gpuName;
            } else {
                return "Bộ PC " + cpuName + " (Đồ họa tích hợp)";
            }
        }
        return "Cấu hình PC Custom #" + buildId;
    }

    // Không @Transactional: chỉ đọc snapshot trong bộ nhớ, 0 câu SQL khi cache ấm (AC13)
    @Override
    public ValidateBuildResponse validateBuild(ValidateBuildRequest request) {
        CompatibilitySnapshot snapshot = candidateRepositoryFacade.snapshot();
        ResolvedBuild build = resolveBuild(request.getItems(), snapshot);
        EngineReport report = compatibilityEngine.evaluate(build.lines(),
                        new EvaluationContext(request.getBudget(), validatedProfile(request.getProfile(), snapshot),
                                EvaluationContext.Mode.USER, snapshot))
                .withExtraChecks(build.stockChecks());

        return ValidateBuildResponse.builder()
                .isCompatible(report.compatible())
                .hasWarnings(report.hasWarnings())
                .hasUnknown(report.hasUnknown())
                .totalPrice(report.total())
                .budgetFit(buildBudgetFit(report.total(), request.getBudget()))
                .checks(report.checks())
                .requiredPsuW(report.requiredPsuW())
                .capacity(report.capacity())
                .items(build.lines().stream().filter(Objects::nonNull).map(PcCustomServiceImpl::toSelectedPart).toList())
                .build();
    }

    @Override
    public List<CompatibleReplacementResponse> getCompatibleReplacements(ReplacementQueryRequest request) {
        String role = normalizeRole(request.getRole());
        CompatibilitySnapshot snapshot = candidateRepositoryFacade.snapshot(); // một lần cho mọi ứng viên
        List<ValidateBuildRequest.ItemPart> items = request.getItems() != null ? request.getItems() : List.of();
        List<BuildLine> currentLines = resolveBuild(items, snapshot).lines();
        int targetIndex = findReplacementIndex(items, role, request.getLineIndex());
        int quantity = targetIndex >= 0 && items.get(targetIndex).getQuantity() != null
                ? items.get(targetIndex).getQuantity() : 1;
        BigDecimal currentTotal = totalOf(currentLines);
        BigDecimal budget = request.getBudget();
        EvaluationContext ctx = new EvaluationContext(budget, validatedProfile(request.getProfile(), snapshot),
                EvaluationContext.Mode.USER, snapshot);
        Set<String> preExisting = preExistingIssues(currentLines, targetIndex, ctx);

        // DD 9.3: tương thích trước → không warning → |newTotal − B| tăng dần → giá tăng dần
        Comparator<CompatibleReplacementResponse> order = Comparator
                .comparing((CompatibleReplacementResponse response) -> !response.isCompatible())
                .thenComparing(response -> !response.getWarningChecks().isEmpty());
        if (budget != null) {
            order = order.thenComparing(response -> response.getNewTotal().subtract(budget).abs());
        }
        order = order.thenComparing(CompatibleReplacementResponse::getPrice, Comparator.nullsLast(Comparator.naturalOrder()));

        int limit = request.getLimit() != null ? request.getLimit() : DEFAULT_REPLACEMENT_LIMIT;
        return snapshot.candidates().stream()
                .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug()))
                .map(candidate -> {
                    List<BuildLine> lines = new ArrayList<>(currentLines);
                    BuildLine replacement = new BuildLine(role, candidate, quantity);
                    if (targetIndex >= 0) {
                        lines.set(targetIndex, replacement);
                    } else {
                        lines.add(replacement);
                    }
                    return toReplacement(candidate, compatibilityEngine.evaluate(lines, ctx), preExisting, currentTotal, budget);
                })
                .sorted(order)
                .limit(limit)
                .toList();
    }

    /** Dòng build đã tra giá và thông số (cùng thứ tự với items; null = biến thể không tồn tại) và các check STOCK. */
    private record ResolvedBuild(List<BuildLine> lines, List<CompatibilityCheckResponse> stockChecks) {
    }

    private ResolvedBuild resolveBuild(List<ValidateBuildRequest.ItemPart> items, CompatibilitySnapshot snapshot) {
        // Chỉ món ngoài snapshot (hết hàng, ngừng bán) mới phải tra DB; build bình thường không phát sinh SQL
        List<Integer> outOfStockIds = items.stream()
                .map(ValidateBuildRequest.ItemPart::getVariantId)
                .filter(id -> snapshot.candidate(id).isEmpty())
                .distinct()
                .toList();
        Map<Integer, ComponentCandidateDTO> outOfSnapshot = outOfStockIds.isEmpty()
                ? Collections.emptyMap()
                : catalogPublicApi.getCandidatesByIds(outOfStockIds).stream()
                        .collect(Collectors.toMap(ComponentCandidateDTO::getVariantId, Function.identity()));

        List<BuildLine> lines = new ArrayList<>();
        List<CompatibilityCheckResponse> stockChecks = new ArrayList<>();
        for (ValidateBuildRequest.ItemPart item : items) {
            int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
            Optional<ComponentCandidateDTO> inStock = snapshot.candidate(item.getVariantId());
            ComponentCandidateDTO part = inStock.orElse(outOfSnapshot.get(item.getVariantId()));
            if (part == null) {
                lines.add(null);
                stockChecks.add(stockFail("Linh kiện #" + item.getVariantId() + " không tồn tại"));
                continue;
            }
            lines.add(new BuildLine(item.getRole(), part, quantity));
            if (inStock.isEmpty()) {
                stockChecks.add(stockFail(part.getProductName() + " đã hết hàng hoặc ngừng kinh doanh"));
            } else if (part.getStockQuantity() != null && part.getStockQuantity() < quantity) {
                stockChecks.add(stockFail(part.getProductName() + " chỉ còn " + part.getStockQuantity() + " sản phẩm"));
            }
        }
        return new ResolvedBuild(lines, stockChecks);
    }

    private static CompatibilityCheckResponse stockFail(String detail) {
        return CompatibilityCheckResponse.of("STOCK", "Tình trạng hàng", "fail", detail);
    }

    private static int findReplacementIndex(List<ValidateBuildRequest.ItemPart> items, String role, Integer lineIndex) {
        if (lineIndex != null) {
            if (lineIndex >= items.size() || !role.equals(normalizeRole(items.get(lineIndex).getRole()))) {
                throw new BusinessException("lineIndex " + lineIndex + " không trỏ tới dòng " + role + " trong build");
            }
            return lineIndex;
        }
        for (int index = 0; index < items.size(); index++) {
            if (role.equals(normalizeRole(items.get(index).getRole()))) {
                return index;
            }
        }
        return -1; // build chưa có role này: thêm ứng viên làm dòng mới
    }

    /**
     * Vấn đề đã có khi build chưa có dòng đang thay (ví dụ CPU tray chưa có tản) không phải lỗi của ứng viên:
     * đánh giá build bỏ dòng đó một lần, ghi lại các check không pass để loại khỏi kết quả từng ứng viên.
     */
    private Set<String> preExistingIssues(List<BuildLine> currentLines, int targetIndex, EvaluationContext ctx) {
        List<BuildLine> withoutTarget = new ArrayList<>(currentLines);
        if (targetIndex >= 0) {
            withoutTarget.set(targetIndex, null);
        }
        return compatibilityEngine.evaluate(withoutTarget, ctx).checks().stream()
                .filter(check -> !"pass".equals(check.getStatus()))
                .map(PcCustomServiceImpl::issueSignature)
                .collect(Collectors.toSet());
    }

    private static String issueSignature(CompatibilityCheckResponse check) {
        return check.getKey() + "|" + check.getStatus() + "|" + check.getDetail();
    }

    private static CompatibleReplacementResponse toReplacement(ComponentCandidateDTO candidate, EngineReport report,
                                                               Set<String> preExisting, BigDecimal currentTotal,
                                                               BigDecimal budget) {
        // Bỏ CORE (build thiếu nhóm khác) và các vấn đề đã có trước khi thay
        List<CompatibilityCheckResponse> checks = report.checks().stream()
                .filter(check -> !"CORE".equals(check.getKey()))
                .filter(check -> "pass".equals(check.getStatus()) || !preExisting.contains(issueSignature(check)))
                .toList();
        List<String> failed = keysWithStatus(checks, "fail");
        List<String> warnings = keysWithStatus(checks, "warning");
        boolean hasUnknown = checks.stream().anyMatch(check -> "unknown".equals(check.getStatus()));
        String status = !failed.isEmpty() ? "fail" : !warnings.isEmpty() ? "warning" : hasUnknown ? "unknown" : "pass";
        String note = checks.stream()
                .filter(check -> "fail".equals(check.getStatus()))
                .map(CompatibilityCheckResponse::getDetail)
                .findFirst()
                .orElse(hasUnknown ? "Chưa xác minh" : "Tương thích");

        return CompatibleReplacementResponse.builder()
                .variantId(candidate.getVariantId())
                .productName(candidate.getProductName())
                .variantName(candidate.getVariantName())
                .price(candidate.getPrice())
                .imageUrl(candidate.getImageUrl())
                .stockQuantity(candidate.getStockQuantity())
                .specs(candidate.getSpecs())
                .isCompatible(failed.isEmpty())
                .compatibilityNote(note)
                .status(status)
                .failedChecks(failed)
                .warningChecks(warnings)
                .priceDelta(report.total().subtract(currentTotal))
                .newTotal(report.total())
                .newPercent(budget != null
                        ? report.total().multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP)
                        : null)
                .build();
    }

    private static List<String> keysWithStatus(List<CompatibilityCheckResponse> checks, String status) {
        return checks.stream()
                .filter(check -> status.equals(check.getStatus()))
                .map(CompatibilityCheckResponse::getKey)
                .distinct()
                .toList();
    }

    private static BudgetFitResponse buildBudgetFit(BigDecimal total, BigDecimal budget) {
        if (budget == null || budget.signum() <= 0) return null;
        BigDecimal ratio = total.divide(budget, 4, RoundingMode.HALF_UP);
        BigDecimal windowMin = budget.multiply(BUDGET_WINDOW_MIN).setScale(0, RoundingMode.CEILING);
        String status = total.compareTo(budget) > 0 ? "OVER"
                : total.compareTo(windowMin) >= 0 ? "IN_WINDOW" : "UNDER_WINDOW";
        return BudgetFitResponse.builder()
                .ratio(ratio)
                .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP))
                .status(status)
                .windowMin(windowMin)
                .windowMax(budget)
                .leftover(budget.subtract(total))
                .build();
    }

    private static SelectedPartResponse toSelectedPart(BuildLine line) {
        ComponentCandidateDTO part = line.part();
        BigDecimal unitPrice = part.getPrice() != null ? part.getPrice() : BigDecimal.ZERO;
        return SelectedPartResponse.builder()
                .role(line.role())
                .variantId(part.getVariantId())
                .productName(part.getProductName())
                .variantName(part.getVariantName())
                .imageUrl(part.getImageUrl())
                .unitPrice(unitPrice)
                .quantity(line.quantity())
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(line.quantity())))
                .specs(part.getSpecs())
                .build();
    }

    private static BigDecimal totalOf(List<BuildLine> lines) {
        return lines.stream()
                .filter(line -> line != null && line.part().getPrice() != null)
                .map(line -> line.part().getPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String normalizeRole(String role) {
        return role != null ? role.trim().toLowerCase(Locale.ROOT) : "";
    }
}
