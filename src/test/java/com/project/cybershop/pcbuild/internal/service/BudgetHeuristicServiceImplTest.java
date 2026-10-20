package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.service.BudgetHeuristicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BudgetHeuristicServiceImpl - Kiểm thử thuật toán phân bổ ngân sách & lọc ứng viên")
class BudgetHeuristicServiceImplTest {

    private BudgetHeuristicServiceImpl heuristicService;

    @BeforeEach
    void setUp() {
        heuristicService = new BudgetHeuristicServiceImpl();
    }

    @Test
    @DisplayName("Phân bổ ngân sách cho mục đích 'gaming' -> GPU chiếm tỷ trọng cao nhất (35% - 48%)")
    void shouldCalculateRangesForGamingPurpose() {
        BigDecimal budget = new BigDecimal("20000000");
        Map<String, BudgetHeuristicService.PriceRange> ranges = heuristicService.calculateTargetRanges(budget, "gaming");

        assertThat(ranges).containsKey("gpu");
        assertThat(ranges.get("gpu").min()).isEqualTo(20000000 * 0.35);
        assertThat(ranges.get("gpu").max()).isEqualTo(20000000 * 0.48);
        assertThat(ranges.get("cpu").min()).isEqualTo(20000000 * 0.18);
    }

    @Test
    @DisplayName("Phân bổ ngân sách cho mục đích 'creative' -> CPU & RAM được tăng tỷ trọng")
    void shouldCalculateRangesForCreativePurpose() {
        BigDecimal budget = new BigDecimal("30000000");
        Map<String, BudgetHeuristicService.PriceRange> ranges = heuristicService.calculateTargetRanges(budget, "creative");

        assertThat(ranges.get("cpu").min()).isEqualTo(30000000 * 0.22);
        assertThat(ranges.get("ram").min()).isEqualTo(30000000 * 0.10);
    }

    @Test
    @DisplayName("Phân bổ ngân sách cho mục đích 'work' -> Tối ưu đa nhiệm văn phòng & đồ họa cơ bản")
    void shouldCalculateRangesForWorkPurpose() {
        BigDecimal budget = new BigDecimal("15000000");
        Map<String, BudgetHeuristicService.PriceRange> ranges = heuristicService.calculateTargetRanges(budget, "work");

        assertThat(ranges.get("cpu").min()).isEqualTo(15000000 * 0.25);
        assertThat(ranges.get("gpu").min()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Mục đích không xác định hoặc null -> Mặc định áp dụng cấu hình gaming")
    void shouldDefaultToGamingWhenPurposeIsUnknown() {
        BigDecimal budget = new BigDecimal("25000000");
        Map<String, BudgetHeuristicService.PriceRange> ranges = heuristicService.calculateTargetRanges(budget, "unknown_purpose");

        assertThat(ranges.get("gpu").min()).isEqualTo(25000000 * 0.35);
    }

    @Test
    @DisplayName("Lọc linh kiện: Giữ tối đa 5 sản phẩm mỗi loại nằm trong dải giá tương thích")
    void shouldFilterCandidatesWithinTargetRange() {
        List<ComponentCandidateDTO> all = new ArrayList<>();
        // Tạo 7 CPU với mức giá khác nhau
        for (int i = 1; i <= 7; i++) {
            all.add(ComponentCandidateDTO.builder()
                    .variantId(i)
                    .categorySlug("cpu")
                    .price(new BigDecimal(4_000_000 + i * 200_000)) // Nằm trong dải gaming cho budget 20M: [3.6M * 0.7, 5.2M * 1.3]
                    .build());
        }

        List<ComponentCandidateDTO> filtered = heuristicService.filterCandidates(all, new BigDecimal("20000000"), "gaming");

        assertThat(filtered).hasSize(5); // Chỉ lấy tối đa 5
    }

    @Test
    @DisplayName("Fallback: Khi không có linh kiện nào trong dải giá -> Lấy 4 sản phẩm tiêu biểu nhất")
    void shouldFallbackWhenNoCandidatesMatchRange() {
        List<ComponentCandidateDTO> all = new ArrayList<>();
        // Tạo 6 card đồ họa giá siêu rẻ (ngoài khoảng 35%-48% của 50M)
        for (int i = 1; i <= 6; i++) {
            all.add(ComponentCandidateDTO.builder()
                    .variantId(i)
                    .categorySlug("gpu")
                    .price(new BigDecimal(1_000_000 + i * 100_000))
                    .build());
        }

        List<ComponentCandidateDTO> filtered = heuristicService.filterCandidates(all, new BigDecimal("50000000"), "gaming");

        assertThat(filtered).hasSize(4); // Fallback 4 linh kiện đầu
    }

    @Test
    @DisplayName("Ưu tiên linh kiện của thương hiệu khách hàng chỉ định trong userPrompt")
    void shouldPrioritizeUserRequestedBrand() {
        List<ComponentCandidateDTO> all = new ArrayList<>();
        // 5 RAM Corsair trong tầm giá
        for (int i = 1; i <= 5; i++) {
            all.add(ComponentCandidateDTO.builder()
                    .variantId(i)
                    .brandName("Corsair")
                    .categorySlug("ram")
                    .productName("Corsair Vengeance DDR5")
                    .price(new BigDecimal(3_500_000 + i * 100_000))
                    .build());
        }
        // 1 RAM Kingston giá thấp hơn dải giá thường nhưng đúng thương hiệu khách hàng muốn
        all.add(ComponentCandidateDTO.builder()
                .variantId(99)
                .brandName("Kingston")
                .categorySlug("ram")
                .productName("Kingston FURY Beast DDR5")
                .variantName("32GB 6000MT/s")
                .price(new BigDecimal("2500000"))
                .build());

        List<ComponentCandidateDTO> filtered = heuristicService.filterCandidates(
                all,
                new BigDecimal("70000000"),
                "gaming",
                "Tôi muốn bộ PC của tôi phải có RAM của hãng Kingston",
                32,
                List.of("Kingston", "Corsair")
        );

        assertThat(filtered).anyMatch(c -> c.getVariantId().equals(99) && "Kingston".equals(c.getBrandName()));
    }
}
