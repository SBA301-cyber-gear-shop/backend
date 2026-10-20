package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildDetailResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildSummaryResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.entity.PcCustomItem;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.HardwareCompatibilityValidator;
import com.project.cybershop.pcbuild.internal.service.PcCustomService;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PcCustomServiceImpl - Kiểm thử lưu cấu hình và lọc linh kiện thay thế thông minh")
class PcCustomServiceImplTest {

    @Mock
    private PcCustomRepository pcCustomRepository;

    @Mock
    private CatalogPublicApi catalogPublicApi;

    @Mock
    private HardwareCompatibilityValidator validator;

    @InjectMocks
    private PcCustomServiceImpl pcCustomService;

    @Test
    @DisplayName("Lưu cấu hình thành công: Gán userId cho PcCustom")
    void shouldSaveBuildForUserSuccessfully() {
        PcCustom custom = new PcCustom();
        custom.setId(10);

        when(pcCustomRepository.findById(10)).thenReturn(Optional.of(custom));

        pcCustomService.saveBuildForUser(10, 1);

        assertThat(custom.getUserId()).isEqualTo(1);
        verify(pcCustomRepository).save(custom);
    }

    @Test
    @DisplayName("Lưu mới cấu hình từ request: Tạo PcCustom và PcCustomItems gắn với userId")
    void shouldSaveNewBuildSuccessfully() {
        var req = com.project.cybershop.pcbuild.api.dto.request.SavePcBuildRequest.builder()
                .buildName("Gaming Rig")
                .totalPrice(new BigDecimal("15000000"))
                .items(List.of(
                        com.project.cybershop.pcbuild.api.dto.request.SavePcBuildRequest.SavePartItem.builder()
                                .variantId(101).role("cpu").quantity(1).unitPrice(new BigDecimal("5000000")).build(),
                        com.project.cybershop.pcbuild.api.dto.request.SavePcBuildRequest.SavePartItem.builder()
                                .variantId(201).role("mainboard").quantity(1).unitPrice(new BigDecimal("3000000")).build()
                ))
                .build();

        when(pcCustomRepository.save(any())).thenAnswer(inv -> {
            PcCustom c = inv.getArgument(0);
            c.setId(88);
            return c;
        });

        Integer savedId = pcCustomService.saveNewBuild(req, 15);

        assertThat(savedId).isEqualTo(88);
        verify(pcCustomRepository).save(any(PcCustom.class));
    }

    @Test
    @DisplayName("Lưu cấu hình thất bại khi ID không tồn tại -> Ném ResourceNotFoundException")
    void shouldThrowExceptionWhenBuildNotFound() {
        when(pcCustomRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pcCustomService.saveBuildForUser(99, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy cấu hình");
    }

    @Test
    @DisplayName("Lọc linh kiện thay thế: Đánh dấu tương thích chính xác dựa trên Socket CPU và Mainboard")
    void shouldReturnReplacementsWithCompatibilityFlag() {
        ComponentCandidateDTO currentCpu = ComponentCandidateDTO.builder()
                .variantId(101)
                .categorySlug("cpu")
                .specs(Map.of("Socket", "LGA1700"))
                .build();

        ComponentCandidateDTO mbCompat = ComponentCandidateDTO.builder()
                .variantId(201)
                .categorySlug("mainboard")
                .productName("Mainboard LGA1700")
                .price(new BigDecimal("3000000"))
                .specs(Map.of("Socket", "LGA1700"))
                .build();

        ComponentCandidateDTO mbIncompat = ComponentCandidateDTO.builder()
                .variantId(202)
                .categorySlug("mainboard")
                .productName("Mainboard AM5")
                .price(new BigDecimal("2500000"))
                .specs(Map.of("Socket", "AM5"))
                .build();

        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(List.of(mbCompat, mbIncompat));
        when(catalogPublicApi.getCandidateMapByIds(any())).thenReturn(Map.of(101, currentCpu));

        List<CompatibleReplacementResponse> res = pcCustomService.getCompatibleReplacements("mainboard", 101, null);

        assertThat(res).hasSize(2);
        // Linh kiện tương thích được xếp trước
        assertThat(res.get(0).getVariantId()).isEqualTo(201);
        assertThat(res.get(0).isCompatible()).isTrue();
        assertThat(res.get(1).getVariantId()).isEqualTo(202);
        assertThat(res.get(1).isCompatible()).isFalse();
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
