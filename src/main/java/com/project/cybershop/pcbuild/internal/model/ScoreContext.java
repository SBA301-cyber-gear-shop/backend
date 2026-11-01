package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ngữ cảnh chấm điểm (DD 6.2): hồ sơ, pool, snapshot, ngân sách và preference đã resolve sẵn theo vai trò
 * (spec → id, metric → vai trò gắn điểm) để vòng lặp solver không phải tra lại.
 */
public final class ScoreContext {

    /** Preference đã resolve; đúng một trong metric hoặc specId khác null. */
    public record ResolvedPreference(String role, MetricKind metric, Integer specId, String direction,
                                     Set<String> values, double weight, Double cap) {
    }

    private final ValidatedProfile profile;
    private final CandidatePool pool;
    private final CompatibilitySnapshot snapshot;
    private final BigDecimal budget;
    private final Map<String, List<ResolvedPreference>> preferencesByRole;

    private ScoreContext(ValidatedProfile profile, CandidatePool pool, CompatibilitySnapshot snapshot, BigDecimal budget,
                         Map<String, List<ResolvedPreference>> preferencesByRole) {
        this.profile = profile;
        this.pool = pool;
        this.snapshot = snapshot;
        this.budget = budget;
        this.preferencesByRole = preferencesByRole;
    }

    /**
     * @param defaultPreferences ưu tiên mặc định của vai trò (DD 5.5) cho vai trò hồ sơ không có preference nào
     */
    public static ScoreContext of(ValidatedProfile profile, CandidatePool pool, CompatibilitySnapshot snapshot,
                                  BigDecimal budget, Function<String, List<ProfilePreferenceDto>> defaultPreferences) {
        Map<String, List<ResolvedPreference>> byRole = new HashMap<>();
        for (ProfilePreferenceDto preference : profile.profile().getPreferences()) {
            ResolvedPreference resolved = resolve(preference, snapshot);
            if (resolved != null) {
                byRole.computeIfAbsent(resolved.role(), key -> new ArrayList<>()).add(resolved);
            }
        }
        for (String role : List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler")) {
            if (!byRole.containsKey(role)) {
                List<ResolvedPreference> defaults = defaultPreferences.apply(role).stream()
                        .map(preference -> resolve(preference, snapshot))
                        .filter(java.util.Objects::nonNull)
                        .toList();
                byRole.put(role, new ArrayList<>(defaults));
            }
        }
        Map<String, List<ResolvedPreference>> frozen = byRole.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        return new ScoreContext(profile, pool, snapshot, budget, frozen);
    }

    /** Cùng preference và snapshot, pool khác (ví dụ sau khi hạ yêu cầu). */
    public ScoreContext withPool(CandidatePool otherPool) {
        return new ScoreContext(profile, otherPool, snapshot, budget, preferencesByRole);
    }

    private static ResolvedPreference resolve(ProfilePreferenceDto preference, CompatibilitySnapshot snapshot) {
        double weight = preference.getWeight() != null ? preference.getWeight() : 0;
        Set<String> values = preference.getValues() == null ? Set.of()
                : preference.getValues().stream().map(value -> value.toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        String direction = preference.getDirection() != null ? preference.getDirection() : "MAX";
        if (preference.getMetric() != null) {
            return MetricKind.parse(preference.getMetric())
                    .map(kind -> new ResolvedPreference(kind.role(), kind, null, direction, values, weight, preference.getCap()))
                    .orElse(null);
        }
        Integer specId = snapshot.specId(preference.getSpec());
        if (specId == null || preference.getRole() == null) return null;
        return new ResolvedPreference(preference.getRole(), null, specId, direction, values, weight, preference.getCap());
    }

    public List<ResolvedPreference> preferences(String role) {
        return preferencesByRole.getOrDefault(role, List.of());
    }

    public ValidatedProfile profile() {
        return profile;
    }

    public CandidatePool pool() {
        return pool;
    }

    public CompatibilitySnapshot snapshot() {
        return snapshot;
    }

    public BigDecimal budget() {
        return budget;
    }
}
