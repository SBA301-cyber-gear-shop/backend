package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.AutoBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.BuildItemRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BuildItemResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.api.dto.response.BuildViolationResponse;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.model.BuildCheck;
import com.project.cybershop.pcbuild.internal.model.BuildContext;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.PurposeMeta;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.SlotMeta;
import com.project.cybershop.pcbuild.internal.model.BuildPlan;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import com.project.cybershop.pcbuild.internal.service.BudgetAllocationService;
import com.project.cybershop.pcbuild.internal.service.BuildMetadataService;
import com.project.cybershop.pcbuild.internal.service.BuildSolver;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.service.PcBuildService;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Điều phối bước 3 (kiểm lại) → 4 → 5 → 6. Không @Transactional: các lần đọc DB nằm trong transaction riêng
 * của catalog và BuildMetadataService, phần còn lại chạy trên bộ nhớ.
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

    @Override
    public BuildResultResponse autoBuild(AutoBuildRequest request) {
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
        log.info("[PC_BUILD_AUTO] reqId={} purpose={} budget={} total={} difference={} items={} elapsed={}ms",
                requestId, purposeCode, budget, check.total(), check.difference(),
                lines.stream().map(line -> line.slot().getComponentCode() + ":" + line.part().getVariantId()
                        + "x" + line.quantity()).toList(), System.currentTimeMillis() - startTime);
        return toResponse(budget, purposeCode, lines, check, context.rules());
    }

    @Override
    public BuildResultResponse validate(ValidateBuildRequest request) {
        String requestId = UUID.randomUUID().toString();
        long budget = request.budget();
        List<Integer> variantIds = request.items().stream().map(BuildItemRequest::variantId).distinct().toList();
        Map<Integer, ComponentCandidateDTO> parts = catalogPublicApi.getCandidateMapByIds(variantIds);
        List<Integer> missingIds = variantIds.stream().filter(id -> !parts.containsKey(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new PcBuildException("VARIANT_NOT_FOUND", HttpStatus.BAD_REQUEST,
                    "Không tìm thấy linh kiện có id " + missingIds + ".", Map.of("variantIds", missingIds), null);
        }

        // Không cần cận trên ở đây nên không đọc kho: maxBuildPrice = 0 và không dùng tới
        BuildMetadata metadata = buildMetadataService.loadMetadata(List.of());
        Map<Integer, SlotMeta> slotByCategory = new LinkedHashMap<>();
        for (SlotMeta slot : metadata.getSlots()) {
            slotByCategory.put(slot.getCategoryId(), slot);
        }
        List<BuildLine> lines = new ArrayList<>();
        for (BuildItemRequest item : request.items()) {
            ComponentCandidateDTO part = parts.get(item.variantId());
            lines.add(new BuildLine(slotByCategory.get(part.getCategoryId()), part, item.qty()));
        }
        lines.sort(Comparator.comparingInt(line -> line.slot() != null ? line.slot().getStepOrder() : Integer.MAX_VALUE));

        BuildContext context = new BuildContext(metadata, List.copyOf(parts.values()), loadRules());
        BuildCheck check = compatibilityEngine.checkBuild(budget, lines, context);              // bước 6
        log.info("[PC_BUILD_VALIDATE] reqId={} budget={} status={} total={} difference={} violations={}", requestId,
                budget, check.status(), check.total(), check.difference(), check.violations());
        return toResponse(budget, null, lines, check, context.rules());
    }

    private List<CompatibilityRule> loadRules() {
        return compatibilityRuleRepository.findAll();
    }

    private BuildResultResponse toResponse(long budget, String purpose, List<BuildLine> lines, BuildCheck check,
                                           List<CompatibilityRule> rules) {
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
                .violations(check.violations().stream()
                        .map(violation -> BuildViolationResponse.builder()
                                .rule(violation.rule())
                                .message(violation.message())
                                .build())
                        .toList())
                .budgetWarning(check.status() == BuildStatus.OVER_BUDGET ? budgetWarning(budget, check) : null)
                .message(null)
                .build();
    }

    /** Câu cố định của bước 7 cho dòng cảnh báo đỏ (docx: "Vượt ngân sách 300.000đ (tổng 20.300.000đ / ngân sách 20.000.000đ)"). */
    static String budgetWarning(long budget, BuildCheck check) {
        return "Vượt ngân sách " + vnd(check.difference()) + " (tổng " + vnd(check.total()) + " / ngân sách "
                + vnd(budget) + ")";
    }

    /** 30000000 → "30.000.000đ". */
    static String vnd(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + "đ";
    }
}
