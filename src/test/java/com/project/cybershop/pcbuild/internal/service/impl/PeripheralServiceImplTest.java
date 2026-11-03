package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.PeripheralQueryRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.PeripheralSuggestionResponse;
import com.project.cybershop.pcbuild.internal.PcBuildTestData;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PeripheralServiceImpl - POST /peripherals")
class PeripheralServiceImplTest {

    private final CandidateRepositoryFacade facade = Mockito.mock(CandidateRepositoryFacade.class);
    private final CatalogPublicApi catalog = Mockito.mock(CatalogPublicApi.class);
    private final PeripheralServiceImpl service = new PeripheralServiceImpl(facade, catalog);

    private final ComponentCandidateDTO cpu = PcBuildTestData.part(1, "cpu", "Ryzen", 5_000_000, Map.of());
    private final ComponentCandidateDTO gpu = PcBuildTestData.part(4, "gpu", "RTX", 18_000_000, Map.of());

    private static ComponentCandidateDTO peripheral(int id, String slug, long price, Map<Integer, String> specs) {
        return ComponentCandidateDTO.builder().variantId(id).categorySlug(slug).productName("SP " + id)
                .variantName("Tiêu chuẩn").brandName("Hãng").imageUrl("/" + id + ".png").stockQuantity(5)
                .price(BigDecimal.valueOf(price)).specValuesById(specs).build();
    }

    private static List<ComponentCandidateDTO> peripherals() {
        return List.of(
                peripheral(203, "monitor", 3_890_000, Map.of(68, "24", 69, "FHD", 70, "180", 71, "IPS")),
                peripheral(202, "monitor", 2_490_000, Map.of(68, "24", 69, "FHD", 70, "100", 71, "IPS")),
                peripheral(213, "keyboard", 690_000, Map.of(72, "MEMBRANE", 73, "PCT75", 74, "WIRELESS")),
                peripheral(224, "mouse", 490_000, Map.of(75, "6400", 76, "96", 74, "WIRED")));
    }

    private static CompatibilitySnapshot snapshot(List<ComponentCandidateDTO> candidates, List<ComponentCandidateDTO> peripherals) {
        return new CompatibilitySnapshot(candidates, Map.of(), List.of(), Map.of(),
                Map.of(69, Map.of("FHD", "Full HD (1920x1080)"), 71, Map.of("IPS", "IPS"),
                        72, Map.of("MEMBRANE", "Phím màng"), 73, Map.of("PCT75", "Nhỏ gọn (75%)"),
                        74, Map.of("WIRELESS", "Không dây", "WIRED", "Có dây")),
                Map.of(), null, null, Instant.parse("2026-11-02T00:00:00Z"), peripherals);
    }

    private static ValidateBuildRequest.ItemPart line(String role, int variantId, int quantity) {
        return ValidateBuildRequest.ItemPart.builder().role(role).variantId(variantId).quantity(quantity).build();
    }

    private static PeripheralQueryRequest request(long budget, String archetype, ValidateBuildRequest.ItemPart... items) {
        return PeripheralQueryRequest.builder().budget(BigDecimal.valueOf(budget)).archetype(archetype).items(List.of(items)).build();
    }

    private static PeripheralSuggestionResponse.RoleEntry role(PeripheralSuggestionResponse response, String role) {
        return response.getRoles().stream().filter(entry -> role.equals(entry.getRole())).findFirst().orElseThrow();
    }

    @BeforeEach
    void stubSnapshot() {
        when(facade.snapshot()).thenReturn(snapshot(List.of(cpu, gpu), peripherals()));
    }

    @Test
    @DisplayName("tính tiền từ giá server theo số lượng, trả các tổng và 3 món đúng thứ tự màn, phím, chuột")
    void returnsTotalsAndPicksInDisplayOrder() {
        PeripheralSuggestionResponse response = service.suggest(request(30_000_000, "GAMING", line("cpu", 1, 1), line("gpu", 4, 1)));

        assertThat(response.getPcTotal()).isEqualByComparingTo("23000000");
        assertThat(response.getAvailable()).isEqualByComparingTo("7000000");
        assertThat(response.getSpendLimit()).isEqualByComparingTo("5750000");
        assertThat(response.getPeripheralTotal()).isEqualByComparingTo("5070000");
        assertThat(response.getGrandTotal()).isEqualByComparingTo("28070000");
        assertThat(response.getRoles()).extracting(PeripheralSuggestionResponse.RoleEntry::getRole)
                .containsExactly("monitor", "keyboard", "mouse");
        assertThat(role(response, "monitor").getPicked().getVariantId()).isEqualTo(203);
        // Bộ máy bình thường không phải tra DB (0 SQL khi cache ấm)
        verify(catalog, never()).getCandidatesByIds(any());
    }

    @Test
    @DisplayName("số lượng được nhân vào giá thùng máy")
    void multipliesQuantity() {
        PeripheralSuggestionResponse response = service.suggest(request(40_000_000, "GENERAL", line("cpu", 1, 2), line("gpu", 4, 1)));

        assertThat(response.getPcTotal()).isEqualByComparingTo("28000000");
    }

    @Test
    @DisplayName("mỗi món trả đủ thông tin hiển thị, kèm thông số đã đổi sang nhãn tiếng Việt")
    void mapsDisplayFields() {
        PeripheralSuggestionResponse response = service.suggest(request(30_000_000, "GAMING", line("cpu", 1, 1), line("gpu", 4, 1)));

        PeripheralSuggestionResponse.Item monitor = role(response, "monitor").getPicked();
        assertThat(monitor.getProductName()).isEqualTo("SP 203");
        assertThat(monitor.getVariantName()).isEqualTo("Tiêu chuẩn");
        assertThat(monitor.getBrandName()).isEqualTo("Hãng");
        assertThat(monitor.getImageUrl()).isEqualTo("/203.png");
        assertThat(monitor.getStockQuantity()).isEqualTo(5);
        assertThat(monitor.getHighlights()).containsExactly("24 inch", "Full HD (1920x1080)", "180Hz", "IPS");
        assertThat(role(response, "keyboard").getPicked().getHighlights()).containsExactly("Phím màng", "Nhỏ gọn (75%)", "Không dây");
        assertThat(role(response, "mouse").getPicked().getHighlights()).containsExactly("6400 DPI", "96 g", "Có dây");
    }

    @Test
    @DisplayName("thông số thiếu thì bỏ khỏi highlights, không hiện chữ null")
    void skipsMissingHighlights() {
        when(facade.snapshot()).thenReturn(snapshot(List.of(cpu, gpu), List.of(
                peripheral(203, "monitor", 3_000_000, Map.of(70, "60")),
                peripheral(213, "keyboard", 300_000, Map.of()),
                peripheral(224, "mouse", 200_000, Map.of()))));

        PeripheralSuggestionResponse response = service.suggest(request(30_000_000, "GENERAL", line("cpu", 1, 1)));

        assertThat(role(response, "monitor").getPicked().getHighlights()).containsExactly("60Hz");
        assertThat(role(response, "mouse").getPicked().getHighlights()).isEmpty();
    }

    @Test
    @DisplayName("lý do: chơi game nêu tần số quét; không đủ tiền thì báo rõ; không chơi game nói theo ngân sách")
    void explainsMonitorChoice() {
        PeripheralSuggestionResponse gaming = service.suggest(request(30_000_000, "GAMING", line("cpu", 1, 1), line("gpu", 4, 1)));
        assertThat(role(gaming, "monitor").getPicked().getReason()).contains("180Hz");

        PeripheralSuggestionResponse tight = service.suggest(request(27_000_000, "GAMING", line("cpu", 1, 1), line("gpu", 4, 1)));
        assertThat(role(tight, "monitor").getPicked().getVariantId()).isEqualTo(202);
        assertThat(role(tight, "monitor").getPicked().getReason()).contains("144Hz");

        PeripheralSuggestionResponse office = service.suggest(request(30_000_000, "OFFICE", line("cpu", 1, 1), line("gpu", 4, 1)));
        assertThat(role(office, "monitor").getPicked().getReason()).doesNotContain("Hz");
        assertThat(role(office, "keyboard").getPicked().getReason()).isNotBlank();
    }

    @Test
    @DisplayName("lựa chọn thay thế gồm mọi món cùng loại, sắp theo giá, chỉ món được chọn mới có lý do")
    void listsOptionsPerRole() {
        PeripheralSuggestionResponse response = service.suggest(request(30_000_000, "GAMING", line("cpu", 1, 1), line("gpu", 4, 1)));

        List<PeripheralSuggestionResponse.Item> options = role(response, "monitor").getOptions();
        assertThat(options).extracting(PeripheralSuggestionResponse.Item::getVariantId).containsExactly(202, 203);
        assertThat(options.get(0).getReason()).isNull();
    }

    @Test
    @DisplayName("linh kiện hết hàng ngoài snapshot: tra DB rồi vẫn tính giá")
    void looksUpPartsOutsideSnapshot() {
        ComponentCandidateDTO soldOut = PcBuildTestData.part(9, "ram", "RAM", 2_000_000, Map.of());
        when(catalog.getCandidatesByIds(List.of(9))).thenReturn(List.of(soldOut));

        PeripheralSuggestionResponse response = service.suggest(request(30_000_000, "GENERAL", line("cpu", 1, 1), line("ram", 9, 1)));

        assertThat(response.getPcTotal()).isEqualByComparingTo("7000000");
    }

    @Test
    @DisplayName("biến thể không tồn tại: 422 BUILD_ITEM_NOT_FOUND")
    void rejectsUnknownVariant() {
        when(catalog.getCandidatesByIds(List.of(777))).thenReturn(List.of());

        assertThatThrownBy(() -> service.suggest(request(30_000_000, "GENERAL", line("cpu", 777, 1))))
                .isInstanceOfSatisfying(BuildRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("BUILD_ITEM_NOT_FOUND"));
    }

    @Test
    @DisplayName("client gửi màn hình như linh kiện thùng máy: 422, không tính vào giá thùng máy")
    void rejectsPeripheralSentAsBuildItem() {
        when(catalog.getCandidatesByIds(List.of(203))).thenReturn(List.of(peripheral(203, "monitor", 3_890_000, Map.of())));

        assertThatThrownBy(() -> service.suggest(request(30_000_000, "GENERAL", line("monitor", 203, 1))))
                .isInstanceOfSatisfying(BuildRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("BUILD_ITEM_NOT_FOUND"));
    }

    @Test
    @DisplayName("sản phẩm không có danh mục: 422, không tính vào giá thùng máy")
    void rejectsPartWithoutCategory() {
        ComponentCandidateDTO noCategory = ComponentCandidateDTO.builder().variantId(7).price(BigDecimal.ONE).build();
        when(catalog.getCandidatesByIds(List.of(7))).thenReturn(List.of(noCategory));

        assertThatThrownBy(() -> service.suggest(request(30_000_000, "GENERAL", line("ram", 7, 1))))
                .isInstanceOfSatisfying(BuildRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("BUILD_ITEM_NOT_FOUND"));
    }

    @Test
    @DisplayName("linh kiện chưa có giá: 422 BUILD_ITEM_NOT_FOUND")
    void rejectsPartWithoutPrice() {
        ComponentCandidateDTO noPrice = ComponentCandidateDTO.builder().variantId(8).categorySlug("ram").build();
        when(catalog.getCandidatesByIds(List.of(8))).thenReturn(List.of(noPrice));

        assertThatThrownBy(() -> service.suggest(request(30_000_000, "GENERAL", line("ram", 8, 1))))
                .isInstanceOfSatisfying(BuildRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("BUILD_ITEM_NOT_FOUND"));
    }

    @Test
    @DisplayName("kho thiếu một loại: lỗi PERIPHERALS_UNAVAILABLE lan ra để FE vẫn giữ thùng máy")
    void propagatesUnavailable() {
        when(facade.snapshot()).thenReturn(snapshot(List.of(cpu, gpu), List.of()));

        assertThatThrownBy(() -> service.suggest(request(30_000_000, "GENERAL", line("cpu", 1, 1))))
                .isInstanceOfSatisfying(BuildRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("PERIPHERALS_UNAVAILABLE"));
    }
}
