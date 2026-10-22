package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.HardwareCompatibilityValidator;
import com.project.cybershop.pcbuild.internal.service.PcCustomService;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
