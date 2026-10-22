package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.service.BudgetHeuristicService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
        return filterCandidates(all, budget, purpose, null, null, null);
    }

    @Override
    public List<ComponentCandidateDTO> filterCandidates(List<ComponentCandidateDTO> all, BigDecimal budget, String purpose, String userPrompt, Integer targetRamGb) {
        return filterCandidates(all, budget, purpose, userPrompt, targetRamGb, null);
    }

    @Override
    public List<ComponentCandidateDTO> filterCandidates(List<ComponentCandidateDTO> all, BigDecimal budget, String purpose, String userPrompt, Integer targetRamGb, List<String> availableBrandNames) {
        Map<String, PriceRange> ranges = calculateTargetRanges(budget, purpose);
        List<ComponentCandidateDTO> result = new ArrayList<>();

        // Danh sách thương hiệu động: ưu tiên lấy từ DB, fallback trích xuất từ chính candidates
        List<String> knownBrands;
        if (availableBrandNames != null && !availableBrandNames.isEmpty()) {
            knownBrands = new ArrayList<>(availableBrandNames);
        } else {
            knownBrands = all.stream()
                    .map(ComponentCandidateDTO::getBrandName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        String promptLower = userPrompt != null ? userPrompt.toLowerCase(Locale.ROOT) : "";
        List<String> requestedBrands = knownBrands.stream()
                .filter(b -> {
                    String bLower = b.toLowerCase(Locale.ROOT);
                    if (promptLower.contains(bLower)) return true;
                    if ("western digital".equals(bLower) && (promptLower.contains("wd") || promptLower.contains("w.d"))) return true;
                    return false;
                })
                .toList();

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

            Set<Integer> addedVariantIds = new HashSet<>();
            List<ComponentCandidateDTO> catResult = new ArrayList<>();

            // 1. Ưu tiên hàng đầu: Các linh kiện thuộc thương hiệu khách hàng chỉ định (khớp brandName từ DB hoặc productName)
            if (!requestedBrands.isEmpty()) {
                for (ComponentCandidateDTO c : list) {
                    String cBrandLower = c.getBrandName() != null ? c.getBrandName().toLowerCase(Locale.ROOT) : "";
                    String pNameLower = c.getProductName() != null ? c.getProductName().toLowerCase(Locale.ROOT) : "";
                    boolean brandMatch = requestedBrands.stream()
                            .anyMatch(b -> {
                                String bLower = b.toLowerCase(Locale.ROOT);
                                return (!cBrandLower.isEmpty() && (cBrandLower.contains(bLower) || bLower.contains(cBrandLower)))
                                        || pNameLower.contains(bLower)
                                        || ("western digital".equals(bLower) && (cBrandLower.contains("wd") || pNameLower.contains("wd")));
                            });
                    if (brandMatch && c.getPrice().compareTo(budget) <= 0) {
                        catResult.add(c);
                        addedVariantIds.add(c.getVariantId());
                        if (catResult.size() >= 3) break;
                    }
                }
            }

            // 2. Với RAM: Ưu tiên thêm biến thể khớp dung lượng targetRamGb nếu có
            if ("ram".equals(cat) && targetRamGb != null && targetRamGb > 0) {
                String ramTargetStr = targetRamGb + "gb";
                for (ComponentCandidateDTO c : list) {
                    if (addedVariantIds.contains(c.getVariantId())) continue;
                    String vName = c.getVariantName() != null ? c.getVariantName().toLowerCase(Locale.ROOT) : "";
                    String pName = c.getProductName() != null ? c.getProductName().toLowerCase(Locale.ROOT) : "";
                    String capSpec = c.getSpecs() != null ? c.getSpecs().get("Capacity_GB") : null;
                    boolean capMatch = String.valueOf(targetRamGb).equals(capSpec)
                            || vName.contains(ramTargetStr)
                            || pName.contains(ramTargetStr);
                    if (capMatch && c.getPrice().compareTo(budget) <= 0) {
                        catResult.add(c);
                        addedVariantIds.add(c.getVariantId());
                        if (catResult.size() >= 5) break;
                    }
                }
            }

            // 3. Lọc theo dải ngân sách Heuristic
            int maxPerCat = (promptLower.isEmpty() && targetRamGb == null) ? 5 : 8;
            if (range != null) {
                int remainingSlots = maxPerCat - catResult.size();
                if (remainingSlots > 0) {
                    List<ComponentCandidateDTO> matched = list.stream()
                            .filter(p -> p.getPrice().doubleValue() >= range.min() * 0.7 
                                      && p.getPrice().doubleValue() <= range.max() * 1.3)
                            .filter(p -> !addedVariantIds.contains(p.getVariantId()))
                            .limit(remainingSlots)
                            .toList();
                    for (ComponentCandidateDTO m : matched) {
                        catResult.add(m);
                        addedVariantIds.add(m.getVariantId());
                    }
                }
            }

            // 4. Fallback nếu vẫn chưa có đủ linh kiện
            if (catResult.isEmpty()) {
                int fallbackLimit = (promptLower.isEmpty() && targetRamGb == null) ? 4 : 5;
                catResult.addAll(list.stream().limit(fallbackLimit).toList());
            } else if (catResult.size() < 4) {
                for (ComponentCandidateDTO c : list) {
                    if (!addedVariantIds.contains(c.getVariantId())) {
                        catResult.add(c);
                        addedVariantIds.add(c.getVariantId());
                        if (catResult.size() >= 4) break;
                    }
                }
            }

            result.addAll(catResult);
        }
        return result;
    }
}
