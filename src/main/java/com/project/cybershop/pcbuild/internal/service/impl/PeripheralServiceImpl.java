package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.PeripheralQueryRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.PeripheralSuggestionResponse;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.PeripheralService;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import com.project.cybershop.pcbuild.internal.support.CandidatePoolBuilder;
import com.project.cybershop.pcbuild.internal.support.PeripheralSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeripheralServiceImpl implements PeripheralService {

    private static final Map<String, String> LABELS = Map.of(
            PeripheralSelector.MONITOR, "Màn hình",
            PeripheralSelector.KEYBOARD, "Bàn phím",
            PeripheralSelector.MOUSE, "Chuột");
    private static final String REASON_BUDGET = "Giá cân đối với cấu hình thùng máy";

    private static final int SCREEN_SIZE_INCH = 68;
    private static final int RESOLUTION = 69;
    private static final int PANEL_TYPE = 71;
    private static final int KEYBOARD_SWITCH = 72;
    private static final int KEYBOARD_LAYOUT = 73;
    private static final int CONNECTION_TYPE = 74;
    private static final int MOUSE_DPI = 75;
    private static final int MOUSE_WEIGHT_G = 76;

    private final CandidateRepositoryFacade candidateRepositoryFacade;
    private final CatalogPublicApi catalogPublicApi;

    @Override
    public PeripheralSuggestionResponse suggest(PeripheralQueryRequest request) {
        CompatibilitySnapshot snapshot = candidateRepositoryFacade.snapshot();
        BigDecimal pcTotal = pcTotal(request.getItems(), snapshot);
        PeripheralSelector.Selection selection = PeripheralSelector.select(snapshot.peripherals(), request.getBudget(), pcTotal,
                request.getArchetype());

        List<PeripheralSuggestionResponse.RoleEntry> roles = new ArrayList<>();
        for (String role : PeripheralSelector.ROLES) {
            ComponentCandidateDTO picked = selection.picks().get(role);
            roles.add(PeripheralSuggestionResponse.RoleEntry.builder()
                    .role(role)
                    .label(LABELS.get(role))
                    .picked(toItem(role, picked, snapshot, reasonOf(role, picked, request.getArchetype(), selection)))
                    .options(PeripheralSelector.optionsOf(snapshot.peripherals(), role).stream()
                            .map(option -> toItem(role, option, snapshot, null)).toList())
                    .build());
        }
        return PeripheralSuggestionResponse.builder()
                .pcTotal(pcTotal)
                .available(selection.available())
                .spendLimit(selection.spendLimit())
                .peripheralTotal(selection.peripheralTotal())
                .grandTotal(pcTotal.add(selection.peripheralTotal()))
                .roles(roles)
                .build();
    }

    // Giá lấy như resolveBuild của /validate: snapshot trước, món ngoài snapshot (hết hàng, ngừng bán) mới tra DB
    private BigDecimal pcTotal(List<ValidateBuildRequest.ItemPart> items, CompatibilitySnapshot snapshot) {
        List<Integer> outOfSnapshot = items.stream().map(ValidateBuildRequest.ItemPart::getVariantId)
                .filter(id -> snapshot.candidate(id).isEmpty()).distinct().toList();
        Map<Integer, ComponentCandidateDTO> fromDb = outOfSnapshot.isEmpty() ? Map.of()
                : catalogPublicApi.getCandidatesByIds(outOfSnapshot).stream()
                        .collect(Collectors.toMap(ComponentCandidateDTO::getVariantId, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        for (ValidateBuildRequest.ItemPart item : items) {
            ComponentCandidateDTO part = snapshot.candidate(item.getVariantId()).orElse(fromDb.get(item.getVariantId()));
            if (part == null || part.getPrice() == null || !isBuildPart(part)) {
                throw new BuildRuleException("BUILD_ITEM_NOT_FOUND", HttpStatus.UNPROCESSABLE_ENTITY,
                        "Linh kiện #" + item.getVariantId() + " không thuộc thùng máy hoặc không còn tồn tại",
                        Map.of("variantId", item.getVariantId()));
            }
            total = total.add(part.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

    private static boolean isBuildPart(ComponentCandidateDTO part) {
        return part.getCategorySlug() != null
                && CandidatePoolBuilder.BUILD_ROLES.contains(part.getCategorySlug().toLowerCase(Locale.ROOT));
    }

    private static String reasonOf(String role, ComponentCandidateDTO picked, String archetype, PeripheralSelector.Selection selection) {
        if (!PeripheralSelector.MONITOR.equals(role) || !"GAMING".equalsIgnoreCase(archetype)) return REASON_BUDGET;
        return selection.gamingFallback()
                ? "Ngân sách phụ kiện chưa đủ cho màn từ 144Hz, đây là lựa chọn tốt nhất trong tầm giá"
                : "Tần số quét " + picked.getSpecById(PeripheralSelector.REFRESH_RATE_HZ) + "Hz hợp nhu cầu chơi game";
    }

    private static PeripheralSuggestionResponse.Item toItem(String role, ComponentCandidateDTO item, CompatibilitySnapshot snapshot, String reason) {
        return PeripheralSuggestionResponse.Item.builder()
                .variantId(item.getVariantId())
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .brandName(item.getBrandName())
                .imageUrl(item.getImageUrl())
                .price(item.getPrice())
                .stockQuantity(item.getStockQuantity())
                .highlights(highlights(role, item, snapshot))
                .reason(reason)
                .build();
    }

    private static List<String> highlights(String role, ComponentCandidateDTO item, CompatibilitySnapshot snapshot) {
        List<String> result = new ArrayList<>();
        switch (role) {
            case PeripheralSelector.MONITOR -> {
                addValue(result, item, SCREEN_SIZE_INCH, " inch");
                addOption(result, item, RESOLUTION, snapshot);
                addValue(result, item, PeripheralSelector.REFRESH_RATE_HZ, "Hz");
                addOption(result, item, PANEL_TYPE, snapshot);
            }
            case PeripheralSelector.KEYBOARD -> {
                addOption(result, item, KEYBOARD_SWITCH, snapshot);
                addOption(result, item, KEYBOARD_LAYOUT, snapshot);
                addOption(result, item, CONNECTION_TYPE, snapshot);
            }
            default -> {
                addValue(result, item, MOUSE_DPI, " DPI");
                addValue(result, item, MOUSE_WEIGHT_G, " g");
                addOption(result, item, CONNECTION_TYPE, snapshot);
            }
        }
        return result;
    }

    private static void addValue(List<String> result, ComponentCandidateDTO item, int specId, String unit) {
        String value = item.getSpecById(specId);
        if (value != null) result.add(value + unit);
    }

    private static void addOption(List<String> result, ComponentCandidateDTO item, int specId, CompatibilitySnapshot snapshot) {
        String value = snapshot.display(specId, item.getSpecById(specId));
        if (value != null) result.add(value);
    }
}
