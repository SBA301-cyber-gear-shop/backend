package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.service.BudgetHeuristicService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class BudgetHeuristicServiceImpl implements BudgetHeuristicService {

    @Override
    public Map<String, PriceRange> calculateTargetRanges(BigDecimal budget, String purpose) {
        double b = budget.doubleValue();
        Map<String, PriceRange> ranges = new HashMap<>();

        if ("creative".equalsIgnoreCase(purpose)) {
            ranges.put("cpu", new PriceRange(b * 0.22, b * 0.32));
            ranges.put("gpu", new PriceRange(b * 0.25, b * 0.35));
            ranges.put("mainboard", new PriceRange(b * 0.10, b * 0.15));
            ranges.put("ram", new PriceRange(b * 0.10, b * 0.18));
            ranges.put("ssd", new PriceRange(b * 0.06, b * 0.12));
            ranges.put("psu", new PriceRange(b * 0.05, b * 0.09));
            ranges.put("case", new PriceRange(b * 0.03, b * 0.06));
            ranges.put("cooler", new PriceRange(b * 0.03, b * 0.07));
        } else if ("work".equalsIgnoreCase(purpose)) {
            ranges.put("cpu", new PriceRange(b * 0.25, b * 0.38));
            ranges.put("mainboard", new PriceRange(b * 0.12, b * 0.20));
            ranges.put("ram", new PriceRange(b * 0.10, b * 0.18));
            ranges.put("ssd", new PriceRange(b * 0.08, b * 0.15));
            ranges.put("psu", new PriceRange(b * 0.06, b * 0.12));
            ranges.put("case", new PriceRange(b * 0.04, b * 0.08));
            ranges.put("cooler", new PriceRange(b * 0.02, b * 0.05));
            ranges.put("gpu", new PriceRange(0, b * 0.20));
        } else { // Gaming mặc định
            ranges.put("gpu", new PriceRange(b * 0.35, b * 0.48));
            ranges.put("cpu", new PriceRange(b * 0.18, b * 0.26));
            ranges.put("mainboard", new PriceRange(b * 0.10, b * 0.15));
            ranges.put("ram", new PriceRange(b * 0.06, b * 0.12));
            ranges.put("ssd", new PriceRange(b * 0.05, b * 0.09));
            ranges.put("psu", new PriceRange(b * 0.05, b * 0.09));
            ranges.put("case", new PriceRange(b * 0.03, b * 0.07));
            ranges.put("cooler", new PriceRange(b * 0.02, b * 0.05));
        }
        return ranges;
    }

    @Override
    public List<ComponentCandidateDTO> filterCandidates(List<ComponentCandidateDTO> all, BigDecimal budget, String purpose) {
        Map<String, PriceRange> ranges = calculateTargetRanges(budget, purpose);
        List<ComponentCandidateDTO> result = new ArrayList<>();

        Map<String, List<ComponentCandidateDTO>> byCategory = new HashMap<>();
        for (ComponentCandidateDTO c : all) {
            byCategory.computeIfAbsent(c.getCategorySlug(), k -> new ArrayList<>()).add(c);
        }

        for (Map.Entry<String, List<ComponentCandidateDTO>> entry : byCategory.entrySet()) {
            String cat = entry.getKey();
            List<ComponentCandidateDTO> list = entry.getValue();
            PriceRange range = ranges.get(cat);

            // Sắp xếp theo giá tăng dần
            list.sort(Comparator.comparing(ComponentCandidateDTO::getPrice));

            if (range != null) {
                List<ComponentCandidateDTO> matched = list.stream()
                        .filter(p -> p.getPrice().doubleValue() >= range.min() * 0.7 
                                  && p.getPrice().doubleValue() <= range.max() * 1.3)
                        .limit(5)
                        .toList();
                if (!matched.isEmpty()) {
                    result.addAll(matched);
                    continue;
                }
            }
            // Fallback lấy 4 sản phẩm tiêu biểu nhất
            result.addAll(list.stream().limit(4).toList());
        }
        return result;
    }
}
