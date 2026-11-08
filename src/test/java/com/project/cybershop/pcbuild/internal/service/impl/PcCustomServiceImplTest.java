package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.ReplacementQueryRequest;
import com.project.cybershop.pcbuild.api.dto.request.SavePcBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildDetailResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildSummaryResponse;
import com.project.cybershop.pcbuild.api.dto.response.ValidateBuildResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.entity.PcCustomItem;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.PcCustomService;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.project.cybershop.pcbuild.internal.PcBuildTestData.RULES;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.part;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.snapshot;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.validDesktop;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.withSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PcCustomServiceImpl - Kiểm thử lưu cấu hình, /validate và linh kiện thay thế qua CompatibilityEngine")
class PcCustomServiceImplTest {

    @Mock
    private PcCustomRepository pcCustomRepository;

    @Mock
    private CatalogPublicApi catalogPublicApi;

    @Mock
    private CandidateRepositoryFacade candidateRepositoryFacade;

    // Engine thật: thuần bộ nhớ nên không cần mock
    @Spy
    private RuleBasedCompatibilityEngine compatibilityEngine = new RuleBasedCompatibilityEngine();

    @InjectMocks
    private PcCustomServiceImpl pcCustomService;

    // Bộ desktop chuẩn (variant 1–8, tổng 27.000.000) cùng 2 bo thay thế để thử replacements
    private Map<String, ComponentCandidateDTO> desktop;
    private ComponentCandidateDTO boardCompatible;
    private ComponentCandidateDTO boardWrongSocket;

    @BeforeEach
    void setUpBuild() {
        desktop = validDesktop();
        boardCompatible = withSpec(part(201, "mainboard", "B650M Pro", 3_500_000, desktop.get("mainboard").getSpecValuesById()), 1, "AM5");
        boardWrongSocket = withSpec(part(202, "mainboard", "B760M LGA1700", 2_500_000, desktop.get("mainboard").getSpecValuesById()), 1, "LGA1700");
    }

    private void stubSnapshotWithDesktop() {
        List<ComponentCandidateDTO> candidates = new ArrayList<>(desktop.values());
        candidates.add(boardCompatible);
        candidates.add(boardWrongSocket);
        when(candidateRepositoryFacade.snapshot()).thenReturn(snapshot(RULES, candidates));
    }

    private List<ValidateBuildRequest.ItemPart> desktopItems() {
        List<ValidateBuildRequest.ItemPart> items = new ArrayList<>();
        for (String role : List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler")) {
            items.add(new ValidateBuildRequest.ItemPart(role, desktop.get(role).getVariantId(), 1));
        }
        return items;
    }

    private List<SavePcBuildRequest.SavePartItem> desktopSaveItems() {
        return desktopItems().stream()
                .map(item -> SavePcBuildRequest.SavePartItem.builder().variantId(item.getVariantId())
                        .role(item.getRole()).quantity(item.getQuantity()).unitPrice(BigDecimal.ONE).build())
                .toList();
    }

    @Test
    @DisplayName("B17: Lưu mới cấu hình lấy giá mới theo id từ catalog, bỏ qua totalPrice/unitPrice client gửi")
    void shouldSaveNewBuildWithCatalogPrices() {
        var req = SavePcBuildRequest.builder().buildName("Gaming Rig").totalPrice(BigDecimal.ONE)
                .items(desktopSaveItems()).build();
        when(catalogPublicApi.getCandidatesByIds(any())).thenReturn(new ArrayList<>(desktop.values()));
        stubSnapshotWithDesktop();
        when(pcCustomRepository.save(any())).thenAnswer(inv -> {
            PcCustom c = inv.getArgument(0);
            c.setId(88);
            return c;
        });

        Integer savedId = pcCustomService.saveNewBuild(req, 15);

        assertThat(savedId).isEqualTo(88);
        ArgumentCaptor<PcCustom> captor = ArgumentCaptor.forClass(PcCustom.class);
        verify(pcCustomRepository).save(captor.capture());
        PcCustom saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(15);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("27000000");
        assertThat(saved.getItems()).extracting(PcCustomItem::getUnitPrice)
                .usingElementComparator(BigDecimal::compareTo)
                .contains(new BigDecimal("5000000"), new BigDecimal("9000000"))
                .doesNotContain(BigDecimal.ONE);
        verify(catalogPublicApi, never()).getCandidateMapByIds(any());
    }

    private static ComponentCandidateDTO peripheral(int id, String slug, long price) {
        return ComponentCandidateDTO.builder().variantId(id).categorySlug(slug).productName("SP " + id)
                .price(BigDecimal.valueOf(price)).build();
    }

    private static SavePcBuildRequest.SavePartItem saveItem(String role, int variantId) {
        return SavePcBuildRequest.SavePartItem.builder().variantId(variantId).role(role).quantity(1).build();
    }

    @Test
    @DisplayName("Trọn bộ: lưu cả màn, phím, chuột vào cấu hình; tổng tiền gồm cả 3 món; tương thích chỉ kiểm trên thùng máy")
    void shouldSavePeripheralsWithBuild() {
        List<SavePcBuildRequest.SavePartItem> items = new ArrayList<>(desktopSaveItems());
        items.add(saveItem("monitor", 203));
        items.add(saveItem("keyboard", 213));
        items.add(saveItem("mouse", 224));
        List<ComponentCandidateDTO> catalog = new ArrayList<>(desktop.values());
        catalog.add(peripheral(203, "monitor", 3_890_000));
        catalog.add(peripheral(213, "keyboard", 690_000));
        catalog.add(peripheral(224, "mouse", 490_000));
        when(catalogPublicApi.getCandidatesByIds(any())).thenReturn(catalog);
        stubSnapshotWithDesktop();
        when(pcCustomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pcCustomService.saveNewBuild(SavePcBuildRequest.builder().items(items).build(), 15);

        ArgumentCaptor<PcCustom> captor = ArgumentCaptor.forClass(PcCustom.class);
        verify(pcCustomRepository).save(captor.capture());
        assertThat(captor.getValue().getItems()).extracting(PcCustomItem::getComponentRole)
                .contains("monitor", "keyboard", "mouse").hasSize(11);
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo("32070000");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.project.cybershop.pcbuild.internal.model.BuildLine>> lines = ArgumentCaptor.forClass(List.class);
        verify(compatibilityEngine).evaluate(lines.capture(), any());
        assertThat(lines.getValue()).extracting(com.project.cybershop.pcbuild.internal.model.BuildLine::role)
                .doesNotContain("monitor", "keyboard", "mouse").hasSize(8);
    }

    @Test
    @DisplayName("Trọn bộ: món phụ kiện gửi sai loại (bàn phím mang role mouse) -> 422, không lưu")
    void shouldRejectPeripheralWithWrongRole() {
        List<SavePcBuildRequest.SavePartItem> items = new ArrayList<>(desktopSaveItems());
        items.add(saveItem("mouse", 213));
        List<ComponentCandidateDTO> catalog = new ArrayList<>(desktop.values());
        catalog.add(peripheral(213, "keyboard", 690_000));
        when(catalogPublicApi.getCandidatesByIds(any())).thenReturn(catalog);

        assertThatThrownBy(() -> pcCustomService.saveNewBuild(SavePcBuildRequest.builder().items(items).build(), 15))
                .isInstanceOfSatisfying(com.project.cybershop.pcbuild.internal.support.BuildRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("PERIPHERAL_ROLE_MISMATCH"));
        verify(pcCustomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trọn bộ: linh kiện PC gửi với role monitor để né kiểm tương thích -> 422, không lưu")
    void shouldRejectPcPartDisguisedAsPeripheral() {
        List<SavePcBuildRequest.SavePartItem> items = new ArrayList<>(desktopSaveItems());
        items.add(saveItem("monitor", 202));
        List<ComponentCandidateDTO> catalog = new ArrayList<>(desktop.values());
        catalog.add(boardWrongSocket);
        when(catalogPublicApi.getCandidatesByIds(any())).thenReturn(catalog);

        assertThatThrownBy(() -> pcCustomService.saveNewBuild(SavePcBuildRequest.builder().items(items).build(), 15))
                .isInstanceOfSatisfying(com.project.cybershop.pcbuild.internal.support.BuildRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("PERIPHERAL_ROLE_MISMATCH"));
        verify(pcCustomRepository, never()).save(any());
    }

    @Test
    @DisplayName("B17: Lưu cấu hình có linh kiện không tồn tại -> Ném BusinessException, không lưu")
    void shouldRejectSaveWhenVariantNotInCatalog() {
        var req = SavePcBuildRequest.builder()
                .items(List.of(SavePcBuildRequest.SavePartItem.builder()
                        .variantId(999).role("cpu").quantity(1).build()))
                .build();
        when(catalogPublicApi.getCandidatesByIds(any())).thenReturn(List.of());

        assertThatThrownBy(() -> pcCustomService.saveNewBuild(req, 15))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("#999");
        verify(pcCustomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lưu build không tương thích -> 422 BUILD_INCOMPATIBLE kèm checks, không lưu")
    void shouldRejectIncompatibleBuildOnSave() {
        desktop.put("mainboard", boardWrongSocket);
        var req = SavePcBuildRequest.builder().items(desktopSaveItems()).build();
        when(catalogPublicApi.getCandidatesByIds(any())).thenReturn(new ArrayList<>(desktop.values()));
        stubSnapshotWithDesktop();

        assertThatThrownBy(() -> pcCustomService.saveNewBuild(req, 15))
                .isInstanceOf(BuildRuleException.class)
                .satisfies(error -> {
                    BuildRuleException exception = (BuildRuleException) error;
                    assertThat(exception.getCode()).isEqualTo("BUILD_INCOMPATIBLE");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(422);
                    assertThat(exception.getData()).isInstanceOf(Map.class);
                    assertThat(((Map<?, ?>) exception.getData()).containsKey("checks")).isTrue();
                });
        verify(pcCustomRepository, never()).save(any());
    }

    @Test
    @DisplayName("/validate: giá từ snapshot, budgetFit đúng, không truy vấn catalog khi mọi món còn hàng")
    void shouldValidateBuildFromSnapshot() {
        stubSnapshotWithDesktop();
        ValidateBuildRequest request = new ValidateBuildRequest(new BigDecimal("28000000"), desktopItems());

        ValidateBuildResponse response = pcCustomService.validateBuild(request);

        assertThat(response.isCompatible()).isTrue();
        assertThat(response.getTotalPrice()).isEqualByComparingTo("27000000");
        assertThat(response.getBudgetFit().getStatus()).isEqualTo("IN_WINDOW");
        assertThat(response.getBudgetFit().getPercent()).isEqualByComparingTo("96.4");
        assertThat(response.getBudgetFit().getLeftover()).isEqualByComparingTo("1000000");
        assertThat(response.getItems()).hasSize(8);
        assertThat(response.getRequiredPsuW()).isEqualTo(650);
        verifyNoInteractions(catalogPublicApi);
    }

    @Test
    @DisplayName("/validate: tổng vượt ngân sách -> J8a fail, budgetFit OVER")
    void shouldReportOverBudget() {
        stubSnapshotWithDesktop();

        ValidateBuildResponse response = pcCustomService.validateBuild(
                new ValidateBuildRequest(new BigDecimal("26000000"), desktopItems()));

        assertThat(response.isCompatible()).isFalse();
        assertThat(response.getBudgetFit().getStatus()).isEqualTo("OVER");
        assertThat(response.getBudgetFit().getLeftover()).isEqualByComparingTo("-1000000");
        assertThat(response.getChecks().get(0).getKey()).isEqualTo("J8a");
    }

    @Test
    @DisplayName("/validate: món hết hàng và id không tồn tại -> check STOCK fail, không ném lỗi")
    void shouldReportStockProblemsAsChecks() {
        stubSnapshotWithDesktop();
        ComponentCandidateDTO soldOutPsu = part(600, "psu", "PSU hết hàng", 2_000_000, Map.of(14, "750"));
        when(catalogPublicApi.getCandidatesByIds(List.of(600, 777))).thenReturn(List.of(soldOutPsu));
        List<ValidateBuildRequest.ItemPart> items = new ArrayList<>(desktopItems());
        items.set(5, new ValidateBuildRequest.ItemPart("psu", 600, 1));
        items.add(new ValidateBuildRequest.ItemPart("ssd", 777, 1));

        ValidateBuildResponse response = pcCustomService.validateBuild(new ValidateBuildRequest(null, items));

        assertThat(response.isCompatible()).isFalse();
        assertThat(response.getChecks()).filteredOn(check -> "STOCK".equals(check.getKey()))
                .extracting(check -> check.getDetail())
                .containsExactlyInAnyOrder("PSU hết hàng đã hết hàng hoặc ngừng kinh doanh", "Linh kiện #777 không tồn tại");
        assertThat(response.getBudgetFit()).isNull();
        assertThat(response.getItems()).hasSize(8);
    }

    @Test
    @DisplayName("POST replacements: mỗi ứng viên được kiểm trên toàn bộ build bằng engine, tương thích xếp trước")
    void shouldRankReplacementsWithEngine() {
        stubSnapshotWithDesktop();
        ReplacementQueryRequest request = ReplacementQueryRequest.builder()
                .role("mainboard").budget(new BigDecimal("28000000")).items(desktopItems()).build();

        List<CompatibleReplacementResponse> res = pcCustomService.getCompatibleReplacements(request);

        // Bo hiện tại (tổng 27 tr) sát B = 28 tr hơn bo 201 (26,5 tr); bo lệch socket xếp cuối
        assertThat(res).extracting(CompatibleReplacementResponse::getVariantId).containsExactly(2, 201, 202);
        CompatibleReplacementResponse cheaper = res.get(1);
        assertThat(cheaper.isCompatible()).isTrue();
        assertThat(cheaper.getStatus()).isEqualTo("pass");
        assertThat(cheaper.getPriceDelta()).isEqualByComparingTo("-500000");
        assertThat(cheaper.getNewTotal()).isEqualByComparingTo("26500000");
        assertThat(cheaper.getNewPercent()).isEqualByComparingTo("94.64");
        CompatibleReplacementResponse wrongSocket = res.get(2);
        assertThat(wrongSocket.isCompatible()).isFalse();
        assertThat(wrongSocket.getFailedChecks()).containsExactly("R1");
        assertThat(wrongSocket.getCompatibilityNote()).contains("Không tương thích");
        verifyNoInteractions(catalogPublicApi);
    }

    @Test
    @DisplayName("getMySavedBuilds: Trả về danh sách cấu hình với tên được tự động tạo từ CPU & GPU")
    void shouldReturnMySavedBuildsWithGeneratedNames() {
        PcCustom custom = new PcCustom();
        custom.setId(1);
        custom.setUserId(10);
        custom.setTotalPrice(new BigDecimal("20000000"));

        PcCustomItem cpuItem = new PcCustomItem();
        cpuItem.setProductVariantId(101);
        cpuItem.setComponentRole("cpu");
        cpuItem.setQuantity(1);

        PcCustomItem gpuItem = new PcCustomItem();
        gpuItem.setProductVariantId(401);
        gpuItem.setComponentRole("gpu");
        gpuItem.setQuantity(1);

        PcCustomItem ramItem = new PcCustomItem();
        ramItem.setProductVariantId(301);
        ramItem.setComponentRole("ram");
        ramItem.setQuantity(2);

        custom.getItems().addAll(List.of(cpuItem, gpuItem, ramItem));

        ComponentCandidateDTO cpuCandidate = ComponentCandidateDTO.builder()
                .variantId(101).productName("Intel Core i5-13400F").build();
        ComponentCandidateDTO gpuCandidate = ComponentCandidateDTO.builder()
                .variantId(401).productName("GeForce RTX 4060").build();
        ComponentCandidateDTO ramCandidate = ComponentCandidateDTO.builder()
                .variantId(301).productName("Kingston Fury Beast 16GB").build();

        when(pcCustomRepository.findByUserIdOrderByIdDesc(10)).thenReturn(List.of(custom));
        when(catalogPublicApi.getCandidateMapByIds(any())).thenReturn(Map.of(
                101, cpuCandidate,
                401, gpuCandidate,
                301, ramCandidate
        ));

        List<SavedPcBuildSummaryResponse> result = pcCustomService.getMySavedBuilds(10);

        assertThat(result).hasSize(1);
        SavedPcBuildSummaryResponse summary = result.get(0);
        assertThat(summary.getBuildId()).isEqualTo(1);
        assertThat(summary.getBuildName()).isEqualTo("Bộ PC Intel Core i5-13400F & GeForce RTX 4060");
        assertThat(summary.getCpuName()).isEqualTo("Intel Core i5-13400F");
        assertThat(summary.getGpuName()).isEqualTo("GeForce RTX 4060");
        assertThat(summary.getRamSummary()).isEqualTo("Kingston Fury Beast 16GB (x2)");
        assertThat(summary.isOrdered()).isFalse();
    }

    @Test
    @DisplayName("getSavedBuildDetail: Lấy chi tiết cấu hình thành công")
    void shouldReturnSavedBuildDetailSuccessfully() {
        PcCustom custom = new PcCustom();
        custom.setId(5);
        custom.setUserId(10);
        custom.setTotalPrice(new BigDecimal("12000000"));

        PcCustomItem cpuItem = new PcCustomItem();
        cpuItem.setProductVariantId(101);
        cpuItem.setComponentRole("cpu");
        cpuItem.setQuantity(1);
        cpuItem.setUnitPrice(new BigDecimal("5000000"));
        custom.getItems().add(cpuItem);

        ComponentCandidateDTO cpuCandidate = ComponentCandidateDTO.builder()
                .variantId(101).productName("Intel Core i5-13400F").imageUrl("http://img.png").build();

        when(pcCustomRepository.findById(5)).thenReturn(Optional.of(custom));
        when(catalogPublicApi.getCandidateMapByIds(any())).thenReturn(Map.of(101, cpuCandidate));

        SavedPcBuildDetailResponse detail = pcCustomService.getSavedBuildDetail(5, 10);

        assertThat(detail.getBuildId()).isEqualTo(5);
        assertThat(detail.getBuildName()).isEqualTo("Bộ PC Intel Core i5-13400F (Đồ họa tích hợp)");
        assertThat(detail.getItems()).hasSize(1);
        assertThat(detail.getItems().get(0).getProductName()).isEqualTo("Intel Core i5-13400F");
    }

    @Test
    @DisplayName("getSavedBuildDetail: Ném AccessDeniedException khi truy cập cấu hình của user khác")
    void shouldThrowAccessDeniedWhenViewingOtherUsersBuild() {
        PcCustom custom = new PcCustom();
        custom.setId(5);
        custom.setUserId(99); // Khác user 10

        when(pcCustomRepository.findById(5)).thenReturn(Optional.of(custom));

        assertThatThrownBy(() -> pcCustomService.getSavedBuildDetail(5, 10))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Bạn không có quyền truy cập cấu hình này");
    }

    @Test
    @DisplayName("deleteSavedBuild: Xóa thành công cấu hình chưa đặt hàng")
    void shouldDeleteSavedBuildSuccessfully() {
        PcCustom custom = new PcCustom();
        custom.setId(5);
        custom.setUserId(10);
        custom.setOrderId(null);

        when(pcCustomRepository.findById(5)).thenReturn(Optional.of(custom));

        pcCustomService.deleteSavedBuild(5, 10);

        verify(pcCustomRepository).delete(custom);
    }

    @Test
    @DisplayName("deleteSavedBuild: Ném BusinessException khi xóa cấu hình đã gắn với đơn hàng")
    void shouldThrowBusinessExceptionWhenDeletingOrderedBuild() {
        PcCustom custom = new PcCustom();
        custom.setId(5);
        custom.setUserId(10);
        custom.setOrderId(1001);

        when(pcCustomRepository.findById(5)).thenReturn(Optional.of(custom));

        assertThatThrownBy(() -> pcCustomService.deleteSavedBuild(5, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể xóa cấu hình đã gắn liền với đơn hàng #1001");
    }
}
