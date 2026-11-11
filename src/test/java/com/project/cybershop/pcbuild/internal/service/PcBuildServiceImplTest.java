package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.BuildItemRequest;
import com.project.cybershop.pcbuild.api.dto.request.UpdateSavedBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BuildItemResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.api.dto.response.SavedBuildResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.entity.PcCustomItem;
import com.project.cybershop.pcbuild.internal.model.BuildCheck;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.impl.PcBuildServiceImpl;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PcBuildServiceImplTest {

    private final BuildMetadataService buildMetadataService = mock(BuildMetadataService.class);
    private final CompatibilityEngine compatibilityEngine = mock(CompatibilityEngine.class);
    private final CompatibilityRuleRepository compatibilityRuleRepository = mock(CompatibilityRuleRepository.class);
    private final CatalogPublicApi catalogPublicApi = mock(CatalogPublicApi.class);
    private final PcCustomRepository pcCustomRepository = mock(PcCustomRepository.class);
    // listSaved / updateSaved không chia ngân sách, không tự build, không gọi AI
    private final PcBuildService service = new PcBuildServiceImpl(buildMetadataService, null, null, compatibilityEngine,
            compatibilityRuleRepository, catalogPublicApi, null, pcCustomRepository);

    @Test
    void listSavedTraGiaLucLuuVaTenTuCatalog() {
        PcCustom custom = new PcCustom();
        custom.setId(12);
        custom.setTotalPrice(new BigDecimal("8280000.00"));
        // Thêm ngược thứ tự id để kiểm tra việc xếp lại theo id
        custom.addItem(item(102, 31, "RAM", 2, "990000.00"));
        custom.addItem(item(101, 11, "CPU", 1, "6300000.00"));
        when(pcCustomRepository.findByUserIdAndOrderIdIsNullOrderByIdDesc(7)).thenReturn(List.of(custom));
        // Biến thể 31 không còn trong catalog: vẫn trả dòng, tên thay bằng mã
        when(catalogPublicApi.getCandidateMapByIds(List.of(31, 11))).thenReturn(Map.of(
                11, ComponentCandidateDTO.builder().variantId(11).productName("Ryzen 5 7500F").build()));

        List<SavedBuildResponse> builds = service.listSaved(7);

        assertEquals(1, builds.size());
        SavedBuildResponse build = builds.get(0);
        assertEquals(12, build.getBuildId());
        assertEquals(8_280_000L, build.getTotal());
        assertEquals(List.of("CPU", "RAM"), build.getItems().stream().map(BuildItemResponse::getCategory).toList());
        BuildItemResponse cpu = build.getItems().get(0);
        assertEquals("Ryzen 5 7500F", cpu.getName());
        assertEquals(6_300_000L, cpu.getPrice());
        assertNull(cpu.getMaxQty());
        BuildItemResponse ram = build.getItems().get(1);
        assertEquals("Linh kiện #31", ram.getName());
        assertEquals(2, ram.getQty());
    }

    @Test
    void listSavedChuaCoBoNaoThiTraMangRong() {
        when(pcCustomRepository.findByUserIdAndOrderIdIsNullOrderByIdDesc(7)).thenReturn(List.of());
        when(catalogPublicApi.getCandidateMapByIds(List.of())).thenReturn(Map.of());

        assertTrue(service.listSaved(7).isEmpty());
    }

    @Test
    void updateSavedBoCuaTaiKhoanKhacTra404() {
        PcCustom custom = new PcCustom();
        custom.setId(12);
        custom.setUserId(99);
        when(pcCustomRepository.findById(12)).thenReturn(Optional.of(custom));

        PcBuildException error = assertThrows(PcBuildException.class,
                () -> service.updateSaved(12, new UpdateSavedBuildRequest(List.of(new BuildItemRequest(11, 1))), 7));

        assertEquals("SAVED_BUILD_NOT_FOUND", error.getCode());
        verify(pcCustomRepository, never()).save(any());
    }

    @Test
    void updateSavedGhiDeMonVaLayGiaHienTai() {
        PcCustom custom = new PcCustom();
        custom.setId(12);
        custom.setUserId(7);
        custom.setTotalPrice(new BigDecimal("6300000.00"));
        custom.addItem(item(101, 11, "CPU", 1, "6300000.00"));
        when(pcCustomRepository.findById(12)).thenReturn(Optional.of(custom));
        when(catalogPublicApi.getCandidatesByIds(List.of(31, 12))).thenReturn(List.of(
                part(12, 1, "7000000"), part(31, 3, "1000000")));
        when(buildMetadataService.loadMetadata(List.of())).thenReturn(BuildMetadata.builder()
                .slots(List.of(slot(4, 1, "CPU"), slot(6, 3, "RAM"))).build());
        when(compatibilityEngine.checkBuild(anyLong(), anyList(), any()))
                .thenReturn(new BuildCheck(BuildStatus.VALID, 9_000_000L, 0L, List.of()));

        // RAM gửi trước CPU: dòng lưu vẫn theo step_order
        var result = service.updateSaved(12, new UpdateSavedBuildRequest(List.of(
                new BuildItemRequest(31, 2), new BuildItemRequest(12, 1))), 7);

        assertEquals(9_000_000L, result.getTotal());
        assertEquals(0, new BigDecimal("9000000").compareTo(custom.getTotalPrice()));
        assertEquals(List.of(12, 31), custom.getItems().stream().map(PcCustomItem::getProductVariantId).toList());
        assertEquals(0, new BigDecimal("7000000").compareTo(custom.getItems().get(0).getUnitPrice()));
        assertEquals(2, custom.getItems().get(1).getQuantity());
        verify(pcCustomRepository).save(custom);
    }

    private static ComponentCandidateDTO part(int variantId, int categoryId, String price) {
        return ComponentCandidateDTO.builder().variantId(variantId).categoryId(categoryId)
                .productName("Linh kiện " + variantId).price(new BigDecimal(price)).build();
    }

    private static BuildMetadata.SlotMeta slot(int stepOrder, int categoryId, String code) {
        return BuildMetadata.SlotMeta.builder().slotId(categoryId).categoryId(categoryId).componentCode(code)
                .stepOrder(stepOrder).build();
    }

    private static PcCustomItem item(int id, int variantId, String role, int quantity, String unitPrice) {
        PcCustomItem item = new PcCustomItem();
        item.setId(id);
        item.setProductVariantId(variantId);
        item.setComponentRole(role);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }
}
