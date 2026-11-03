package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.internal.model.CandidatePool;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.ScoreContext;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.PartScorer;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dựng CandidatePool theo hồ sơ (DD 6.1) từ snapshot, thuần bộ nhớ: không đọc DB, không gọi snapshot().
 * Tách khỏi CandidateRepositoryFacade để BuildSolver không phụ thuộc lớp sở hữu DB (nguyên tắc 7, D24).
 */
@Component
public class CandidatePoolBuilder {

    // id danh mục 1–8 = 8 vai trò cpu, mainboard, ram, gpu, ssd, psu, case, cooler
    public static final List<String> BUILD_ROLES = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");
    private static final int CATEGORY_CASE = 7;
    private static final int CATEGORY_COOLER = 8;
    private static final int MAX_GPU_LENGTH_MM = 16;
    private static final int MAX_COOLER_HEIGHT_MM = 17;
    private static final int HEIGHT_MM = 19;
    private static final int COOLER_TYPE = 20;

    private final PartScorer partScorer;
    private final ProfilePresets profilePresets;

    public CandidatePoolBuilder(PartScorer partScorer, ProfilePresets profilePresets) {
        this.partScorer = partScorer;
        this.profilePresets = profilePresets;
    }

    /** Lọc ứng viên theo constraint, thương hiệu tránh, ngân sách và thông số bắt buộc; sắp xếp tất định theo điểm. */
    public CandidatePool build(CompatibilitySnapshot snapshot, ValidatedProfile profile, BigDecimal budget) {
        Map<String, List<ComponentCandidateDTO>> filtered = new LinkedHashMap<>();
        for (String role : BUILD_ROLES) {
            List<ComponentCandidateDTO> pool = new ArrayList<>();
            if (!profile.isExcluded(role)) {
                List<ProfileConstraintDto> constraints = profile.constraints().stream()
                        .filter(constraint -> role.equals(constraint.getRole())).toList();
                List<String> avoided = profile.profile().getBrands().getAvoid();
                for (ComponentCandidateDTO candidate : snapshot.candidates()) {
                    if (!role.equalsIgnoreCase(candidate.getCategorySlug())) continue;
                    if (!hasRequiredSpecs(candidate, snapshot)) continue;
                    if (!constraints.stream().allMatch(constraint -> ProfileMatcher.matches(constraint, candidate, snapshot))) continue;
                    if (candidate.getBrandName() != null && avoided.stream().anyMatch(candidate.getBrandName()::equalsIgnoreCase)) continue;
                    if (budget != null && (candidate.getPrice() == null || candidate.getPrice().compareTo(budget) > 0)) continue;
                    pool.add(candidate);
                }
            }
            filtered.put(role, pool);
        }

        Map<String, Map<Integer, int[]>> ranges = new HashMap<>();
        filtered.forEach((role, pool) -> ranges.put(role, rangesOf(pool, snapshot)));
        CandidatePool unsorted = new CandidatePool(filtered, ranges);

        // Mỗi role sắp theo (s giảm dần, price tăng dần, variantId tăng dần) để mọi vòng lặp duyệt tất định (DD 6.4.4)
        ScoreContext ctx = ScoreContext.of(profile, unsorted, snapshot, budget, profilePresets::defaultPreferences);
        Map<String, List<ComponentCandidateDTO>> sorted = new LinkedHashMap<>();
        filtered.forEach((role, pool) -> {
            Map<Integer, Double> scores = new HashMap<>();
            pool.forEach(candidate -> scores.put(candidate.getVariantId(), partScorer.partScore(role, candidate, ctx)));
            // MAX_PRICE: sắp theo giá giảm dần vì solver chỉ thử một số món đầu danh sách
            Comparator<ComponentCandidateDTO> order = profile.maxPrice()
                    ? Comparator.comparing(ComponentCandidateDTO::getPrice).reversed()
                    : Comparator.comparing((ComponentCandidateDTO candidate) -> -scores.get(candidate.getVariantId()))
                            .thenComparing(ComponentCandidateDTO::getPrice);
            sorted.put(role, pool.stream().sorted(order.thenComparing(ComponentCandidateDTO::getVariantId)).toList());
        });
        return new CandidatePool(sorted, ranges);
    }

    // DD 4.3 + J10: thiếu Max_GPU_Length/Max_Cooler_Height của vỏ và Height_MM của tản nước được chấp nhận ở đây;
    // engine (mode SOLVER) mới quyết định build cụ thể có dùng được vỏ đó không
    private static boolean hasRequiredSpecs(ComponentCandidateDTO candidate, CompatibilitySnapshot snapshot) {
        ParsedSpecs specs = snapshot.parsed(candidate);
        Integer categoryId = candidate.getCategoryId();
        for (Integer specId : snapshot.requiredSpecIds(categoryId)) {
            if (specs.has(specId)) continue;
            boolean caseClearance = categoryId != null && categoryId == CATEGORY_CASE
                    && (specId == MAX_GPU_LENGTH_MM || specId == MAX_COOLER_HEIGHT_MM);
            boolean liquidHeight = categoryId != null && categoryId == CATEGORY_COOLER && specId == HEIGHT_MM
                    && "Liquid".equalsIgnoreCase(specs.raw().get(COOLER_TYPE));
            if (!caseClearance && !liquidHeight) return false;
        }
        return true;
    }

    private static Map<Integer, int[]> rangesOf(List<ComponentCandidateDTO> pool, CompatibilitySnapshot snapshot) {
        Map<Integer, int[]> ranges = new HashMap<>();
        for (ComponentCandidateDTO candidate : pool) {
            Set<Integer> specIds = new HashSet<>(snapshot.parsed(candidate).ints().keySet());
            specIds.add(PerfIndexEstimator.PERF_INDEX_SPEC_ID);
            for (Integer specId : specIds) {
                Integer value = snapshot.effectiveInt(candidate, specId);
                if (value == null) continue;
                ranges.merge(specId, new int[]{value, value},
                        (current, next) -> new int[]{Math.min(current[0], next[0]), Math.max(current[1], next[1])});
            }
        }
        return ranges;
    }
}
