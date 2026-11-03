package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.support.PerfIndexEstimator;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bản chụp bất biến của dữ liệu tương thích (DD 7.1, 11.4). Nạp tối đa một lần mỗi 60 giây bởi
 * CandidateRepositoryFacade; engine, solver và từ điển chỉ đọc từ đây nên không phát sinh câu SQL nào.
 */
public record CompatibilitySnapshot(
        List<ComponentCandidateDTO> candidates,                  // ACTIVE, stock > 0, thuộc 8 vai trò
        Map<Integer, ParsedSpecs> parsedSpecsByVariantId,        // spec đã phân tích sẵn của các ứng viên trên
        List<RuleDefinition> rules,                              // bản sao compatibility_rules
        Map<Integer, Set<Integer>> requiredSpecIdsByCategoryId,  // category_specifications.is_compatibility = true (DD 4.3)
        Map<Integer, Map<String, String>> optionDisplayBySpecId, // specId → (code → display_value)
        Map<Integer, String> specNameById,                       // specId → tên thông số
        MetadataDictionaryResponse dictionary,                   // DD 5.2; null trong test engine dựng tay
        PerfIndexEstimator perfIndexEstimator,                   // DD 6.2.1; null = dùng số đo gốc
        Instant loadedAt,
        List<ComponentCandidateDTO> peripherals                  // ACTIVE, stock > 0, màn/phím/chuột; ngoài 8 vai trò nên solver và engine không dùng
) {

    /** Bản không có màn, phím, chuột (test engine và solver không cần). */
    public CompatibilitySnapshot(List<ComponentCandidateDTO> candidates, Map<Integer, ParsedSpecs> parsedSpecsByVariantId,
                                 List<RuleDefinition> rules, Map<Integer, Set<Integer>> requiredSpecIdsByCategoryId,
                                 Map<Integer, Map<String, String>> optionDisplayBySpecId, Map<Integer, String> specNameById,
                                 MetadataDictionaryResponse dictionary, PerfIndexEstimator perfIndexEstimator, Instant loadedAt) {
        this(candidates, parsedSpecsByVariantId, rules, requiredSpecIdsByCategoryId, optionDisplayBySpecId, specNameById,
                dictionary, perfIndexEstimator, loadedAt, List.of());
    }

    public CompatibilitySnapshot {
        candidates = List.copyOf(candidates);
        peripherals = List.copyOf(peripherals);
        parsedSpecsByVariantId = Map.copyOf(parsedSpecsByVariantId);
        rules = List.copyOf(rules);
        requiredSpecIdsByCategoryId = requiredSpecIdsByCategoryId.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
        optionDisplayBySpecId = optionDisplayBySpecId.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
        specNameById = Map.copyOf(specNameById);
    }

    /** Spec đã phân tích; linh kiện ngoài snapshot (ví dụ món hết hàng của build đã lưu) được phân tích tại chỗ. */
    public ParsedSpecs parsed(ComponentCandidateDTO part) {
        ParsedSpecs cached = part.getVariantId() != null ? parsedSpecsByVariantId.get(part.getVariantId()) : null;
        return cached != null ? cached : ParsedSpecs.of(part);
    }

    /** Giá trị số hiệu dụng: Perf_Index_1440p dùng giá trị ước lượng từ Perf_Tier khi card thiếu số đo (DD 6.2.1). */
    public Integer effectiveInt(ComponentCandidateDTO part, int specId) {
        ParsedSpecs specs = parsed(part);
        if (specId == PerfIndexEstimator.PERF_INDEX_SPEC_ID && perfIndexEstimator != null) {
            return perfIndexEstimator.perfIndex(part, specs);
        }
        return specs.ints().get(specId);
    }

    public Optional<ComponentCandidateDTO> candidate(Integer variantId) {
        return candidates.stream().filter(c -> c.getVariantId().equals(variantId)).findFirst();
    }

    public Set<Integer> requiredSpecIds(Integer categoryId) {
        return categoryId != null ? requiredSpecIdsByCategoryId.getOrDefault(categoryId, Set.of()) : Set.of();
    }

    public String specName(int specId) {
        return specNameById.getOrDefault(specId, "#" + specId);
    }

    /** Id thông số theo tên (không phân biệt hoa thường); null nếu không có. */
    public Integer specId(String name) {
        if (name == null) return null;
        return specNameById.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(name.trim()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    public String dictionaryVersion() {
        return dictionary != null ? dictionary.getDictionaryVersion() : null;
    }

    /** display_value của option nếu có, ngược lại trả chính giá trị gốc. */
    public String display(int specId, String value) {
        if (value == null) return null;
        Map<String, String> options = optionDisplayBySpecId.get(specId);
        return options != null ? options.getOrDefault(value, value) : value;
    }
}
