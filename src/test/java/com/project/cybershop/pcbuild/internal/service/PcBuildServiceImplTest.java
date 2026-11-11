package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.BuildItemResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedBuildResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.entity.PcCustomItem;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.impl.PcBuildServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PcBuildServiceImplTest {

    private final CatalogPublicApi catalogPublicApi = mock(CatalogPublicApi.class);
    private final PcCustomRepository pcCustomRepository = mock(PcCustomRepository.class);
    // listSaved chỉ đọc repository và catalog, các phụ thuộc còn lại không dùng tới
    private final PcBuildService service = new PcBuildServiceImpl(null, null, null, null, null, catalogPublicApi,
            null, pcCustomRepository);

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
