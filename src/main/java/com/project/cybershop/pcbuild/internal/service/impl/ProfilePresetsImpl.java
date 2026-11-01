package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.pcbuild.api.dto.profile.BalancePairDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileRoleRefDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.BuildTier;
import com.project.cybershop.pcbuild.internal.model.MetricKind;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Bảng hồ sơ mẫu của DD 5.5 [QĐTK, D27], ghi dưới dạng dữ liệu; mỗi ô đúng một giá trị. */
@Service
public class ProfilePresetsImpl implements ProfilePresets {

    static final List<String> ROLE_ORDER = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");
    private static final List<String> MIDDLE_TIERS_AND_UP = List.of("T3", "T4", "T5", "T6", "T7");

    private static final Map<Archetype, double[]> ROLE_WEIGHTS = new EnumMap<>(Map.of(
            //                        cpu   mainboard ram  gpu   ssd   psu   case  cooler
            Archetype.GAMING, new double[]{0.22, 0.05, 0.10, 0.45, 0.08, 0.04, 0.03, 0.03},
            Archetype.CREATIVE, new double[]{0.28, 0.05, 0.22, 0.25, 0.12, 0.03, 0.02, 0.03},
            Archetype.AI_DATA, new double[]{0.12, 0.04, 0.18, 0.50, 0.10, 0.03, 0.01, 0.02},
            Archetype.OFFICE, new double[]{0.30, 0.10, 0.20, 0.00, 0.25, 0.05, 0.05, 0.05},
            Archetype.DEV, new double[]{0.32, 0.08, 0.30, 0.05, 0.15, 0.04, 0.03, 0.03},
            Archetype.GENERAL, new double[]{0.25, 0.07, 0.15, 0.25, 0.15, 0.05, 0.04, 0.04}));

    // Cap (bão hòa) mẫu, không đổi theo mức; gắn vào preference cùng spec hoặc metric
    private static final Map<Archetype, Map<String, Double>> CAPS = new EnumMap<>(Map.of(
            Archetype.GAMING, Map.of("Core_Count", 16.0, "TOTAL_RAM_GB", 64.0, "TOTAL_STORAGE_GB", 4000.0, "VRAM_GB", 32.0),
            Archetype.CREATIVE, Map.of("Core_Count", 128.0, "TOTAL_RAM_GB", 512.0, "GPU_COUNT", 2.0),
            Archetype.AI_DATA, Map.of("TOTAL_RAM_GB", 2048.0, "GPU_COUNT", 4.0),
            Archetype.OFFICE, Map.of("Core_Count", 8.0, "Boost_Clock_MHz", 5100.0, "TOTAL_RAM_GB", 32.0, "TOTAL_STORAGE_GB", 2000.0),
            Archetype.DEV, Map.of("Core_Count", 16.0, "TOTAL_RAM_GB", 128.0),
            Archetype.GENERAL, Map.of("TOTAL_RAM_GB", 64.0, "TOTAL_STORAGE_GB", 2000.0)));

    @Override
    public Map<String, Double> roleWeights(Archetype archetype) {
        double[] weights = ROLE_WEIGHTS.get(archetype);
        Map<String, Double> result = new LinkedHashMap<>();
        for (int index = 0; index < ROLE_ORDER.size(); index++) {
            result.put(ROLE_ORDER.get(index), weights[index]);
        }
        return result;
    }

    @Override
    public List<ProfilePreferenceDto> defaultPreferences(String role) {
        switch (role) {
            case "cpu":
                return List.of(spec("cpu", "Core_Count", "MAX", 0.5), spec("cpu", "Boost_Clock_MHz", "MAX", 0.5));
            case "mainboard":
                return List.of(spec("mainboard", "M2_Slots", "MAX", 0.4), spec("mainboard", "RAM_Slots", "MAX", 0.3),
                        prefer("mainboard", "Has_WiFi", 0.3, "YES"));
            case "ram":
                return List.of(metric("TOTAL_RAM_GB", 0.8, null), spec("ram", "Bus_Speed", "MAX", 0.2));
            case "gpu":
                return List.of(spec("gpu", "Perf_Index_1440p", "MAX", 0.8), spec("gpu", "VRAM_GB", "MAX", 0.2));
            case "ssd":
                return List.of(metric("TOTAL_STORAGE_GB", 0.7, null),
                        prefer("ssd", "SSD_Interface", 0.3, "NVME_GEN4", "NVME_GEN5"));
            case "psu":
                return List.of(prefer("psu", "Efficiency_Rating", 0.6, "GOLD", "PLATINUM", "TITANIUM"),
                        prefer("psu", "ATX_Version", 0.4, "ATX30", "ATX31"));
            case "case":
                return List.of(spec("case", "Max_GPU_Length_MM", "MAX", 0.5), spec("case", "Max_Radiator_MM", "MAX", 0.5));
            case "cooler":
                // shortcut: tháp cao hơn coi là tản tốt hơn, cho tới khi có thông số định mức TDP của tản
                return List.of(spec("cooler", "Radiator_Size_MM", "MAX", 0.5), spec("cooler", "Height_MM", "MAX", 0.5));
            default:
                return List.of();
        }
    }

    private List<ProfilePreferenceDto> archetypePreferences(Archetype archetype, String role) {
        if ("gpu".equals(role) && archetype == Archetype.CREATIVE) {
            return List.of(spec("gpu", "Perf_Index_1440p", "MAX", 0.6), spec("gpu", "VRAM_GB", "MAX", 0.2),
                    metric("GPU_COUNT", 0.2, 2.0));
        }
        if ("gpu".equals(role) && archetype == Archetype.AI_DATA) {
            return List.of(metric("TOTAL_VRAM_GB", 0.6, null), spec("gpu", "Perf_Index_1440p", "MAX", 0.2),
                    metric("GPU_COUNT", 0.2, 4.0));
        }
        return defaultPreferences(role);
    }

    @Override
    public RequirementProfileDto preset(Archetype archetype, BuildTier tier) {
        String reason = "Hồ sơ mẫu " + archetype.name() + " " + tier.name();
        String level = tier.name();
        List<ProfileRoleRefDto> required = new ArrayList<>();
        List<ProfileRoleRefDto> excluded = new ArrayList<>();
        List<ProfileConstraintDto> constraints = new ArrayList<>();
        List<ProfileMetricDto> metrics = new ArrayList<>();
        List<BalancePairDto> balancePairs = new ArrayList<>();
        List<String> unmetNeeds = new ArrayList<>();

        // Bước 2: ma trận theo mức, thứ tự hàng từ trên xuống (DD 5.5)
        switch (archetype) {
            case GAMING -> {
                if (tier != BuildTier.T1) required.add(roleRef("gpu", reason));
                Integer tierFloor = pick(level, null, null, 3, 5, 8, 8, 8);
                if (tierFloor != null) constraints.add(intConstraint("gpu", "Perf_Tier", "GTE", tierFloor, reason));
                metrics.add(metricConstraint("TOTAL_RAM_GB", "GTE", pick(level, 8, 16, 32, 32, 32, 32, 32), reason));
                if (tier != BuildTier.T1) metrics.add(metricConstraint("GPU_COUNT", "EQ", 1, reason));
                if (tier != BuildTier.T1) balancePairs.add(new BalancePairDto("cpu", "gpu", 0.2));
            }
            case CREATIVE -> {
                if (MIDDLE_TIERS_AND_UP.contains(level)) required.add(roleRef("gpu", reason));
                Integer vram = pick(level, null, null, 8, 12, 16, 16, 16);
                if (vram != null) constraints.add(intConstraint("gpu", "VRAM_GB", "GTE", vram, reason));
                Integer ram = pick(level, null, 32, 32, 64, 96, 128, 128);
                if (ram != null) metrics.add(metricConstraint("TOTAL_RAM_GB", "GTE", ram, reason));
                Integer cores = pick(level, null, 6, 6, 8, 8, 16, 16);
                if (cores != null) constraints.add(intConstraint("cpu", "Core_Count", "GTE", cores, reason));
            }
            case AI_DATA -> {
                if (MIDDLE_TIERS_AND_UP.contains(level)) {
                    required.add(roleRef("gpu", reason));
                    constraints.add(ProfileConstraintDto.builder().role("gpu").spec("Compute_Stack").op("IN")
                            .values(new ArrayList<>(List.of("CUDA", "ROCM"))).priority(2).reason(reason).build());
                }
                Integer vram = pick(level, null, null, 12, 16, 16, 32, 96);
                if (vram != null) metrics.add(metricConstraint("TOTAL_VRAM_GB", "GTE", vram, reason));
                Integer ram = pick(level, null, null, 32, 32, 64, 64, 128);
                if (ram != null) metrics.add(metricConstraint("TOTAL_RAM_GB", "GTE", ram, reason));
                if (!MIDDLE_TIERS_AND_UP.contains(level)) unmetNeeds.add("Ngân sách chỉ đủ chạy model AI nhỏ bằng CPU");
            }
            case OFFICE -> {
                excluded.add(roleRef("gpu", reason));
                metrics.add(metricConstraint("TOTAL_RAM_GB", "GTE", tier == BuildTier.T1 ? 8 : 16, reason));
            }
            case DEV -> {
                Integer ram = pick(level, null, 16, 32, 64, 64, 64, 64);
                if (ram != null) metrics.add(metricConstraint("TOTAL_RAM_GB", "GTE", ram, reason));
                Integer cores = pick(level, null, null, 6, 12, 12, 12, 12);
                if (cores != null) constraints.add(intConstraint("cpu", "Core_Count", "GTE", cores, reason));
            }
            case GENERAL -> {
                // không có ràng buộc ở mọi mức
            }
        }

        // Bước 1, 3, 4: trọng số, preference theo vai trò (bỏ vai trò trọng số 0), gắn cap
        Map<String, Double> weights = roleWeights(archetype);
        List<ProfilePreferenceDto> preferences = new ArrayList<>();
        for (String role : ROLE_ORDER) {
            if (weights.get(role) > 0) {
                archetypePreferences(archetype, role).forEach(preference -> preferences.add(preference.toBuilder().build()));
            }
        }
        applyCaps(preferences, archetype);

        // Bước 5: đánh id theo thứ tự thêm
        numberIds(constraints, metrics, preferences);

        // Bước 6: các trường còn lại
        return RequirementProfileDto.builder()
                .profileVersion(1).source("AI").archetype(archetype.name())
                .summary(reason).useCases(new ArrayList<>())
                .roleWeights(weights)
                .requiredRoles(required).excludedRoles(excluded)
                .constraints(constraints).metrics(metrics).preferences(preferences)
                .balancePairs(balancePairs)
                .brands(new RequirementProfileDto.Brands())
                .unmetNeeds(unmetNeeds)
                .confidence(1.0).clarifyingQuestion(null)
                .droppedItems(new ArrayList<>())
                .build();
    }

    @Override
    public RequirementProfileDto fillMissing(RequirementProfileDto profile, BuildTier tier) {
        Archetype archetype = Archetype.parse(profile.getArchetype());
        RequirementProfileDto filled = profile.toBuilder()
                .roleWeights(normalizedWeightKeys(nullSafe(profile.getRoleWeights())))
                .preferences(new ArrayList<>(nullSafe(profile.getPreferences())))
                .build();

        boolean noWeights = filled.getRoleWeights().values().stream().noneMatch(weight -> weight != null && weight > 0);
        if (noWeights) {
            filled.setRoleWeights(roleWeights(archetype));
        } else {
            fillWeightGaps(filled, archetype);
        }

        // Vai trò có trọng số nhưng chưa có preference nào (theo spec của role hoặc metric gắn role) → lấy từ hồ sơ mẫu
        for (String role : ROLE_ORDER) {
            Double weight = filled.getRoleWeights().get(role);
            if (weight == null || weight <= 0 || hasPreferenceFor(filled.getPreferences(), role)) continue;
            archetypePreferences(archetype, role).forEach(preference -> filled.getPreferences().add(preference.toBuilder().build()));
        }

        // Preference cùng spec/metric chưa có cap → gắn cap mẫu
        Map<String, Double> caps = CAPS.get(archetype);
        for (ProfilePreferenceDto preference : filled.getPreferences()) {
            String target = preference.targetsMetric() ? preference.getMetric() : preference.getSpec();
            if (preference.getCap() == null && target != null && caps.containsKey(target)) {
                preference.setCap(caps.get(target));
            }
        }
        return filled;
    }

    /** Khóa vai trò về chữ thường để không có hai khóa song song ("GPU" và "gpu"); khóa lạ giữ nguyên cho validator báo. */
    private static Map<String, Double> normalizedWeightKeys(Map<String, Double> weights) {
        Map<String, Double> normalized = new LinkedHashMap<>();
        weights.forEach((key, weight) -> {
            String role = key != null ? key.trim().toLowerCase(Locale.ROOT) : null;
            if (!ROLE_ORDER.contains(role)) {
                normalized.put(key, weight);
            } else if (weight != null) {
                normalized.put(role, weight);
            }
        });
        return normalized;
    }

    /**
     * Gemini thường bỏ khóa trong roleWeights; khóa thiếu bị coi là trọng số 0 và solver không nâng cấp vai trò đó.
     * Khóa thiếu → lấy từ hồ sơ mẫu. Khóa có giá trị, kể cả 0, là chủ đích của AI nên giữ, trừ khi vai trò đang được
     * yêu cầu: khi đó trọng số 0 là tự mâu thuẫn và lấy từ mẫu (mẫu cũng 0 thì lấy GENERAL).
     */
    private void fillWeightGaps(RequirementProfileDto filled, Archetype archetype) {
        Map<String, Double> weights = filled.getRoleWeights();
        Map<String, Double> preset = roleWeights(archetype);
        Map<String, Double> general = roleWeights(Archetype.GENERAL);
        for (String role : ROLE_ORDER) {
            if (!weights.containsKey(role)) {
                weights.put(role, preset.get(role));
            }
            if (weights.get(role) <= 0 && isDemanded(filled, role)) {
                weights.put(role, preset.get(role) > 0 ? preset.get(role) : general.get(role));
            }
        }
    }

    /** Hồ sơ đòi có vai trò này: qua requiredRoles, metric cần giá trị dương, hoặc constraint; excludedRoles thắng tất cả. */
    private static boolean isDemanded(RequirementProfileDto profile, String role) {
        if (nullSafe(profile.getExcludedRoles()).stream().anyMatch(ref -> role.equalsIgnoreCase(trimmed(ref.getRole())))) {
            return false;
        }
        return nullSafe(profile.getRequiredRoles()).stream().anyMatch(ref -> role.equalsIgnoreCase(trimmed(ref.getRole())))
                || nullSafe(profile.getConstraints()).stream().anyMatch(item -> role.equalsIgnoreCase(trimmed(item.getRole())))
                || nullSafe(profile.getMetrics()).stream().anyMatch(item -> demandsRole(item, role));
    }

    private static boolean demandsRole(ProfileMetricDto metric, String role) {
        boolean positive = metric.getValue() != null && metric.getValue() > 0
                && ("GTE".equalsIgnoreCase(trimmed(metric.getOp())) || "EQ".equalsIgnoreCase(trimmed(metric.getOp())));
        return positive && MetricKind.parse(metric.getMetric()).map(kind -> kind.role().equals(role)).orElse(false);
    }

    private static String trimmed(String value) {
        return value != null ? value.trim() : null;
    }

    private static boolean hasPreferenceFor(List<ProfilePreferenceDto> preferences, String role) {
        return preferences.stream().anyMatch(preference -> preference.targetsMetric()
                ? MetricKind.parse(preference.getMetric()).map(kind -> kind.role().equals(role)).orElse(false)
                : role.equalsIgnoreCase(preference.getRole()));
    }

    private static void applyCaps(List<ProfilePreferenceDto> preferences, Archetype archetype) {
        Map<String, Double> caps = CAPS.get(archetype);
        for (ProfilePreferenceDto preference : preferences) {
            String target = preference.getMetric() != null ? preference.getMetric() : preference.getSpec();
            if (caps.containsKey(target)) {
                preference.setCap(caps.get(target));
            }
        }
    }

    private static void numberIds(List<ProfileConstraintDto> constraints, List<ProfileMetricDto> metrics,
                                  List<ProfilePreferenceDto> preferences) {
        for (int index = 0; index < constraints.size(); index++) constraints.get(index).setId("c" + (index + 1));
        for (int index = 0; index < metrics.size(); index++) metrics.get(index).setId("m" + (index + 1));
        for (int index = 0; index < preferences.size(); index++) preferences.get(index).setId("p" + (index + 1));
    }

    /** Giá trị của ô theo mức T1..T7; null = "—". */
    private static Integer pick(String tier, Integer... valuesByTier) {
        return valuesByTier[Integer.parseInt(tier.substring(1)) - 1];
    }

    private static ProfileRoleRefDto roleRef(String role, String reason) {
        return ProfileRoleRefDto.builder().role(role).priority(2).reason(reason).build();
    }

    private static ProfileConstraintDto intConstraint(String role, String spec, String op, int value, String reason) {
        return ProfileConstraintDto.builder().role(role).spec(spec).op(op).value((double) value)
                .values(new ArrayList<>()).priority(2).reason(reason).build();
    }

    private static ProfileMetricDto metricConstraint(String metric, String op, int value, String reason) {
        return ProfileMetricDto.builder().metric(metric).op(op).value((double) value).priority(2).reason(reason).build();
    }

    private static ProfilePreferenceDto spec(String role, String spec, String direction, double weight) {
        return ProfilePreferenceDto.builder().role(role).spec(spec).direction(direction).weight(weight)
                .values(new ArrayList<>()).build();
    }

    private static ProfilePreferenceDto prefer(String role, String spec, double weight, String... values) {
        return ProfilePreferenceDto.builder().role(role).spec(spec).direction("PREFER").weight(weight)
                .values(new ArrayList<>(List.of(values))).build();
    }

    private static ProfilePreferenceDto metric(String metric, double weight, Double cap) {
        return ProfilePreferenceDto.builder().metric(metric).direction("MAX").weight(weight).cap(cap)
                .values(new ArrayList<>()).build();
    }

    private static <T> T nullSafe(T value, T fallback) {
        return Objects.requireNonNullElse(value, fallback);
    }

    private static Map<String, Double> nullSafe(Map<String, Double> value) {
        return nullSafe(value, Map.of());
    }

    private static <T> List<T> nullSafe(List<T> value) {
        return nullSafe(value, List.of());
    }
}
