package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thông số đã phân tích một lần của một linh kiện.
 * ints: số nguyên đã parse (thiếu hoặc không parse được thì không có key, ví dụ "65/88").
 * lists: giá trị tách theo dấu phẩy thành tập code viết hoa (giá trị đơn là tập một phần tử).
 * raw: chuỗi gốc đã trim để so sánh EQUALS và hiển thị.
 */
public record ParsedSpecs(Map<Integer, Integer> ints, Map<Integer, Set<String>> lists, Map<Integer, String> raw) {

    public ParsedSpecs {
        ints = Map.copyOf(ints);
        lists = Map.copyOf(lists);
        raw = Map.copyOf(raw);
    }

    public static ParsedSpecs of(ComponentCandidateDTO part) {
        Map<Integer, Integer> ints = new HashMap<>();
        Map<Integer, Set<String>> lists = new HashMap<>();
        Map<Integer, String> raw = new HashMap<>();
        Map<Integer, String> values = part.getSpecValuesById() != null ? part.getSpecValuesById() : Map.of();
        for (Map.Entry<Integer, String> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            String value = entry.getValue().trim();
            raw.put(entry.getKey(), value);
            lists.put(entry.getKey(), Arrays.stream(value.split(","))
                    .map(code -> code.trim().toUpperCase(Locale.ROOT))
                    .filter(code -> !code.isEmpty())
                    .collect(Collectors.toUnmodifiableSet()));
            try {
                ints.put(entry.getKey(), Integer.valueOf(value));
            } catch (NumberFormatException ignored) {
                // Không phải số nguyên: chỉ giữ ở raw/lists
            }
        }
        return new ParsedSpecs(ints, lists, raw);
    }

    public boolean has(int specId) {
        return raw.containsKey(specId);
    }
}
