package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.AutoBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.BuildItemRequest;
import com.project.cybershop.pcbuild.api.dto.request.ReplacementRequest;
import com.project.cybershop.pcbuild.api.dto.request.SaveBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BuildItemResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.api.dto.response.ReplacementOptionResponse;
import com.project.cybershop.pcbuild.api.dto.response.SaveBuildResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.entity.PcCustomItem;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.BuildExplainer;
import com.project.cybershop.pcbuild.internal.service.BuildMetadataService;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.project.cybershop.pcbuild.internal.service.impl.BuildFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Điều phối: kiểm lại purpose / ngân sách (bước 3), rồi 4 → 5 → 6 → 8; bước 9: validate, replacements, save. */
class PcBuildServiceImplTest {

    private static final String EXPLANATION = "Dạ, Shop tư vấn cho bạn bộ PC chơi game...";

    private CatalogPublicApi catalogPublicApi;
    private BuildExplainer buildExplainer;
    private PcCustomRepository pcCustomRepository;
    private PcBuildServiceImpl service;

    @BeforeEach
    void setUp() {
        BuildMetadataService metadataService = mock(BuildMetadataService.class);
        when(metadataService.loadMetadata(anyList())).thenReturn(metadata());
        CompatibilityRuleRepository ruleRepository = mock(CompatibilityRuleRepository.class);
        when(ruleRepository.findAll()).thenReturn(rules());
        catalogPublicApi = mock(CatalogPublicApi.class);
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(catalog());
        buildExplainer = mock(BuildExplainer.class);
        when(buildExplainer.explain(anyString(), anyLong(), anyList(), anyLong(), anyString()))
                .thenReturn(new BuildExplainer.Explanation(EXPLANATION, BuildExplainer.Source.AI, null));
        pcCustomRepository = mock(PcCustomRepository.class);
        when(pcCustomRepository.save(any(PcCustom.class))).thenAnswer(invocation -> {
            PcCustom custom = invocation.getArgument(0);
            custom.setId(88);
            return custom;
        });
        CompatibilityEngineImpl engine = new CompatibilityEngineImpl();
        service = new PcBuildServiceImpl(metadataService, new BudgetAllocationServiceImpl(),
                new BuildSolverImpl(engine, 200), engine, ruleRepository, catalogPublicApi, buildExplainer,
                pcCustomRepository);
    }

    // ===================================================================== autoBuild (bước 3–6, 8)

    @Test
    @DisplayName("autoBuild 20tr GAMING: VALID, 11 món theo step_order, RAM 2 thanh maxQty 2, dư 100.000đ, có lời giải thích")
    void autoBuild_docxExample() {
        BuildResultResponse response = service.autoBuild(new AutoBuildRequest(20_000_000L, "GAMING"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(BuildStatus.VALID);
        assertThat(response.getPurpose()).isEqualTo("GAMING");
        assertThat(response.getTotal()).isEqualTo(19_900_000L);
        assertThat(response.getDifference()).isEqualTo(-100_000L);
        assertThat(response.getItems()).extracting(BuildItemResponse::getCategory)
                .containsExactly(CODE_BY_STEP);
        BuildItemResponse ram = response.getItems().get(5);
        assertThat(ram.getQty()).isEqualTo(2);
        assertThat(ram.getMaxQty()).isEqualTo(2);
        assertThat(ram.getPrice()).isEqualTo(800_000L);
        assertThat(response.getViolations()).isEmpty();
        assertThat(response.getBudgetWarning()).isNull();
        assertThat(response.getMessage()).isEqualTo(EXPLANATION);
    }

    @Test
    @DisplayName("Bước 8 nhận đúng nhãn purpose, ngân sách, tổng tiền code đã tính và khóa rate limit của client")
    void autoBuild_passesComputedNumbersToExplainer() {
        service.autoBuild(new AutoBuildRequest(20_000_000L, "GAMING"), "ip:test");

        verify(buildExplainer).explain(eq("GAMING"), eq(20_000_000L), anyList(), eq(19_900_000L), eq("ip:test"));
    }

    @Test
    @DisplayName("autoBuild nhận purpose chữ thường; OFFICE 12,5tr (ngay mức tối thiểu) ra 10 món, không GPU")
    void autoBuild_officeLowercase() {
        BuildResultResponse response = service.autoBuild(new AutoBuildRequest(12_500_000L, "office"), "ip:test");

        assertThat(response.getStatus()).isEqualTo(BuildStatus.VALID);
        assertThat(response.getPurpose()).isEqualTo("OFFICE");
        assertThat(response.getItems()).hasSize(10).noneMatch(item -> item.getCategory().equals("GPU"));
        assertThat(response.getTotal()).isLessThanOrEqualTo(12_500_000L);
    }

    @Test
    @DisplayName("purpose không có trong cấu hình → 400 PURPOSE_INVALID, không gọi AI bước 8")
    void autoBuild_unknownPurpose() {
        assertBusinessError(() -> service.autoBuild(new AutoBuildRequest(20_000_000L, "MINING"), "ip:test"),
                "PURPOSE_INVALID", 400);
        verifyNoInteractions(buildExplainer);
    }

    @Test
    @DisplayName("Ngân sách dưới min_budget của purpose → 400 BUDGET_TOO_LOW kèm minBudget")
    void autoBuild_budgetTooLow() {
        assertThatThrownBy(() -> service.autoBuild(new AutoBuildRequest(14_000_000L, "GAMING"), "ip:test"))
                .isInstanceOfSatisfying(PcBuildException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("BUDGET_TOO_LOW");
                    assertThat(((Map<?, ?>) exception.getData()).get("minBudget")).isEqualTo(16_000_000L);
                });
    }

    @Test
    @DisplayName("Ngân sách vượt maxBuildPrice → 400 BUDGET_TOO_HIGH")
    void autoBuild_budgetTooHigh() {
        assertBusinessError(() -> service.autoBuild(new AutoBuildRequest(100_000_001L, "GAMING"), "ip:test"),
                "BUDGET_TOO_HIGH", 400);
    }

    // ===================================================================== validate (bước 9 → 6, 7)

    @Test
    @DisplayName("validate bộ docx với ngân sách 19,6tr → OVER_BUDGET, câu cảnh báo theo mẫu bước 7, không gọi AI")
    void validate_overBudget() {
        mockPartsById();

        BuildResultResponse response = service.validate(new ValidateBuildRequest(19_600_000L, docxItems(2)));

        assertThat(response.getStatus()).isEqualTo(BuildStatus.OVER_BUDGET);
        assertThat(response.getDifference()).isEqualTo(300_000L);
        assertThat(response.getBudgetWarning())
                .isEqualTo("Vượt ngân sách 300.000đ (tổng 19.900.000đ / ngân sách 19.600.000đ)");
        assertThat(response.getPurpose()).isNull();
        assertThat(response.getMessage()).isNull();
        verifyNoInteractions(buildExplainer);
    }

    @Test
    @DisplayName("validate 3 thanh RAM trên main 2 khe → INCOMPATIBLE, RULE_7, maxQty RAM = 2")
    void validate_tooManyRamSticks() {
        mockPartsById();

        BuildResultResponse response = service.validate(new ValidateBuildRequest(30_000_000L, docxItems(3)));

        assertThat(response.getStatus()).isEqualTo(BuildStatus.INCOMPATIBLE);
        assertThat(response.getViolations()).extracting("rule").containsExactly("RULE_7");
        assertThat(response.getItems().stream().filter(item -> item.getId() == RAM_A).findFirst().orElseThrow().getMaxQty())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("validate với variantId không tồn tại → 400 VARIANT_NOT_FOUND")
    void validate_unknownVariant() {
        mockPartsById();
        List<BuildItemRequest> items = List.of(new BuildItemRequest(CPU_A, 1), new BuildItemRequest(99_999, 1));

        assertBusinessError(() -> service.validate(new ValidateBuildRequest(20_000_000L, items)), "VARIANT_NOT_FOUND", 400);
    }

    // ===================================================================== replacements (bước 9, dropdown)

    @Test
    @DisplayName("Đổi Main: chỉ hiện main cùng socket AM5 (Main Intel LGA1700 bị loại theo R1)")
    void replacements_mainboardKeepsSocket() {
        mockPartsById();

        List<ReplacementOptionResponse> options = service.replacements(new ReplacementRequest("MAINBOARD", docxItems(2)));

        assertThat(options).extracting(ReplacementOptionResponse::getId).containsExactly(MAIN_A);
        assertThat(options.get(0).isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Đổi RAM: chỉ RAM DDR5 (R2), giữ 2 thanh, maxQty = 2 theo số khe của Main-A")
    void replacements_ramFollowsMainboard() {
        mockPartsById();

        List<ReplacementOptionResponse> options = service.replacements(new ReplacementRequest("ram", docxItems(2)));

        assertThat(options).singleElement().satisfies(option -> {
            assertThat(option.getId()).isEqualTo(RAM_A);
            assertThat(option.getQty()).isEqualTo(2);
            assertThat(option.getMaxQty()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("Đổi GPU: mọi card vừa case và nguồn, xếp giá giảm dần, đánh dấu món đang chọn")
    void replacements_gpuSortedByPrice() {
        mockPartsById();

        List<ReplacementOptionResponse> options = service.replacements(new ReplacementRequest("GPU", docxItems(2)));

        assertThat(options).extracting(ReplacementOptionResponse::getId).containsExactly(GPU_A, GPU_CHEAP);
        assertThat(options).extracting(ReplacementOptionResponse::isCurrent).containsExactly(true, false);
    }

    @Test
    @DisplayName("Đổi CPU: loại CPU khác socket; CPU đắt hơn cùng socket vẫn hiện (vượt tiền không phải xung đột)")
    void replacements_cpuOverBudgetStillListed() {
        mockPartsById();

        List<ReplacementOptionResponse> options = service.replacements(new ReplacementRequest("CPU", docxItems(2)));

        assertThat(options).extracting(ReplacementOptionResponse::getId).containsExactly(CPU_EXPENSIVE, CPU_A);
    }

    @Test
    @DisplayName("Loại linh kiện không có trong khuôn → 400 CATEGORY_INVALID")
    void replacements_unknownCategory() {
        assertBusinessError(() -> service.replacements(new ReplacementRequest("PRINTER", List.of())),
                "CATEGORY_INVALID", 400);
    }

    @Test
    @DisplayName("Bộ PC có id không tồn tại → 400 VARIANT_NOT_FOUND")
    void replacements_unknownVariant() {
        mockPartsById();
        List<BuildItemRequest> items = List.of(new BuildItemRequest(CPU_A, 1), new BuildItemRequest(99_999, 1));

        assertBusinessError(() -> service.replacements(new ReplacementRequest("GPU", items)), "VARIANT_NOT_FOUND", 400);
    }

    @Test
    @DisplayName("Bộ PC rỗng → mọi màn hình còn hàng, giá giảm dần, không có món đang chọn")
    void replacements_emptyBuild() {
        List<ReplacementOptionResponse> options = service.replacements(new ReplacementRequest("MONITOR", List.of()));

        assertThat(options).extracting(ReplacementOptionResponse::getId)
                .containsExactly(MONITOR_EXPENSIVE, MONITOR_A, MONITOR_CHEAP);
        assertThat(options).noneMatch(ReplacementOptionResponse::isCurrent);
    }

    @Test
    @DisplayName("GPU dài 400mm không vừa case 330mm (luật chiều dài VGA) → không hiện")
    void replacements_gpuTooLongForCase() {
        mockPartsById();
        List<ComponentCandidateDTO> stock = new java.util.ArrayList<>(catalog());
        stock.add(gpu(64, "GPU dài", 7_000_000, "400", "160"));
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(stock);

        List<ReplacementOptionResponse> options = service.replacements(new ReplacementRequest("GPU", docxItems(2)));

        assertThat(options).extracting(ReplacementOptionResponse::getId).containsExactly(GPU_A, GPU_CHEAP);
    }

    @Test
    @DisplayName("RAM DDR5 chỉ còn 1 thanh trong kho khi đang cần 2 thanh → không hiện")
    void replacements_notEnoughStock() {
        mockPartsById();
        List<ComponentCandidateDTO> stock = new java.util.ArrayList<>(catalog());
        stock.add(part(47, 3, "RAM DDR5 16GB", 1_200_000, 1, Map.of(4, "DDR5", 9, "16", 34, "1")));
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(stock);

        List<ReplacementOptionResponse> options = service.replacements(new ReplacementRequest("RAM", docxItems(2)));

        assertThat(options).extracting(ReplacementOptionResponse::getId).containsExactly(RAM_A);
    }

    @Test
    @DisplayName("Đang có 2 GPU: main chỉ 1 khe PCIe x16 không hiện, dù cùng socket và cùng loại RAM")
    void replacements_mainboardWithFewerPcieSlots() {
        ComponentCandidateDTO twoGpuBoard = part(33, 2, "Main 2 khe PCIe", 2_000_000, 10, Map.of(1, "AM5", 4, "DDR5",
                5, "2", 6, "64", 7, "MICRO_ATX", 8, "1", 31, "1", 32, "2"));
        ComponentCandidateDTO multiGpu = part(63, 4, "GPU chạy đôi", 5_600_000, 10, Map.of(11, "250", 2, "160", 50, "550",
                54, "2"));
        List<ComponentCandidateDTO> stock = new java.util.ArrayList<>(catalog());
        stock.add(twoGpuBoard);
        stock.add(multiGpu);
        mockPartsById(stock);
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(stock);
        List<BuildItemRequest> items = List.of(new BuildItemRequest(MONITOR_A, 1), new BuildItemRequest(KEYBOARD_A, 1),
                new BuildItemRequest(MOUSE_A, 1), new BuildItemRequest(CPU_A, 1), new BuildItemRequest(33, 1),
                new BuildItemRequest(RAM_A, 2), new BuildItemRequest(SSD_A, 1), new BuildItemRequest(63, 2),
                new BuildItemRequest(COOLER_A, 1), new BuildItemRequest(CASE_A, 1), new BuildItemRequest(PSU_650, 1));

        List<ReplacementOptionResponse> options = service.replacements(new ReplacementRequest("MAINBOARD", items));

        assertThat(options).extracting(ReplacementOptionResponse::getId).containsExactly(33);
    }

    // ===================================================================== save (bước 9, nút Lưu)

    @Test
    @DisplayName("Lưu bộ VALID: 11 dòng pc_custom_items, giá đọc lại từ DB, component_role là mã slot")
    void save_validBuild() {
        mockPartsByIdForSave();

        SaveBuildResponse response = service.save(new SaveBuildRequest(20_000_000L, docxItems(2), null), 7);

        assertThat(response.getBuildId()).isEqualTo(88);
        assertThat(response.getStatus()).isEqualTo(BuildStatus.VALID);
        assertThat(response.getTotal()).isEqualTo(19_900_000L);
        PcCustom saved = savedBuild();
        assertThat(saved.getUserId()).isEqualTo(7);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("19900000");
        assertThat(saved.getItems()).hasSize(11).allMatch(item -> item.getPcCustom() == saved);
        assertThat(saved.getItems()).extracting(PcCustomItem::getComponentRole).containsExactly(CODE_BY_STEP);
        PcCustomItem ram = saved.getItems().get(5);
        assertThat(ram.getQuantity()).isEqualTo(2);
        assertThat(ram.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(800_000));
    }

    @Test
    @DisplayName("OVER_BUDGET chưa xác nhận → 409 OVER_BUDGET_NOT_CONFIRMED, message là câu popup bước 7, không lưu")
    void save_overBudgetNeedsConfirmation() {
        mockPartsByIdForSave();

        assertThatThrownBy(() -> service.save(new SaveBuildRequest(19_600_000L, docxItems(2), false), 7))
                .isInstanceOfSatisfying(PcBuildException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("OVER_BUDGET_NOT_CONFIRMED");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(409);
                    assertThat(exception.getMessage()).isEqualTo("Bộ PC hiện tại đã vượt quá ngân sách 300.000đ "
                            + "(tổng 19.900.000đ, ngân sách 19.600.000đ). Bạn có muốn tiếp tục?");
                    assertThat(((Map<?, ?>) exception.getData()).get("difference")).isEqualTo(300_000L);
                });
        verify(pcCustomRepository, never()).save(any());
    }

    @Test
    @DisplayName("OVER_BUDGET đã bấm Tiếp tục (confirmOverBudget = true) → lưu, trả status OVER_BUDGET")
    void save_overBudgetConfirmed() {
        mockPartsByIdForSave();

        SaveBuildResponse response = service.save(new SaveBuildRequest(19_600_000L, docxItems(2), true), 7);

        assertThat(response.getStatus()).isEqualTo(BuildStatus.OVER_BUDGET);
        assertThat(response.getDifference()).isEqualTo(300_000L);
        verify(pcCustomRepository).save(any(PcCustom.class));
    }

    @Test
    @DisplayName("Bộ INCOMPATIBLE (3 thanh RAM, main 2 khe) → 422 BUILD_INCOMPATIBLE kèm violations, không lưu dù có xác nhận")
    void save_incompatibleIsRejected() {
        mockPartsByIdForSave();

        assertThatThrownBy(() -> service.save(new SaveBuildRequest(30_000_000L, docxItems(3), true), 7))
                .isInstanceOfSatisfying(PcBuildException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("BUILD_INCOMPATIBLE");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(422);
                    assertThat((List<?>) ((Map<?, ?>) exception.getData()).get("violations")).isNotEmpty();
                });
        verify(pcCustomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lưu với id không tồn tại → 400 VARIANT_NOT_FOUND, không lưu")
    void save_unknownVariant() {
        mockPartsByIdForSave();
        List<BuildItemRequest> items = List.of(new BuildItemRequest(CPU_A, 1), new BuildItemRequest(99_999, 1));

        assertBusinessError(() -> service.save(new SaveBuildRequest(20_000_000L, items, true), 7), "VARIANT_NOT_FOUND", 400);
        verify(pcCustomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lưu bộ thiếu nguồn → 422 BUILD_INCOMPATIBLE với MISSING_SLOT, không lưu")
    void save_missingRequiredSlot() {
        mockPartsByIdForSave();
        List<BuildItemRequest> withoutPsu = docxItems(2).stream().filter(item -> item.variantId() != PSU_650).toList();

        assertThatThrownBy(() -> service.save(new SaveBuildRequest(20_000_000L, withoutPsu, null), 7))
                .isInstanceOfSatisfying(PcBuildException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("BUILD_INCOMPATIBLE");
                    assertThat(exception.getData().toString()).contains("MISSING_SLOT");
                });
        verify(pcCustomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Giá GPU tăng lên 6tr trong DB lúc lưu → tính theo giá mới: tổng 20,3tr vượt 20tr → 409")
    void save_usesFreshDatabasePrice() {
        Map<Integer, ComponentCandidateDTO> byId = catalogById();
        byId.put(GPU_A, gpu(GPU_A, "GPU-A", 6_000_000, "250", "160"));
        when(catalogPublicApi.getCandidatesByIds(anyList())).thenAnswer(invocation -> {
            List<Integer> ids = invocation.getArgument(0);
            return ids.stream().filter(byId::containsKey).map(byId::get).toList();
        });

        assertThatThrownBy(() -> service.save(new SaveBuildRequest(20_000_000L, docxItems(2), null), 7))
                .isInstanceOfSatisfying(PcBuildException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("OVER_BUDGET_NOT_CONFIRMED");
                    assertThat(((Map<?, ?>) exception.getData()).get("total")).isEqualTo(20_300_000L);
                });

        service.save(new SaveBuildRequest(20_000_000L, docxItems(2), true), 7);
        PcCustomItem gpu = savedBuild().getItems().get(7);
        assertThat(gpu.getProductVariantId()).isEqualTo(GPU_A);
        assertThat(gpu.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(6_000_000));
    }

    // ===================================================================== tiện ích

    private void mockPartsById() {
        mockPartsById(catalog());
    }

    private void mockPartsById(List<ComponentCandidateDTO> parts) {
        Map<Integer, ComponentCandidateDTO> byId = parts.stream()
                .collect(Collectors.toMap(ComponentCandidateDTO::getVariantId, part -> part));
        when(catalogPublicApi.getCandidateMapByIds(anyList())).thenAnswer(invocation -> {
            List<Integer> ids = invocation.getArgument(0);
            return ids.stream().filter(byId::containsKey).collect(Collectors.toMap(id -> id, byId::get));
        });
    }

    private void mockPartsByIdForSave() {
        Map<Integer, ComponentCandidateDTO> byId = catalogById();
        when(catalogPublicApi.getCandidatesByIds(anyList())).thenAnswer(invocation -> {
            List<Integer> ids = invocation.getArgument(0);
            return ids.stream().filter(byId::containsKey).map(byId::get).toList();
        });
    }

    private static Map<Integer, ComponentCandidateDTO> catalogById() {
        return catalog().stream().collect(Collectors.toMap(ComponentCandidateDTO::getVariantId, part -> part,
                (first, second) -> first, java.util.HashMap::new));
    }

    private PcCustom savedBuild() {
        ArgumentCaptor<PcCustom> captor = ArgumentCaptor.forClass(PcCustom.class);
        verify(pcCustomRepository).save(captor.capture());
        return captor.getValue();
    }

    /** Bộ của ví dụ docx, gửi theo thứ tự lộn xộn để kiểm server tự xếp theo step_order. */
    private static List<BuildItemRequest> docxItems(int ramQuantity) {
        return List.of(new BuildItemRequest(PSU_650, 1), new BuildItemRequest(CPU_A, 1), new BuildItemRequest(MAIN_A, 1),
                new BuildItemRequest(RAM_A, ramQuantity), new BuildItemRequest(SSD_A, 1), new BuildItemRequest(GPU_A, 1),
                new BuildItemRequest(COOLER_A, 1), new BuildItemRequest(CASE_A, 1), new BuildItemRequest(MONITOR_A, 1),
                new BuildItemRequest(KEYBOARD_A, 1), new BuildItemRequest(MOUSE_A, 1));
    }

    private static void assertBusinessError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code,
                                            int httpStatus) {
        assertThatThrownBy(call).isInstanceOfSatisfying(PcBuildException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(code);
            assertThat(exception.getHttpStatus().value()).isEqualTo(httpStatus);
        });
    }
}
