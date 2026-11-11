package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.AutoBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.BuildItemRequest;
import com.project.cybershop.pcbuild.api.dto.request.ReplacementRequest;
import com.project.cybershop.pcbuild.api.dto.request.SaveBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.UpdateSavedBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BuildItemResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.api.dto.response.BuildViolationResponse;
import com.project.cybershop.pcbuild.api.dto.response.ReplacementOptionResponse;
import com.project.cybershop.pcbuild.api.dto.response.SaveBuildResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedBuildResponse;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.entity.PcCustomItem;
import com.project.cybershop.pcbuild.internal.model.BuildCheck;
import com.project.cybershop.pcbuild.internal.model.BuildContext;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.PurposeMeta;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.SlotMeta;
import com.project.cybershop.pcbuild.internal.model.BuildPlan;
import com.project.cybershop.pcbuild.internal.model.RuleViolation;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.BudgetAllocationService;
import com.project.cybershop.pcbuild.internal.service.BuildExplainer;
import com.project.cybershop.pcbuild.internal.service.BuildMetadataService;
import com.project.cybershop.pcbuild.internal.service.BuildSolver;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.service.PcBuildService;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Điều phối bước 3 (kiểm lại) → 4 → 5 → 6 → 8 và các thao tác của bước 9. Chỉ save có @Transactional (một lần ghi
 * pc_customs + pc_custom_items); autoBuild không giữ transaction khi chờ AI ở bước 8.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PcBuildServiceImpl implements PcBuildService {

    private final BuildMetadataService buildMetadataService;
    private final BudgetAllocationService budgetAllocationService;
    private final BuildSolver buildSolver;
    private final CompatibilityEngine compatibilityEngine;
    private final CompatibilityRuleRepository compatibilityRuleRepository;
    private final CatalogPublicApi catalogPublicApi;
    private final BuildExplainer buildExplainer;
    private final PcCustomRepository pcCustomRepository;

    @Override
    public BuildResultResponse autoBuild(AutoBuildRequest request, String clientKey) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        long budget = request.budget();
        String purposeCode = request.purpose().trim().toUpperCase(Locale.ROOT);

        List<ComponentCandidateDTO> candidates = catalogPublicApi.getActiveCandidatesForBuild();
        BuildMetadata metadata = buildMetadataService.loadMetadata(candidates);

        // Bước 3 (kiểm lại): purpose và budget do frontend gửi nên không tin kết quả /requirements/analyze
        PurposeMeta purpose = metadata.getPurposes().get(purposeCode);
        if (purpose == null) {
            throw new PcBuildException("PURPOSE_INVALID", HttpStatus.BAD_REQUEST,
                    "Mục đích " + request.purpose() + " không có trong cấu hình. Mục đích hợp lệ: " + metadata.purposeCodes(),
                    Map.of("validPurposes", List.copyOf(metadata.purposeCodes())), null);
        }
        if (budget > metadata.getMaxBuildPrice()) {
            throw new PcBuildException("BUDGET_TOO_HIGH", HttpStatus.BAD_REQUEST,
                    "Bộ PC cao cấp nhất Shop ráp được hiện có giá khoảng " + vnd(metadata.getMaxBuildPrice())
                            + ". Bạn vui lòng nhập ngân sách không vượt mức này ạ.",
                    Map.of("maxBuildPrice", metadata.getMaxBuildPrice()), null);
        }
        if (budget < purpose.getMinBudget()) {
            throw new PcBuildException("BUDGET_TOO_LOW", HttpStatus.BAD_REQUEST,
                    "Ngân sách tối thiểu cho mục đích " + purposeCode + " là " + vnd(purpose.getMinBudget()) + ".",
                    Map.of("purpose", purposeCode, "minBudget", purpose.getMinBudget()), null);
        }

        BuildPlan plan = budgetAllocationService.plan(purposeCode, budget, metadata);           // bước 4
        BuildContext context = new BuildContext(metadata, candidates, loadRules());
        List<BuildLine> lines = buildSolver.solve(budget, plan, context);                      // bước 5
        BuildCheck check = compatibilityEngine.checkBuild(budget, lines, context);            // bước 6
        if (check.status() != BuildStatus.VALID) {
            log.error("[PC_BUILD_AUTO] reqId={} solver ra bộ không VALID status={} violations={}", requestId,
                    check.status(), check.violations());
            throw new PcBuildException("BUILD_INVALID", HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi hệ thống khi ráp bộ PC, vui lòng thử lại sau.");
        }
        BuildExplainer.Explanation explanation = buildExplainer.explain(purpose.getLabel(), budget, lines,
                check.total(), clientKey);                                                    // bước 8
        log.info("[PC_BUILD_AUTO] reqId={} purpose={} budget={} total={} difference={} items={} explainSource={} "
                        + "explainReason={} message={} elapsed={}ms",
                requestId, purposeCode, budget, check.total(), check.difference(),
                lines.stream().map(line -> line.slot().getComponentCode() + ":" + line.part().getVariantId()
                        + "x" + line.quantity()).toList(), explanation.source(), explanation.reason(),
                explanation.message(), System.currentTimeMillis() - startTime);
        return toResponse(budget, purposeCode, lines, check, context.rules(), explanation.message());
    }

    @Override
    public BuildResultResponse validate(ValidateBuildRequest request) {
        String requestId = UUID.randomUUID().toString();
        long budget = request.budget();
        List<Integer> variantIds = variantIds(request.items());
        Map<Integer, ComponentCandidateDTO> parts = catalogPublicApi.getCandidateMapByIds(variantIds);
        requireAll(variantIds, parts);

        // Không cần cận trên ở đây nên không đọc kho: maxBuildPrice = 0 và không dùng tới
        BuildMetadata metadata = buildMetadataService.loadMetadata(List.of());
        List<BuildLine> lines = toLines(request.items(), parts, metadata);
        BuildContext context = new BuildContext(metadata, List.copyOf(parts.values()), loadRules());
        BuildCheck check = compatibilityEngine.checkBuild(budget, lines, context);              // bước 6
        log.info("[PC_BUILD_VALIDATE] reqId={} budget={} status={} total={} difference={} violations={}", requestId,
                budget, check.status(), check.total(), check.difference(), check.violations());
        // Không gọi lại AI (docx bước 8–9): message null, frontend giữ lời giải thích của lần tự build
        return toResponse(budget, null, lines, check, context.rules(), null);
    }

    @Override
    public List<ReplacementOptionResponse> replacements(ReplacementRequest request) {
        BuildMetadata metadata = buildMetadataService.loadMetadata(List.of());
        String code = request.category().trim().toUpperCase(Locale.ROOT);
        SlotMeta slot = metadata.getSlots().stream()
                .filter(candidateSlot -> candidateSlot.getComponentCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new PcBuildException("CATEGORY_INVALID", HttpStatus.BAD_REQUEST,
                        "Loại linh kiện " + request.category() + " không có trong khuôn bộ PC.",
                        Map.of("validCategories", metadata.getSlots().stream().map(SlotMeta::getComponentCode).toList()),
                        null));

        List<Integer> variantIds = variantIds(request.items());
        Map<Integer, ComponentCandidateDTO> parts = variantIds.isEmpty()
                ? Map.of()
                : catalogPublicApi.getCandidateMapByIds(variantIds);
        requireAll(variantIds, parts);
        List<BuildLine> lines = toLines(request.items(), parts, metadata);
        List<BuildLine> others = lines.stream().filter(line -> !inSlot(line, slot)).toList();
        Set<Integer> currentIds = lines.stream().filter(line -> inSlot(line, slot))
                .map(line -> line.part().getVariantId()).collect(Collectors.toSet());
        int currentQty = lines.stream().filter(line -> inSlot(line, slot)).mapToInt(BuildLine::quantity).findFirst().orElse(1);
        int minQty = Math.max(1, slot.getMinQty() != null ? slot.getMinQty() : 1);

        List<CompatibilityRule> rules = loadRules();
        // Lỗi đã có sẵn giữa các món còn lại không do món thay gây ra, nên không dùng để loại món thay
        Set<RuleViolation> existing = new HashSet<>(compatibilityEngine.checkRules(others, rules));
        List<ReplacementOptionResponse> options = new ArrayList<>();
        for (ComponentCandidateDTO part : catalogPublicApi.getActiveCandidatesForBuild()) {
            if (!Objects.equals(part.getCategoryId(), slot.getCategoryId()) || part.getPrice() == null) {
                continue;
            }
            int maxQty = compatibilityEngine.maxQuantity(slot, part, others, rules);
            if (maxQty < minQty) {
                continue;
            }
            int quantity = Math.max(minQty, Math.min(currentQty, maxQty));
            if (stockOf(part) < quantity) {
                continue;
            }
            List<BuildLine> withPart = new ArrayList<>(others);
            withPart.add(new BuildLine(slot, part, quantity));
            if (!fits(withPart, others, existing, rules)) {
                continue;
            }
            options.add(ReplacementOptionResponse.builder()
                    .id(part.getVariantId())
                    .name(part.getProductName())
                    .brand(part.getBrandName())
                    .price(part.getPrice().longValueExact())
                    .qty(quantity)
                    .maxQty(maxQty)
                    .current(currentIds.contains(part.getVariantId()))
                    .build());
        }
        // Giá cao trước, đúng giả định của docx mục 2 (giá cao hơn là chất lượng cao hơn)
        options.sort(Comparator.comparing(ReplacementOptionResponse::getPrice).reversed()
                .thenComparing(ReplacementOptionResponse::getId));
        log.info("[PC_BUILD_REPLACEMENTS] slot={} items={} options={}", code, variantIds.size(), options.size());
        return options;
    }

    @Override
    @Transactional
    public SaveBuildResponse save(SaveBuildRequest request, Integer userId) {
        String requestId = UUID.randomUUID().toString();
        long budget = request.budget();
        CheckedBuild checked = checkForSave(requestId, userId, budget, request.items());
        BuildCheck check = checked.check();
        boolean confirmed = Boolean.TRUE.equals(request.confirmOverBudget());
        if (check.status() == BuildStatus.OVER_BUDGET && !confirmed) {
            log.info("[PC_BUILD_SAVE] reqId={} user={} chờ khách xác nhận vượt ngân sách difference={}", requestId,
                    userId, check.difference());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("budget", budget);
            data.put("total", check.total());
            data.put("difference", check.difference());
            data.put("budgetWarning", budgetWarning(budget, check));
            throw new PcBuildException("OVER_BUDGET_NOT_CONFIRMED", HttpStatus.CONFLICT,
                    overBudgetQuestion(budget, check), data, null);
        }

        PcCustom custom = new PcCustom();
        custom.setUserId(userId);
        fillItems(custom, checked);
        PcCustom saved = pcCustomRepository.save(custom);
        // docx mục 7: log lựa chọn của khách ở popup (lưu được nghĩa là "Tiếp tục"; "Không" thì frontend không gọi API)
        log.info("[PC_BUILD_SAVE] reqId={} user={} buildId={} status={} confirmOverBudget={} total={} budget={}",
                requestId, userId, saved.getId(), check.status(), confirmed, check.total(), budget);
        return SaveBuildResponse.builder()
                .buildId(saved.getId())
                .status(check.status())
                .total(check.total())
                .difference(check.difference())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedBuildResponse> listSaved(Integer userId) {
        List<PcCustom> customs = pcCustomRepository.findByUserIdAndOrderIdIsNullOrderByIdDesc(userId);
        List<Integer> variantIds = customs.stream()
                .flatMap(custom -> custom.getItems().stream())
                .map(PcCustomItem::getProductVariantId)
                .distinct()
                .toList();
        // Không lọc tồn kho: món đã hết hàng vẫn hiện tên trong bộ đã lưu
        Map<Integer, ComponentCandidateDTO> parts = catalogPublicApi.getCandidateMapByIds(variantIds);

        List<SavedBuildResponse> builds = new ArrayList<>();
        for (PcCustom custom : customs) {
            List<BuildItemResponse> items = custom.getItems().stream()
                    .sorted(Comparator.comparing(PcCustomItem::getId))   // id tăng dần = thứ tự step_order lúc lưu
                    .map(item -> {
                        ComponentCandidateDTO part = parts.get(item.getProductVariantId());
                        return BuildItemResponse.builder()
                                .category(item.getComponentRole())
                                .id(item.getProductVariantId())
                                .name(part != null ? part.getProductName() : "Linh kiện #" + item.getProductVariantId())
                                .qty(item.getQuantity())
                                .price(item.getUnitPrice().longValue())
                                .build();
                    })
                    .toList();
            builds.add(SavedBuildResponse.builder()
                    .buildId(custom.getId())
                    .total(custom.getTotalPrice() != null ? custom.getTotalPrice().longValue() : 0L)
                    .items(items)
                    .build());
        }
        return builds;
    }

    @Override
    @Transactional
    public SaveBuildResponse updateSaved(Integer buildId, UpdateSavedBuildRequest request, Integer userId) {
        String requestId = UUID.randomUUID().toString();
        // Bộ của tài khoản khác hoặc đã gắn đơn hàng trả 404 như không tồn tại, không lộ là có bộ đó
        PcCustom custom = pcCustomRepository.findById(buildId)
                .filter(found -> Objects.equals(found.getUserId(), userId) && found.getOrderId() == null)
                .orElseThrow(() -> new PcBuildException("SAVED_BUILD_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Không tìm thấy cấu hình đã lưu #" + buildId + "."));
        // pc_customs không lưu ngân sách: lấy trần Long nên kết quả chỉ còn VALID hoặc INCOMPATIBLE
        CheckedBuild checked = checkForSave(requestId, userId, Long.MAX_VALUE, request.items());
        custom.getItems().clear();                      // orphanRemoval xóa các dòng cũ
        fillItems(custom, checked);
        pcCustomRepository.save(custom);
        log.info("[PC_BUILD_UPDATE_SAVED] reqId={} user={} buildId={} total={} items={}", requestId, userId, buildId,
                checked.check().total(), checked.lines().size());
        return SaveBuildResponse.builder()
                .buildId(buildId)
                .status(checked.check().status())
                .total(checked.check().total())
                .difference(0L)                         // không có ngân sách để so
                .build();
    }

    // =========================================================================
    // DÙNG CHUNG CHO validate / replacements / save
    // =========================================================================

    private record CheckedBuild(List<BuildLine> lines, BuildCheck check) {
    }

    /** Bước 6 lần cuối trước khi ghi pc_customs: giá đọc mới từ DB, bộ có xung đột phần cứng thì 422. */
    private CheckedBuild checkForSave(String requestId, Integer userId, long budget, List<BuildItemRequest> items) {
        List<Integer> variantIds = variantIds(items);
        // Không nhận giá từ frontend (docx 3.5: unit_price là giá tại thời điểm lưu)
        Map<Integer, ComponentCandidateDTO> parts = catalogPublicApi.getCandidatesByIds(variantIds).stream()
                .collect(Collectors.toMap(ComponentCandidateDTO::getVariantId, Function.identity(), (first, second) -> first));
        requireAll(variantIds, parts);

        BuildMetadata metadata = buildMetadataService.loadMetadata(List.of());
        List<BuildLine> lines = toLines(items, parts, metadata);
        BuildCheck check = compatibilityEngine.checkBuild(budget, lines,
                new BuildContext(metadata, List.copyOf(parts.values()), loadRules()));
        if (check.status() == BuildStatus.INCOMPATIBLE) {
            log.info("[PC_BUILD_SAVE] reqId={} user={} từ chối INCOMPATIBLE violations={}", requestId, userId,
                    check.violations());
            throw new PcBuildException("BUILD_INCOMPATIBLE", HttpStatus.UNPROCESSABLE_CONTENT,
                    "Bộ PC có linh kiện không tương thích, chưa thể lưu.",
                    Map.of("violations", toViolationResponses(check.violations())), null);
        }
        return new CheckedBuild(lines, check);
    }

    private static void fillItems(PcCustom custom, CheckedBuild checked) {
        custom.setTotalPrice(BigDecimal.valueOf(checked.check().total()));
        for (BuildLine line : checked.lines()) {
            PcCustomItem item = new PcCustomItem();
            item.setProductVariantId(line.part().getVariantId());
            item.setComponentRole(line.slot().getComponentCode());
            item.setQuantity(line.quantity());
            item.setUnitPrice(line.part().getPrice());
            custom.addItem(item);
        }
    }

    private List<CompatibilityRule> loadRules() {
        return compatibilityRuleRepository.findAll();
    }

    private static List<Integer> variantIds(List<BuildItemRequest> items) {
        return items.stream().map(BuildItemRequest::variantId).distinct().toList();
    }

    private static void requireAll(List<Integer> variantIds, Map<Integer, ComponentCandidateDTO> parts) {
        List<Integer> missingIds = variantIds.stream().filter(id -> !parts.containsKey(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new PcBuildException("VARIANT_NOT_FOUND", HttpStatus.BAD_REQUEST,
                    "Không tìm thấy linh kiện có id " + missingIds + ".", Map.of("variantIds", missingIds), null);
        }
    }

    /** Ghép món khách gửi với slot của khuôn theo category, rồi xếp theo step_order. */
    private static List<BuildLine> toLines(List<BuildItemRequest> items, Map<Integer, ComponentCandidateDTO> parts,
                                           BuildMetadata metadata) {
        Map<Integer, SlotMeta> slotByCategory = new LinkedHashMap<>();
        for (SlotMeta slot : metadata.getSlots()) {
            slotByCategory.put(slot.getCategoryId(), slot);
        }
        List<BuildLine> lines = new ArrayList<>();
        for (BuildItemRequest item : items) {
            ComponentCandidateDTO part = parts.get(item.variantId());
            lines.add(new BuildLine(slotByCategory.get(part.getCategoryId()), part, item.qty()));
        }
        lines.sort(Comparator.comparingInt(line -> line.slot() != null ? line.slot().getStepOrder() : Integer.MAX_VALUE));
        return lines;
    }

    private static boolean inSlot(BuildLine line, SlotMeta slot) {
        return line.slot() != null && Objects.equals(line.slot().getSlotId(), slot.getSlotId());
    }

    /**
     * Món thay không gây lỗi mới: (1) không thêm vi phạm luật nào so với các món còn lại, (2) không làm số lượng
     * đang chọn của món khác vượt trần mới (ví dụ đổi main ít khe PCIe hơn khi đang có 2 GPU).
     */
    private boolean fits(List<BuildLine> withPart, List<BuildLine> others, Set<RuleViolation> existing,
                         List<CompatibilityRule> rules) {
        for (RuleViolation violation : compatibilityEngine.checkRules(withPart, rules)) {
            if (!existing.contains(violation)) {
                return false;
            }
        }
        for (BuildLine other : others) {
            if (other.slot() == null) {
                continue;
            }
            List<BuildLine> restWith = withPart.stream().filter(line -> line != other).toList();
            List<BuildLine> restWithout = others.stream().filter(line -> line != other).toList();
            int maxWith = compatibilityEngine.maxQuantity(other.slot(), other.part(), restWith, rules);
            int maxWithout = compatibilityEngine.maxQuantity(other.slot(), other.part(), restWithout, rules);
            if (other.quantity() > maxWith && other.quantity() <= maxWithout) {
                return false;
            }
        }
        return true;
    }

    private static int stockOf(ComponentCandidateDTO part) {
        return part.getStockQuantity() != null ? part.getStockQuantity() : 0;
    }

    private BuildResultResponse toResponse(long budget, String purpose, List<BuildLine> lines, BuildCheck check,
                                           List<CompatibilityRule> rules, String message) {
        List<BuildItemResponse> items = new ArrayList<>();
        for (BuildLine line : lines) {
            List<BuildLine> others = lines.stream().filter(other -> other != line).toList();
            int maxQty = line.slot() != null
                    ? compatibilityEngine.maxQuantity(line.slot(), line.part(), others, rules)
                    : line.quantity();
            items.add(BuildItemResponse.builder()
                    .category(line.slot() != null
                            ? line.slot().getComponentCode()
                            : Objects.toString(line.part().getCategorySlug(), "OTHER").toUpperCase(Locale.ROOT))
                    .id(line.part().getVariantId())
                    .name(line.displayName())
                    .qty(line.quantity())
                    .maxQty(maxQty)
                    .price(line.unitPrice())
                    .build());
        }
        return BuildResultResponse.builder()
                .status(check.status())
                .budget(budget)
                .purpose(purpose)
                .total(check.total())
                .difference(check.difference())
                .items(items)
                .violations(toViolationResponses(check.violations()))
                .budgetWarning(check.status() == BuildStatus.OVER_BUDGET ? budgetWarning(budget, check) : null)
                .message(message)
                .build();
    }

    private static List<BuildViolationResponse> toViolationResponses(List<RuleViolation> violations) {
        return violations.stream()
                .map(violation -> BuildViolationResponse.builder()
                        .rule(violation.rule())
                        .message(violation.message())
                        .build())
                .toList();
    }

    /** Câu cố định của bước 7 cho dòng cảnh báo đỏ (docx: "Vượt ngân sách 300.000đ (tổng 20.300.000đ / ngân sách 20.000.000đ)"). */
    static String budgetWarning(long budget, BuildCheck check) {
        return "Vượt ngân sách " + vnd(check.difference()) + " (tổng " + vnd(check.total()) + " / ngân sách "
                + vnd(budget) + ")";
    }

    /** Câu cố định của popup khi bấm Lưu (docx bước 7). */
    static String overBudgetQuestion(long budget, BuildCheck check) {
        return "Bộ PC hiện tại đã vượt quá ngân sách " + vnd(check.difference()) + " (tổng " + vnd(check.total())
                + ", ngân sách " + vnd(budget) + "). Bạn có muốn tiếp tục?";
    }

    /** 30000000 → "30.000.000đ". */
    static String vnd(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + "đ";
    }
}
