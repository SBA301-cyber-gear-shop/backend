package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.AutoBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.BuildItemRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BuildItemResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import com.project.cybershop.pcbuild.internal.service.BuildMetadataService;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.project.cybershop.pcbuild.internal.service.impl.BuildFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Điều phối: kiểm lại purpose / ngân sách (bước 3), rồi 4 → 5 → 6; API validate cho bước 9 gọi lại bước 6. */
class PcBuildServiceImplTest {

    private CatalogPublicApi catalogPublicApi;
    private PcBuildServiceImpl service;

    @BeforeEach
    void setUp() {
        BuildMetadataService metadataService = mock(BuildMetadataService.class);
        when(metadataService.loadMetadata(anyList())).thenReturn(metadata());
        CompatibilityRuleRepository ruleRepository = mock(CompatibilityRuleRepository.class);
        when(ruleRepository.findAll()).thenReturn(rules());
        catalogPublicApi = mock(CatalogPublicApi.class);
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(catalog());
        CompatibilityEngineImpl engine = new CompatibilityEngineImpl();
        service = new PcBuildServiceImpl(metadataService, new BudgetAllocationServiceImpl(),
                new BuildSolverImpl(engine, 200), engine, ruleRepository, catalogPublicApi);
    }

    @Test
    @DisplayName("autoBuild 20tr GAMING: VALID, 11 món theo step_order, RAM 2 thanh maxQty 2, dư 100.000đ")
    void autoBuild_docxExample() {
        BuildResultResponse response = service.autoBuild(new AutoBuildRequest(20_000_000L, "GAMING"));

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
        assertThat(response.getMessage()).isNull();
    }

    @Test
    @DisplayName("autoBuild nhận purpose chữ thường; OFFICE 12,5tr (ngay mức tối thiểu) ra 10 món, không GPU")
    void autoBuild_officeLowercase() {
        BuildResultResponse response = service.autoBuild(new AutoBuildRequest(12_500_000L, "office"));

        assertThat(response.getStatus()).isEqualTo(BuildStatus.VALID);
        assertThat(response.getPurpose()).isEqualTo("OFFICE");
        assertThat(response.getItems()).hasSize(10).noneMatch(item -> item.getCategory().equals("GPU"));
        assertThat(response.getTotal()).isLessThanOrEqualTo(12_500_000L);
    }

    @Test
    @DisplayName("purpose không có trong cấu hình → 400 PURPOSE_INVALID")
    void autoBuild_unknownPurpose() {
        assertBusinessError(() -> service.autoBuild(new AutoBuildRequest(20_000_000L, "MINING")), "PURPOSE_INVALID", 400);
    }

    @Test
    @DisplayName("Ngân sách dưới min_budget của purpose → 400 BUDGET_TOO_LOW kèm minBudget")
    void autoBuild_budgetTooLow() {
        assertThatThrownBy(() -> service.autoBuild(new AutoBuildRequest(14_000_000L, "GAMING")))
                .isInstanceOfSatisfying(PcBuildException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("BUDGET_TOO_LOW");
                    assertThat(((Map<?, ?>) exception.getData()).get("minBudget")).isEqualTo(16_000_000L);
                });
    }

    @Test
    @DisplayName("Ngân sách vượt maxBuildPrice → 400 BUDGET_TOO_HIGH")
    void autoBuild_budgetTooHigh() {
        assertBusinessError(() -> service.autoBuild(new AutoBuildRequest(100_000_001L, "GAMING")), "BUDGET_TOO_HIGH", 400);
    }

    @Test
    @DisplayName("validate bộ docx với ngân sách 19,6tr → OVER_BUDGET, câu cảnh báo theo mẫu bước 7")
    void validate_overBudget() {
        mockPartsById();

        BuildResultResponse response = service.validate(new ValidateBuildRequest(19_600_000L, docxItems(2)));

        assertThat(response.getStatus()).isEqualTo(BuildStatus.OVER_BUDGET);
        assertThat(response.getDifference()).isEqualTo(300_000L);
        assertThat(response.getBudgetWarning())
                .isEqualTo("Vượt ngân sách 300.000đ (tổng 19.900.000đ / ngân sách 19.600.000đ)");
        assertThat(response.getPurpose()).isNull();
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

    private void mockPartsById() {
        Map<Integer, ComponentCandidateDTO> byId = catalog().stream()
                .collect(Collectors.toMap(ComponentCandidateDTO::getVariantId, part -> part));
        when(catalogPublicApi.getCandidateMapByIds(anyList())).thenAnswer(invocation -> {
            List<Integer> ids = invocation.getArgument(0);
            return ids.stream().filter(byId::containsKey).collect(Collectors.toMap(id -> id, byId::get));
        });
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
