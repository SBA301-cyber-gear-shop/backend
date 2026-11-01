package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.model.MetricKind;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.SpecType;
import com.project.cybershop.pcbuild.internal.service.MetadataDictionaryService;
import com.project.cybershop.pcbuild.internal.support.PerfIndexEstimator;
import com.project.cybershop.pcbuild.internal.support.ProfileMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataDictionaryServiceImpl implements MetadataDictionaryService {

    private static final List<String> ROLE_ORDER = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");
    private static final Pattern INTEGER = Pattern.compile("^\\d+$");

    // Id thông số dùng để tính khoảng khả thi của metric (DD 4.2)
    private static final int MAX_RAM_GB = 6;
    private static final int M2_SLOTS = 8;
    private static final int CAPACITY_GB = 9;
    private static final int VRAM_GB = 12;
    private static final int CORE_COUNT = 22;
    private static final int MAX_CPU_SOCKETS = 28;
    private static final int CPU_SOCKET_COUNT = 31;
    private static final int PCIE_X16_SLOTS = 32;
    private static final int MAX_GPU_QTY = 54;
    private static final int MAX_SSD_LINES = 8;

    private final ObjectMapper objectMapper;

    @Override
    public MetadataDictionaryResponse build(List<ComponentCandidateDTO> candidates,
                                            Map<Integer, ParsedSpecs> parsedByVariantId,
                                            List<CategorySpecDto> specificationCatalog,
                                            PerfIndexEstimator perfIndexEstimator) {
        Map<Integer, SpecType> typeBySpecId = inferTypes(candidates, parsedByVariantId, specificationCatalog);

        Map<String, MetadataDictionaryResponse.RoleEntry> roles = new LinkedHashMap<>();
        for (String role : ROLE_ORDER) {
            List<CategorySpecDto> rows = specificationCatalog.stream()
                    .filter(row -> role.equalsIgnoreCase(row.getCategorySlug()))
                    .toList();
            if (rows.isEmpty()) continue;
            List<ComponentCandidateDTO> roleCandidates = candidates.stream()
                    .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug()))
                    .toList();
            roles.put(role, buildRole(role, rows, roleCandidates, parsedByVariantId, typeBySpecId, perfIndexEstimator));
        }

        MetadataDictionaryResponse dictionary = MetadataDictionaryResponse.builder()
                .roles(roles)
                .metrics(buildMetrics(candidates, parsedByVariantId))
                .build();
        dictionary.setDictionaryVersion(versionOf(dictionary));
        return dictionary;
    }

    // ---------- Suy kiểu (DD 5.2) ----------

    private Map<Integer, SpecType> inferTypes(List<ComponentCandidateDTO> candidates, Map<Integer, ParsedSpecs> parsed,
                                              List<CategorySpecDto> catalog) {
        Map<Integer, Boolean> hasOptions = new HashMap<>();
        for (CategorySpecDto row : catalog) {
            hasOptions.merge(row.getSpecificationId(), !row.getOptions().isEmpty(), Boolean::logicalOr);
        }
        Map<Integer, List<String>> valuesBySpec = new HashMap<>();
        Map<Integer, List<Integer>> nonNumericVariants = new HashMap<>();
        for (ComponentCandidateDTO candidate : candidates) {
            parsedOf(candidate, parsed).raw().forEach((specId, value) -> {
                valuesBySpec.computeIfAbsent(specId, key -> new ArrayList<>()).add(value);
                if (!INTEGER.matcher(value).matches()) {
                    nonNumericVariants.computeIfAbsent(specId, key -> new ArrayList<>()).add(candidate.getVariantId());
                }
            });
        }

        Map<Integer, SpecType> types = new HashMap<>();
        for (Integer specId : hasOptions.keySet()) {
            List<String> values = valuesBySpec.getOrDefault(specId, List.of());
            List<Integer> dirty = nonNumericVariants.getOrDefault(specId, List.of());
            SpecType type;
            if (Boolean.TRUE.equals(hasOptions.get(specId))) {
                type = SpecType.ENUM;
            } else if (values.stream().anyMatch(value -> value.contains(","))) {
                type = SpecType.LIST;
            } else if (!values.isEmpty() && dirty.isEmpty()) {
                type = SpecType.INT;
            } else {
                type = SpecType.TEXT;
                if (!values.isEmpty() && dirty.size() < values.size()) {
                    // Lẽ ra là số nhưng có giá trị bẩn (ví dụ "65/88"): thông số rơi khỏi từ điển cho tới khi admin sửa
                    log.warn("Thông số specId={} có giá trị không phải số ở variantId={}, bị coi là TEXT", specId, dirty);
                }
            }
            types.put(specId, type);
        }
        return types;
    }

    // ---------- Vai trò ----------

    private MetadataDictionaryResponse.RoleEntry buildRole(String role, List<CategorySpecDto> rows,
                                                           List<ComponentCandidateDTO> roleCandidates,
                                                           Map<Integer, ParsedSpecs> parsed,
                                                           Map<Integer, SpecType> typeBySpecId,
                                                           PerfIndexEstimator estimator) {
        Map<String, MetadataDictionaryResponse.SpecEntry> specs = new LinkedHashMap<>();
        for (CategorySpecDto row : rows) {
            SpecType type = typeBySpecId.getOrDefault(row.getSpecificationId(), SpecType.TEXT);
            MetadataDictionaryResponse.SpecEntry entry = switch (type) {
                case ENUM -> enumEntry(row);
                case LIST -> listEntry(row, roleCandidates, parsed);
                case INT -> intEntry(row, roleCandidates, parsed, estimator);
                case TEXT -> null;
            };
            if (entry != null) {
                specs.put(row.getSpecificationName(), entry);
            }
        }

        List<BigDecimal> prices = roleCandidates.stream().map(ComponentCandidateDTO::getPrice)
                .filter(Objects::nonNull).sorted().toList();
        TreeSet<String> brands = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        roleCandidates.stream().map(ComponentCandidateDTO::getBrandName).filter(Objects::nonNull).forEach(brands::add);
        if (!brands.isEmpty()) {
            Map<String, String> brandValues = new LinkedHashMap<>();
            brands.forEach(brand -> brandValues.put(brand, brand));
            specs.put(ProfileMatcher.BRAND_SPEC, MetadataDictionaryResponse.SpecEntry.builder()
                    .type(SpecType.ENUM.name()).values(brandValues).build());
        }

        return MetadataDictionaryResponse.RoleEntry.builder()
                .categoryId(rows.get(0).getCategoryId())
                .primarySpecs(rows.stream().filter(CategorySpecDto::isPrimary).map(CategorySpecDto::getSpecificationName).toList())
                .label(rows.get(0).getCategoryName())
                .priceRange(prices.isEmpty() ? null : List.of(prices.get(0), prices.get(prices.size() - 1)))
                .brands(new ArrayList<>(brands))
                .specs(specs)
                .build();
    }

    private MetadataDictionaryResponse.SpecEntry enumEntry(CategorySpecDto row) {
        Map<String, String> values = new LinkedHashMap<>();
        row.getOptions().forEach(option -> values.put(option.getCode(), option.getDisplayValue()));
        return MetadataDictionaryResponse.SpecEntry.builder()
                .specId(row.getSpecificationId()).type(SpecType.ENUM.name()).values(values).build();
    }

    private MetadataDictionaryResponse.SpecEntry listEntry(CategorySpecDto row, List<ComponentCandidateDTO> roleCandidates,
                                                           Map<Integer, ParsedSpecs> parsed) {
        TreeSet<String> codes = new TreeSet<>();
        for (ComponentCandidateDTO candidate : roleCandidates) {
            String raw = parsedOf(candidate, parsed).raw().get(row.getSpecificationId());
            if (raw != null) {
                for (String code : raw.split(",")) {
                    if (!code.isBlank()) codes.add(code.trim());
                }
            }
        }
        if (codes.isEmpty()) return null;
        Map<String, String> values = new LinkedHashMap<>();
        codes.forEach(code -> values.put(code, code));
        return MetadataDictionaryResponse.SpecEntry.builder()
                .specId(row.getSpecificationId()).type(SpecType.LIST.name()).values(values).build();
    }

    private MetadataDictionaryResponse.SpecEntry intEntry(CategorySpecDto row, List<ComponentCandidateDTO> roleCandidates,
                                                          Map<Integer, ParsedSpecs> parsed, PerfIndexEstimator estimator) {
        int specId = row.getSpecificationId();
        Integer min = null;
        Integer max = null;
        for (ComponentCandidateDTO candidate : roleCandidates) {
            ParsedSpecs specs = parsedOf(candidate, parsed);
            // Perf_Index_1440p tính trên giá trị hiệu dụng (DD 6.2.1) để gồm cả card chỉ có Perf_Tier
            Integer value = specId == PerfIndexEstimator.PERF_INDEX_SPEC_ID && estimator != null
                    ? estimator.perfIndex(candidate, specs) : specs.ints().get(specId);
            if (value != null) {
                min = min == null ? value : Math.min(min, value);
                max = max == null ? value : Math.max(max, value);
            }
        }
        if (min == null) return null;
        return MetadataDictionaryResponse.SpecEntry.builder()
                .specId(specId).type(SpecType.INT.name()).unit(unitOf(row.getSpecificationName()))
                .min(min).max(max).build();
    }

    // ---------- Metric (DD 5.2) ----------

    private Map<String, MetadataDictionaryResponse.MetricEntry> buildMetrics(List<ComponentCandidateDTO> candidates,
                                                                             Map<Integer, ParsedSpecs> parsed) {
        List<ParsedSpecs> cpus = specsOf(candidates, parsed, "cpu");
        List<ParsedSpecs> boards = specsOf(candidates, parsed, "mainboard");
        List<ParsedSpecs> rams = specsOf(candidates, parsed, "ram");
        List<ParsedSpecs> gpus = specsOf(candidates, parsed, "gpu");
        List<ParsedSpecs> ssds = specsOf(candidates, parsed, "ssd");

        int maxBoardSlots = maxOf(boards, specs -> specs.ints().getOrDefault(PCIE_X16_SLOTS, 0));
        int maxBoardSockets = Math.max(1, maxOf(boards, specs -> specs.ints().getOrDefault(CPU_SOCKET_COUNT, 1)));
        int maxM2Slots = maxOf(boards, specs -> specs.ints().getOrDefault(M2_SLOTS, 0));

        Map<String, MetadataDictionaryResponse.MetricEntry> metrics = new LinkedHashMap<>();
        metrics.put(MetricKind.TOTAL_RAM_GB.name(), metric(MetricKind.TOTAL_RAM_GB,
                minOf(rams, specs -> specs.ints().get(CAPACITY_GB)),
                maxOf(boards, specs -> specs.ints().getOrDefault(MAX_RAM_GB, 0))));
        metrics.put(MetricKind.TOTAL_STORAGE_GB.name(), metric(MetricKind.TOTAL_STORAGE_GB,
                minOf(ssds, specs -> specs.ints().get(CAPACITY_GB)),
                maxOf(ssds, specs -> specs.ints().getOrDefault(CAPACITY_GB, 0)) * Math.min(MAX_SSD_LINES, maxM2Slots)));
        metrics.put(MetricKind.TOTAL_VRAM_GB.name(), metric(MetricKind.TOTAL_VRAM_GB, 0,
                maxOf(gpus, specs -> specs.ints().getOrDefault(VRAM_GB, 0) * gpuQuantityLimit(specs, maxBoardSlots))));
        metrics.put(MetricKind.GPU_COUNT.name(), metric(MetricKind.GPU_COUNT, 0,
                maxOf(gpus, specs -> gpuQuantityLimit(specs, maxBoardSlots))));
        metrics.put(MetricKind.CPU_COUNT.name(), metric(MetricKind.CPU_COUNT, 1,
                maxOf(cpus, specs -> Math.min(specs.ints().getOrDefault(MAX_CPU_SOCKETS, 1), maxBoardSockets))));
        metrics.put(MetricKind.TOTAL_CPU_CORES.name(), metric(MetricKind.TOTAL_CPU_CORES,
                minOf(cpus, specs -> specs.ints().get(CORE_COUNT)),
                maxOf(cpus, specs -> specs.ints().getOrDefault(CORE_COUNT, 0)
                        * Math.min(specs.ints().getOrDefault(MAX_CPU_SOCKETS, 1), maxBoardSockets))));
        return metrics;
    }

    // GPU_COUNT.max = max(min(PCIe_x16_Slots, Max_GPU_Qty)); thiếu Max_GPU_Qty coi là 1 (J5b)
    private static int gpuQuantityLimit(ParsedSpecs gpu, int maxBoardSlots) {
        return Math.min(maxBoardSlots, gpu.ints().getOrDefault(MAX_GPU_QTY, 1));
    }

    private static MetadataDictionaryResponse.MetricEntry metric(MetricKind kind, Integer min, Integer max) {
        return MetadataDictionaryResponse.MetricEntry.builder()
                .label(kind.label()).unit(kind.unit()).min(min).max(max).build();
    }

    // ---------- Tiện ích ----------

    static String unitOf(String specName) {
        if (specName == null) return null;
        if (specName.equals("Perf_Index_1440p")) return "%";
        if (specName.equals("Bus_Speed")) return "MT/s";
        if (specName.equals("Wattage") || specName.equals("TDP_Power") || specName.endsWith("_W")) return "W";
        if (specName.endsWith("_MM")) return "mm";
        if (specName.endsWith("_GB")) return "GB";
        if (specName.endsWith("_MHz")) return "MHz";
        if (specName.endsWith("_MBps")) return "MB/s";
        if (specName.endsWith("_TBW")) return "TBW";
        for (String suffix : List.of("_Count", "_Slots", "_Qty", "_Channels", "_Sockets")) {
            if (specName.endsWith(suffix)) return "cái";
        }
        return null;
    }

    private static ParsedSpecs parsedOf(ComponentCandidateDTO candidate, Map<Integer, ParsedSpecs> parsed) {
        ParsedSpecs specs = parsed.get(candidate.getVariantId());
        return specs != null ? specs : ParsedSpecs.of(candidate);
    }

    private static List<ParsedSpecs> specsOf(List<ComponentCandidateDTO> candidates, Map<Integer, ParsedSpecs> parsed,
                                             String role) {
        return candidates.stream()
                .filter(candidate -> role.equalsIgnoreCase(candidate.getCategorySlug()))
                .map(candidate -> parsedOf(candidate, parsed))
                .toList();
    }

    private static int maxOf(List<ParsedSpecs> specs, ToIntFunction<ParsedSpecs> value) {
        return specs.stream().mapToInt(value).max().orElse(0);
    }

    private static Integer minOf(List<ParsedSpecs> specs, java.util.function.Function<ParsedSpecs, Integer> value) {
        return specs.stream().map(value).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(0);
    }

    // Hash nội dung: dữ liệu đổi thì version đổi, cache hồ sơ tự hết hiệu lực (DD 5.6)
    private String versionOf(MetadataDictionaryResponse dictionary) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsString(dictionary).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không tính được dictionaryVersion", e);
        }
    }
}
