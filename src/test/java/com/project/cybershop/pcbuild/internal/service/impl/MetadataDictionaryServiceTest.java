package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.support.PerfIndexEstimator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.project.cybershop.pcbuild.internal.PcBuildTestData.part;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MetadataDictionaryService - dựng từ điển từ dữ liệu (DD 5.2)")
class MetadataDictionaryServiceTest {

    private final MetadataDictionaryServiceImpl service = new MetadataDictionaryServiceImpl(new ObjectMapper());

    private static CategorySpecDto row(int categoryId, String slug, int specId, String name, String... options) {
        List<CategorySpecDto.Option> optionList = new ArrayList<>();
        for (String option : options) {
            String[] parts = option.split("=");
            optionList.add(new CategorySpecDto.Option(parts[0], parts[1]));
        }
        return CategorySpecDto.builder().categoryId(categoryId).categorySlug(slug).categoryName("Nhóm " + slug)
                .specificationId(specId).specificationName(name).options(optionList).build();
    }

    private final List<CategorySpecDto> catalog = List.of(
            row(1, "cpu", 2, "TDP_Power"),
            row(1, "cpu", 22, "Core_Count"),
            row(1, "cpu", 28, "Max_CPU_Sockets"),
            row(2, "mainboard", 6, "Max_RAM_GB"),
            row(2, "mainboard", 31, "CPU_Socket_Count"),
            row(2, "mainboard", 32, "PCIe_x16_Slots"),
            row(3, "ram", 9, "Capacity_GB"),
            row(4, "gpu", 12, "VRAM_GB"),
            row(4, "gpu", 46, "Compute_Stack", "CUDA=NVIDIA CUDA", "ROCM=AMD ROCm"),
            row(4, "gpu", 47, "Perf_Index_1440p"),
            row(4, "gpu", 48, "Perf_Tier"),
            row(4, "gpu", 54, "Max_GPU_Qty"),
            row(4, "gpu", 64, "Price_Source"),
            row(8, "cooler", 21, "Supported_Sockets"));

    private List<ComponentCandidateDTO> candidates(String dirtyTdp) {
        return List.of(
                part(1, "cpu", "CPU A", 3_000_000, Map.of(2, "65", 22, "6", 28, "1")),
                part(2, "cpu", "CPU B", 9_000_000, Map.of(2, dirtyTdp, 22, "16", 28, "2")),
                part(3, "mainboard", "Bo 1 khe", 2_000_000, Map.of(6, "64", 31, "1", 32, "1")),
                part(4, "mainboard", "Bo 3 khe", 9_000_000, Map.of(6, "256", 31, "2", 32, "3")),
                part(5, "ram", "RAM 16", 1_000_000, Map.of(9, "16")),
                part(6, "gpu", "RTX 4GB", 4_000_000, Map.of(12, "4", 46, "CUDA", 48, "1", 54, "1", 64, "Shop A")),
                part(7, "gpu", "RTX 32GB", 180_000_000, Map.of(12, "32", 46, "CUDA", 47, "100", 48, "10", 54, "2", 64, "Shop B")),
                part(8, "gpu", "PRO 96GB", 368_000_000, Map.of(12, "96", 46, "CUDA", 48, "10", 54, "4", 64, "Shop C")),
                part(9, "cooler", "Tản", 500_000, Map.of(21, "AM4,AM5")));
    }

    private MetadataDictionaryResponse build(List<ComponentCandidateDTO> candidates) {
        Map<Integer, ParsedSpecs> parsed = new HashMap<>();
        candidates.forEach(candidate -> parsed.put(candidate.getVariantId(), ParsedSpecs.of(candidate)));
        PerfIndexEstimator estimator = PerfIndexEstimator.build(
                candidates.stream().filter(candidate -> "gpu".equals(candidate.getCategorySlug())).toList(), parsed);
        return service.build(candidates, parsed, catalog, estimator);
    }

    @Test
    @DisplayName("Suy kiểu: có option → ENUM; có dấu phẩy → LIST; số nguyên → INT với min/max; Price_Source → TEXT, không có trong từ điển")
    void infersTypes() {
        MetadataDictionaryResponse dictionary = build(candidates("88"));
        var gpu = dictionary.getRoles().get("gpu").getSpecs();

        assertThat(gpu.get("Compute_Stack").getType()).isEqualTo("ENUM");
        assertThat(gpu.get("Compute_Stack").getValues()).containsEntry("CUDA", "NVIDIA CUDA");
        assertThat(gpu.get("VRAM_GB").getType()).isEqualTo("INT");
        assertThat(gpu.get("VRAM_GB").getMin()).isEqualTo(4);
        assertThat(gpu.get("VRAM_GB").getMax()).isEqualTo(96);
        assertThat(gpu).doesNotContainKey("Price_Source");
        assertThat(dictionary.getRoles().get("cooler").getSpecs().get("Supported_Sockets").getType()).isEqualTo("LIST");
        assertThat(dictionary.getRoles().get("cooler").getSpecs().get("Supported_Sockets").getValues()).containsKeys("AM4", "AM5");
    }

    @Test
    @DisplayName("Giá trị \"65/88\" làm TDP_Power thành TEXT (biến khỏi từ điển)")
    void dirtyNumberBecomesText() {
        assertThat(build(candidates("65/88")).getRoles().get("cpu").getSpecs()).doesNotContainKey("TDP_Power")
                .containsKey("Core_Count");
    }

    @Test
    @DisplayName("Đơn vị theo hậu tố; min/max Perf_Index dùng giá trị hiệu dụng (card chỉ có tier được tính)")
    void unitsAndEffectivePerfIndex() {
        var gpu = build(candidates("88")).getRoles().get("gpu").getSpecs();
        assertThat(gpu.get("VRAM_GB").getUnit()).isEqualTo("GB");
        assertThat(gpu.get("Perf_Index_1440p").getUnit()).isEqualTo("%");
        assertThat(gpu.get("Perf_Index_1440p").getMin()).isEqualTo(10); // tier 1 → 100 × 1/10
        assertThat(gpu.get("Perf_Index_1440p").getMax()).isEqualTo(100);
        assertThat(MetadataDictionaryServiceImpl.unitOf("Wattage")).isEqualTo("W");
        assertThat(MetadataDictionaryServiceImpl.unitOf("Height_MM")).isEqualTo("mm");
        assertThat(MetadataDictionaryServiceImpl.unitOf("RAM_Slots")).isEqualTo("cái");
        assertThat(MetadataDictionaryServiceImpl.unitOf("Bus_Speed")).isEqualTo("MT/s");
    }

    @Test
    @DisplayName("Metric: GPU_COUNT.max = max(min(PCIe_x16_Slots, Max_GPU_Qty)); TOTAL_RAM_GB.max = max Max_RAM_GB")
    void metricRanges() {
        var metrics = build(candidates("88")).getMetrics();
        assertThat(metrics.get("GPU_COUNT").getMax()).isEqualTo(3);           // min(3 khe, PRO 4 card)
        assertThat(metrics.get("TOTAL_VRAM_GB").getMax()).isEqualTo(288);     // 96 × 3
        assertThat(metrics.get("TOTAL_RAM_GB").getMax()).isEqualTo(256);
        assertThat(metrics.get("CPU_COUNT").getMax()).isEqualTo(2);
        assertThat(metrics.get("TOTAL_CPU_CORES").getMax()).isEqualTo(32);
    }

    @Test
    @DisplayName("Thương hiệu, khoảng giá theo role; dictionaryVersion đổi khi dữ liệu đổi, không đổi khi dữ liệu giữ nguyên")
    void versionTracksData() {
        MetadataDictionaryResponse first = build(candidates("88"));
        MetadataDictionaryResponse same = build(candidates("88"));
        MetadataDictionaryResponse changed = build(candidates("95"));

        assertThat(first.getRoles().get("gpu").getPriceRange()).extracting(Number::longValue).containsExactly(4_000_000L, 368_000_000L);
        assertThat(first.getDictionaryVersion()).hasSize(12).isEqualTo(same.getDictionaryVersion());
        assertThat(changed.getDictionaryVersion()).isNotEqualTo(first.getDictionaryVersion());
    }
}
