package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.pcbuild.api.dto.profile.BalancePairDto;
import com.project.cybershop.pcbuild.api.dto.profile.DroppedItemDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileRoleRefDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.MetricKind;
import com.project.cybershop.pcbuild.internal.model.SpecType;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import com.project.cybershop.pcbuild.internal.service.ProfileValidator;
import com.project.cybershop.pcbuild.internal.support.ProfileMatcher;
import com.project.cybershop.pcbuild.internal.support.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileValidatorImpl implements ProfileValidator {

    private static final Set<String> ROLES = Set.copyOf(ProfilePresetsImpl.ROLE_ORDER);
    private static final Set<String> REQUIRABLE_ROLES = Set.of("gpu", "cooler");   // ngoài 6 vai trò lõi (DD 5.3)
    private static final Set<String> EXCLUDABLE_ROLES = Set.of("gpu");
    private static final Set<String> INT_OPS = Set.of("GTE", "LTE", "EQ");
    private static final Set<String> ENUM_OPS = Set.of("IN", "NOT_IN");
    private static final Set<String> LIST_OPS = Set.of("CONTAINS");
    private static final int MAX_CONSTRAINTS = 20;
    private static final int MAX_METRICS = 10;
    private static final int MAX_PREFERENCES = 20;
    private static final int MAX_TEXT = 200;
    private static final int MAX_USE_CASES = 5;
    private static final int MAX_UNMET_NEEDS = 10;

    private final ProfilePresets profilePresets;

    @Override
    public ValidatedProfile validate(RequirementProfileDto input, MetadataDictionaryResponse dictionary, String source,
                                     Integer legacyRamGb) {
        RequirementProfileDto in = input != null ? input : new RequirementProfileDto();
        Run run = new Run(dictionary, source);
        Archetype archetype = Archetype.parse(in.getArchetype());

        RequirementProfileDto out = RequirementProfileDto.builder()
                .profileVersion(1)
                .source(source)
                .archetype(archetype.name())
                .summary(truncate(in.getSummary()))
                .useCases(truncateAll(in.getUseCases(), MAX_USE_CASES))
                .unmetNeeds(truncateAll(in.getUnmetNeeds(), MAX_UNMET_NEEDS))
                .objective(ValidatedProfile.MAX_PRICE.equals(in.getObjective()) ? ValidatedProfile.MAX_PRICE : null)
                .confidence(in.getConfidence() == null ? 1.0 : clamp(in.getConfidence()))
                .clarifyingQuestion(truncate(in.getClarifyingQuestion()))
                .build();

        out.setRoleWeights(run.roleWeights(in.getRoleWeights(), archetype));
        out.setRequiredRoles(run.roleRefs(in.getRequiredRoles(), REQUIRABLE_ROLES, "required"));
        out.setExcludedRoles(run.roleRefs(in.getExcludedRoles(), EXCLUDABLE_ROLES, "excluded"));
        out.setConstraints(run.constraints(nullSafe(in.getConstraints())));
        out.setMetrics(run.metrics(nullSafe(in.getMetrics()), legacyRamGb));
        out.setPreferences(run.preferences(nullSafe(in.getPreferences())));
        out.setBalancePairs(run.balancePairs(nullSafe(in.getBalancePairs())));
        out.setBrands(run.brands(in.getBrands()));
        // Đã bắt buộc hãng cho một linh kiện thì không cộng điểm hãng đó cho cả bộ máy nữa
        out.getBrands().getPrefer().removeIf(brand -> out.getConstraints().stream().anyMatch(item ->
                ProfileMatcher.BRAND_SPEC.equals(item.getSpec()) && "IN".equals(item.getOp()) && item.getValues().contains(brand)));
        if (!run.missingBrands.isEmpty()) {
            run.missingBrands.addAll(out.getUnmetNeeds());
            out.setUnmetNeeds(truncateAll(run.missingBrands, MAX_UNMET_NEEDS));
        }
        run.resolveRoleConflicts(out);
        run.implyRequiredRoles(out);
        out.setDroppedItems(run.dropped);
        return new ValidatedProfile(out, archetype, run.warnings);
    }

    /** Trạng thái của một lần kiểm: droppedItems, warnings và bộ đếm id. */
    private final class Run {
        private final MetadataDictionaryResponse dictionary;
        private final String source;
        private final List<DroppedItemDto> dropped = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> softBrands = new ArrayList<>();
        private final List<String> missingBrands = new ArrayList<>();

        private Run(MetadataDictionaryResponse dictionary, String source) {
            this.dictionary = dictionary;
            this.source = source;
        }

        private void drop(String id, String reason, String detail) {
            dropped.add(DroppedItemDto.builder().id(id).reason(reason).detail(truncate(detail)).build());
        }

        // V8: ép về [0, 1], chuẩn hóa tổng = 1; tổng = 0 thì lấy trọng số hồ sơ mẫu
        private Map<String, Double> roleWeights(Map<String, Double> weights, Archetype archetype) {
            Map<String, Double> clamped = new LinkedHashMap<>();
            Map<String, Double> given = weights != null ? weights : Map.of();
            for (String role : ProfilePresetsImpl.ROLE_ORDER) {
                Double value = findIgnoreCase(given, role);
                clamped.put(role, value == null || value.isNaN() ? 0.0 : clamp(value));
            }
            given.keySet().stream()
                    .filter(key -> key == null || !ROLES.contains(key.trim().toLowerCase(Locale.ROOT)))
                    .forEach(key -> drop("roleWeights." + key, "UNKNOWN_ROLE", "Vai trò không tồn tại: " + key));
            double sum = clamped.values().stream().mapToDouble(Double::doubleValue).sum();
            if (sum <= 0) {
                return new LinkedHashMap<>(profilePresets.roleWeights(archetype));
            }
            Map<String, Double> normalized = new LinkedHashMap<>();
            clamped.forEach((role, value) -> normalized.put(role, value / sum));
            return normalized;
        }

        private List<ProfileRoleRefDto> roleRefs(List<ProfileRoleRefDto> refs, Set<String> allowed, String kind) {
            List<ProfileRoleRefDto> result = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (ProfileRoleRefDto ref : nullSafe(refs)) {
                String role = normalizeRole(ref.getRole());
                if (!ROLES.contains(role) || !allowed.contains(role)) {
                    drop(kind + ":" + ref.getRole(), "UNKNOWN_ROLE", "Không áp dụng " + kind + " cho vai trò " + ref.getRole());
                } else if (seen.add(role)) {
                    result.add(ProfileRoleRefDto.builder().role(role).priority(priority(ref.getPriority()))
                            .reason(truncate(ref.getReason())).build());
                }
            }
            return result;
        }

        private List<ProfileConstraintDto> constraints(List<ProfileConstraintDto> items) {
            List<ProfileConstraintDto> valid = new ArrayList<>();
            List<String> ids = assignIds(items.stream().map(ProfileConstraintDto::getId).toList(), "c");
            for (int index = 0; index < items.size(); index++) {
                ProfileConstraintDto item = items.get(index);
                String id = ids.get(index);
                String role = normalizeRole(item.getRole());
                MetadataDictionaryResponse.RoleEntry roleEntry = dictionary.getRoles().get(role);
                if (roleEntry == null) {
                    drop(id, "UNKNOWN_ROLE", "Vai trò không tồn tại: " + item.getRole());
                    continue;
                }
                Optional<Map.Entry<String, MetadataDictionaryResponse.SpecEntry>> spec = findSpec(roleEntry, item.getSpec());
                if (spec.isEmpty()) {
                    drop(id, "UNKNOWN_SPEC", "Thông số " + item.getSpec() + " không có trong metadata của " + role);
                    continue;
                }
                String specName = spec.get().getKey();
                MetadataDictionaryResponse.SpecEntry entry = spec.get().getValue();
                SpecType type = SpecType.valueOf(entry.getType());
                String op = normalizeOp(id, type, upper(item.getOp()), item.getValues());
                if (!opsFor(type).contains(op)) {
                    drop(id, "OP_TYPE_MISMATCH", "Toán tử " + item.getOp() + " không dùng được với thông số " + type + " " + specName);
                    continue;
                }
                ProfileConstraintDto.ProfileConstraintDtoBuilder result = ProfileConstraintDto.builder()
                        .id(id).role(role).spec(specName).op(op).priority(priority(item.getPriority()))
                        .reason(truncate(item.getReason())).values(new ArrayList<>());
                if (type == SpecType.INT) {
                    if (!isInteger(item.getValue())) {
                        drop(id, "NOT_A_NUMBER", "Giá trị của " + specName + " phải là số nguyên");
                        continue;
                    }
                    double value = item.getValue();
                    boolean unsatisfiable = isOutOfRange(op, value, entry.getMin(), entry.getMax());
                    if (unsatisfiable) {
                        warnings.add("CONSTRAINT_UNSATISFIABLE:" + id);
                    }
                    result.value(value).unsatisfiable(unsatisfiable ? Boolean.TRUE : null)
                            .label(label(specName, op, value, entry.getUnit()));
                } else {
                    List<String> codes = matchValues(id, entry, item.getValues());
                    if (codes.isEmpty()) {
                        drop(id, "UNKNOWN_VALUE", "Không có giá trị hợp lệ cho " + specName);
                        // Khách đòi hãng kho không có: phải nói rõ, không để khách tưởng bộ máy đã đúng hãng
                        if (ProfileMatcher.BRAND_SPEC.equals(specName) && "IN".equals(op)) {
                            missingBrands.add("Cửa hàng chưa có " + roleEntry.getLabel() + " hãng "
                                    + String.join(", ", nullSafe(item.getValues())) + ", hệ thống đã chọn hãng khác");
                        }
                        continue;
                    }
                    String label = enumLabel(specName, op, codes, entry);
                    result.values(codes).label(ProfileMatcher.BRAND_SPEC.equals(specName) ? roleEntry.getLabel() + " · " + label : label);
                }
                valid.add(result.build());
            }
            return capCount(resolveConflicts(valid, item -> item.getRole() + "." + item.getSpec(),
                    ProfileConstraintDto::getPriority, ProfileConstraintDto::getId, this::constraintInterval,
                    ProfileConstraintDto::getOp, ProfileConstraintDto::getValues), MAX_CONSTRAINTS,
                    ProfileConstraintDto::getPriority, ProfileConstraintDto::getId);
        }

        /**
         * Gemini thật hay viết "Color EQ [WHITE]" cho ENUM: cùng nghĩa với IN, nên chuẩn hóa thay vì bỏ mục, để không mất
         * yêu cầu khách nói rõ (priority 1). Tương tự EQ/IN trên LIST thành CONTAINS. Toán tử khác vẫn bị V3 bỏ.
         */
        private String normalizeOp(String id, SpecType type, String op, List<String> values) {
            boolean hasValues = values != null && !values.isEmpty();
            String normalized = op;
            if (type == SpecType.ENUM && hasValues && Set.of("EQ", "CONTAINS").contains(op)) {
                normalized = "IN";
            } else if (type == SpecType.LIST && hasValues && Set.of("EQ", "IN").contains(op)) {
                normalized = "CONTAINS";
            }
            if (!normalized.equals(op)) {
                warnings.add("OP_NORMALIZED:" + id + ":" + op + "→" + normalized);
            }
            return normalized;
        }

        private List<ProfileMetricDto> metrics(List<ProfileMetricDto> items, Integer legacyRamGb) {
            List<ProfileMetricDto> valid = new ArrayList<>();
            List<String> ids = assignIds(items.stream().map(ProfileMetricDto::getId).toList(), "m");
            for (int index = 0; index < items.size(); index++) {
                ProfileMetricDto item = items.get(index);
                String id = ids.get(index);
                Optional<MetricKind> kind = MetricKind.parse(item.getMetric());
                if (kind.isEmpty()) {
                    drop(id, "UNKNOWN_METRIC", "Chỉ số " + item.getMetric() + " không được hỗ trợ");
                    continue;
                }
                String op = upper(item.getOp());
                if (!INT_OPS.contains(op)) {
                    drop(id, "OP_TYPE_MISMATCH", "Toán tử " + item.getOp() + " không dùng được với chỉ số");
                    continue;
                }
                if (!isInteger(item.getValue())) {
                    drop(id, "NOT_A_NUMBER", "Giá trị của " + item.getMetric() + " phải là số nguyên");
                    continue;
                }
                valid.add(metric(id, kind.get(), op, item.getValue(), priority(item.getPriority()), truncate(item.getReason())));
            }

            // V14: ramGb cũ thành metric priority 1, thay mọi ràng buộc TOTAL_RAM_GB GTE khác
            if (legacyRamGb != null) {
                valid.removeIf(item -> MetricKind.TOTAL_RAM_GB.name().equals(item.getMetric()) && "GTE".equals(item.getOp()));
                Set<String> usedIds = valid.stream().map(ProfileMetricDto::getId).collect(Collectors.toSet());
                int counter = 1;
                while (usedIds.contains("m" + counter)) counter++;
                String id = "m" + counter;
                valid.add(metric(id, MetricKind.TOTAL_RAM_GB, "GTE", legacyRamGb.doubleValue(), 1,
                        "Khách chọn RAM tối thiểu " + legacyRamGb + " GB"));
            }
            return capCount(resolveConflicts(valid, ProfileMetricDto::getMetric, ProfileMetricDto::getPriority,
                    ProfileMetricDto::getId, item -> interval(item.getOp(), item.getValue()), ProfileMetricDto::getOp,
                    item -> List.of()), MAX_METRICS, ProfileMetricDto::getPriority, ProfileMetricDto::getId);
        }

        private ProfileMetricDto metric(String id, MetricKind kind, String op, double value, int priority, String reason) {
            MetadataDictionaryResponse.MetricEntry range = dictionary.getMetrics().get(kind.name());
            boolean unsatisfiable = range != null && isOutOfRange(op, value, range.getMin(), range.getMax());
            if (unsatisfiable) {
                warnings.add("CONSTRAINT_UNSATISFIABLE:" + id);
            }
            return ProfileMetricDto.builder().id(id).metric(kind.name()).op(op).value(value).priority(priority)
                    .reason(reason).unsatisfiable(unsatisfiable ? Boolean.TRUE : null)
                    .label(kind.label() + " " + symbol(op) + " " + formatNumber(value) + (kind.unit() != null ? " " + kind.unit() : ""))
                    .build();
        }

        private List<ProfilePreferenceDto> preferences(List<ProfilePreferenceDto> items) {
            List<ProfilePreferenceDto> valid = new ArrayList<>();
            List<String> ids = assignIds(items.stream().map(ProfilePreferenceDto::getId).toList(), "p");
            for (int index = 0; index < items.size(); index++) {
                ProfilePreferenceDto item = items.get(index);
                String id = ids.get(index);
                // Hãng được chấm điểm qua brands.prefer, không qua preference (Brand không có trong specification_values)
                if (ProfileMatcher.BRAND_SPEC.equalsIgnoreCase(String.valueOf(item.getSpec()).trim())) {
                    softBrands.addAll(nullSafe(item.getValues()));
                    continue;
                }
                String direction = upper(item.getDirection());
                double weight = item.getWeight() == null || item.getWeight().isNaN() ? 0.5 : clamp(item.getWeight());
                ProfilePreferenceDto.ProfilePreferenceDtoBuilder result = ProfilePreferenceDto.builder()
                        .id(id).direction(direction).weight(weight).reason(truncate(item.getReason())).values(new ArrayList<>());

                if (item.targetsMetric()) {
                    Optional<MetricKind> kind = MetricKind.parse(item.getMetric());
                    if (kind.isEmpty()) {
                        drop(id, "UNKNOWN_METRIC", "Chỉ số " + item.getMetric() + " không được hỗ trợ");
                        continue;
                    }
                    if (!Set.of("MAX", "MIN").contains(direction)) {
                        drop(id, "OP_TYPE_MISMATCH", "Chỉ số chỉ dùng MAX hoặc MIN");
                        continue;
                    }
                    MetadataDictionaryResponse.MetricEntry range = dictionary.getMetrics().get(kind.get().name());
                    valid.add(result.metric(kind.get().name()).cap(capOf(item.getCap(), range != null ? range.getMax() : null)).build());
                    continue;
                }

                String role = normalizeRole(item.getRole());
                MetadataDictionaryResponse.RoleEntry roleEntry = dictionary.getRoles().get(role);
                if (roleEntry == null) {
                    drop(id, "UNKNOWN_ROLE", "Vai trò không tồn tại: " + item.getRole());
                    continue;
                }
                Optional<Map.Entry<String, MetadataDictionaryResponse.SpecEntry>> spec = findSpec(roleEntry, item.getSpec());
                if (spec.isEmpty()) {
                    drop(id, "UNKNOWN_SPEC", "Thông số " + item.getSpec() + " không có trong metadata của " + role);
                    continue;
                }
                MetadataDictionaryResponse.SpecEntry entry = spec.get().getValue();
                SpecType type = SpecType.valueOf(entry.getType());
                boolean numeric = type == SpecType.INT;
                if (numeric != Set.of("MAX", "MIN").contains(direction) || (!numeric && !"PREFER".equals(direction))) {
                    drop(id, "OP_TYPE_MISMATCH", "Hướng " + item.getDirection() + " không dùng được với thông số " + type);
                    continue;
                }
                result.role(role).spec(spec.get().getKey());
                if (numeric) {
                    result.cap(capOf(item.getCap(), entry.getMax()));
                } else {
                    List<String> codes = matchValues(id, entry, item.getValues());
                    if (codes.isEmpty()) {
                        drop(id, "UNKNOWN_VALUE", "Không có giá trị hợp lệ cho " + spec.get().getKey());
                        continue;
                    }
                    result.values(codes);
                }
                valid.add(result.build());
            }
            // V13: preference không có priority nên cắt theo trọng số giảm dần
            List<ProfilePreferenceDto> sorted = valid.stream()
                    .sorted(Comparator.comparing(ProfilePreferenceDto::getWeight).reversed()).toList();
            sorted.stream().skip(MAX_PREFERENCES)
                    .forEach(item -> drop(item.getId(), "TOO_MANY_ITEMS", "Vượt quá " + MAX_PREFERENCES + " preference"));
            Set<String> kept = sorted.stream().limit(MAX_PREFERENCES).map(ProfilePreferenceDto::getId).collect(Collectors.toSet());
            return valid.stream().filter(item -> kept.contains(item.getId())).collect(Collectors.toCollection(ArrayList::new));
        }

        private List<BalancePairDto> balancePairs(List<BalancePairDto> pairs) {
            List<BalancePairDto> result = new ArrayList<>();
            for (BalancePairDto pair : pairs) {
                String first = normalizeRole(pair.getA());
                String second = normalizeRole(pair.getB());
                if (!ROLES.contains(first) || !ROLES.contains(second) || first.equals(second)) {
                    drop("balance:" + pair.getA() + "-" + pair.getB(), "UNKNOWN_ROLE", "Cặp cân đối không hợp lệ");
                    continue;
                }
                double gap = pair.getMaxGap() == null ? 0.2 : clamp(pair.getMaxGap());
                result.add(new BalancePairDto(first, second, gap));
            }
            return result;
        }

        // V10: thương hiệu phải có trong từ điển, lấy đúng tên chuẩn
        private RequirementProfileDto.Brands brands(RequirementProfileDto.Brands brands) {
            Map<String, String> available = new LinkedHashMap<>();
            dictionary.getRoles().values().forEach(role -> nullSafe(role.getBrands())
                    .forEach(brand -> available.putIfAbsent(TextNormalizer.normalize(brand), brand)));
            RequirementProfileDto.Brands result = new RequirementProfileDto.Brands();
            List<String> prefer = new ArrayList<>(softBrands);
            if (brands != null) {
                prefer.addAll(nullSafe(brands.getPrefer()));
                result.setAvoid(matchBrands(brands.getAvoid(), available));
            }
            result.setPrefer(matchBrands(prefer, available));
            return result;
        }

        private List<String> matchBrands(List<String> names, Map<String, String> available) {
            Set<String> result = new LinkedHashSet<>();
            for (String name : nullSafe(names)) {
                String brand = available.get(TextNormalizer.normalize(name));
                if (brand == null) {
                    drop("brand:" + name, "BRAND_NOT_AVAILABLE", "Cửa hàng chưa có thương hiệu " + name);
                } else {
                    result.add(brand);
                }
            }
            return new ArrayList<>(result);
        }

        /**
         * Ràng buộc "tích cực" trên gpu/cooler (≥, =, IN, CONTAINS; metric GPU_COUNT ≥ 1, TOTAL_VRAM_GB > 0) chỉ có nghĩa khi
         * build có vai trò đó. Gemini thật hay bỏ quên requiredRoles, khiến solver hạ yêu cầu bằng cách bỏ hẳn GPU của máy chơi
         * game. Vì vậy suy ra requiredRoles với priority nhỏ nhất của các mục liên quan (không áp khi vai trò bị loại).
         */
        private void implyRequiredRoles(RequirementProfileDto profile) {
            for (String role : REQUIRABLE_ROLES.stream().sorted().toList()) {
                if (profile.getRequiredRoles().stream().anyMatch(ref -> role.equals(ref.getRole()))) continue;
                if (profile.getExcludedRoles().stream().anyMatch(ref -> role.equals(ref.getRole()))) continue;
                Integer priority = null;
                String reason = null;
                for (ProfileConstraintDto constraint : profile.getConstraints()) {
                    if (role.equals(constraint.getRole()) && Set.of("GTE", "EQ", "IN", "CONTAINS").contains(constraint.getOp())
                            && (priority == null || constraint.getPriority() < priority)) {
                        priority = constraint.getPriority();
                        reason = constraint.getLabel();
                    }
                }
                if ("gpu".equals(role)) {
                    for (ProfileMetricDto metric : profile.getMetrics()) {
                        boolean needsGpu = (MetricKind.GPU_COUNT.name().equals(metric.getMetric())
                                && Set.of("GTE", "EQ").contains(metric.getOp()) && metric.getValue() >= 1)
                                || (MetricKind.TOTAL_VRAM_GB.name().equals(metric.getMetric())
                                && Set.of("GTE", "EQ").contains(metric.getOp()) && metric.getValue() > 0);
                        if (needsGpu && (priority == null || metric.getPriority() < priority)) {
                            priority = metric.getPriority();
                            reason = metric.getLabel();
                        }
                    }
                }
                if (priority != null) {
                    profile.getRequiredRoles().add(ProfileRoleRefDto.builder().role(role).priority(priority)
                            .reason("Suy ra từ yêu cầu " + reason).build());
                    warnings.add("REQUIRED_ROLE_IMPLIED:" + role);
                }
            }
        }

        // V12: vai trò vừa bắt buộc vừa bị loại → giữ mục priority 1, không có thì bỏ cả hai
        private void resolveRoleConflicts(RequirementProfileDto profile) {
            for (ProfileRoleRefDto excluded : new ArrayList<>(profile.getExcludedRoles())) {
                Optional<ProfileRoleRefDto> required = profile.getRequiredRoles().stream()
                        .filter(ref -> ref.getRole().equals(excluded.getRole())).findFirst();
                if (required.isEmpty()) continue;
                boolean keepRequired = required.get().getPriority() == 1;
                boolean keepExcluded = !keepRequired && excluded.getPriority() == 1;
                if (!keepRequired) {
                    profile.getRequiredRoles().remove(required.get());
                    drop("required:" + excluded.getRole(), "CONFLICT", "Vừa bắt buộc vừa loại vai trò " + excluded.getRole());
                }
                if (!keepExcluded) {
                    profile.getExcludedRoles().remove(excluded);
                    drop("excluded:" + excluded.getRole(), "CONFLICT", "Vừa bắt buộc vừa loại vai trò " + excluded.getRole());
                }
            }
        }

        /**
         * V11: mục cùng role.spec (hoặc cùng metric) mâu thuẫn → giữ mục priority nhỏ hơn; hòa thì giữ mục đến trước
         * (hồ sơ USER_EDITED do chính khách sửa nên thứ tự của khách được tôn trọng).
         */
        private <T> List<T> resolveConflicts(List<T> items, java.util.function.Function<T, String> keyOf,
                                             java.util.function.Function<T, Integer> priorityOf,
                                             java.util.function.Function<T, String> idOf,
                                             java.util.function.Function<T, double[]> intervalOf,
                                             java.util.function.Function<T, String> opOf,
                                             java.util.function.Function<T, List<String>> valuesOf) {
            List<T> ordered = items.stream().sorted(Comparator.comparing(priorityOf)).toList();
            Map<String, double[]> intervals = new LinkedHashMap<>();
            Map<String, Set<String>> allowed = new LinkedHashMap<>();
            Map<String, Set<String>> banned = new LinkedHashMap<>();
            Set<String> kept = new HashSet<>();
            for (T item : ordered) {
                String key = keyOf.apply(item);
                String op = opOf.apply(item);
                boolean conflict;
                if (INT_OPS.contains(op)) {
                    double[] current = intervals.getOrDefault(key, new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY});
                    double[] next = intervalOf.apply(item);
                    double low = Math.max(current[0], next[0]);
                    double high = Math.min(current[1], next[1]);
                    conflict = low > high;
                    if (!conflict) intervals.put(key, new double[]{low, high});
                } else {
                    Set<String> currentAllowed = allowed.get(key);
                    Set<String> currentBanned = banned.getOrDefault(key, Set.of());
                    Set<String> values = new HashSet<>(valuesOf.apply(item));
                    if ("IN".equals(op)) {
                        Set<String> nextAllowed = new HashSet<>(currentAllowed != null ? currentAllowed : values);
                        nextAllowed.retainAll(values);
                        nextAllowed.removeAll(currentBanned);
                        conflict = nextAllowed.isEmpty();
                        if (!conflict) allowed.put(key, nextAllowed);
                    } else if ("NOT_IN".equals(op)) {
                        Set<String> nextBanned = new HashSet<>(currentBanned);
                        nextBanned.addAll(values);
                        conflict = currentAllowed != null && nextBanned.containsAll(currentAllowed);
                        if (!conflict) banned.put(key, nextBanned);
                    } else {
                        conflict = false;
                    }
                }
                if (conflict) {
                    drop(idOf.apply(item), "CONFLICT", "Mâu thuẫn với mục quan trọng hơn trên " + key);
                } else {
                    kept.add(idOf.apply(item));
                }
            }
            return items.stream().filter(item -> kept.contains(idOf.apply(item))).collect(Collectors.toCollection(ArrayList::new));
        }

        // V13: cắt bớt theo thứ tự priority
        private <T> List<T> capCount(List<T> items, int max, java.util.function.Function<T, Integer> priorityOf,
                                     java.util.function.Function<T, String> idOf) {
            if (items.size() <= max) return items;
            List<T> sorted = items.stream().sorted(Comparator.comparing(priorityOf)).toList();
            sorted.stream().skip(max).forEach(item -> drop(idOf.apply(item), "TOO_MANY_ITEMS", "Vượt quá " + max + " mục"));
            Set<String> kept = sorted.stream().limit(max).map(idOf).collect(Collectors.toSet());
            return items.stream().filter(item -> kept.contains(idOf.apply(item))).collect(Collectors.toCollection(ArrayList::new));
        }

        private double[] constraintInterval(ProfileConstraintDto item) {
            return item.getValue() != null ? interval(item.getOp(), item.getValue())
                    : new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
        }

        // V4: khớp code (không phân biệt hoa thường) hoặc display_value (bỏ dấu), ví dụ "trắng" → WHITE
        private List<String> matchValues(String id, MetadataDictionaryResponse.SpecEntry entry, List<String> requested) {
            Map<String, String> values = entry.getValues() != null ? entry.getValues() : Map.of();
            Set<String> result = new LinkedHashSet<>();
            for (String value : nullSafe(requested)) {
                String normalized = TextNormalizer.normalize(value);
                Optional<String> code = values.entrySet().stream()
                        .filter(option -> TextNormalizer.normalize(option.getKey()).equals(normalized)
                                || TextNormalizer.normalize(option.getValue()).equals(normalized))
                        .map(Map.Entry::getKey)
                        .findFirst();
                if (code.isPresent()) {
                    result.add(code.get());
                } else {
                    drop(id, "UNKNOWN_VALUE", "Bỏ giá trị lạ '" + value + "'");
                }
            }
            return new ArrayList<>(result);
        }

        private String enumLabel(String specName, String op, List<String> codes,
                                 MetadataDictionaryResponse.SpecEntry entry) {
            String displayed = codes.stream()
                    .map(code -> entry.getValues() != null ? entry.getValues().getOrDefault(code, code) : code)
                    .collect(Collectors.joining(", "));
            String prefix = readable(specName);
            return switch (op) {
                case "NOT_IN" -> prefix + " không phải " + displayed;
                case "CONTAINS" -> prefix + " hỗ trợ " + displayed;
                default -> prefix + ": " + displayed;
            };
        }
    }

    // ---------- Tiện ích dùng chung ----------

    /** Giữ id hợp lệ, không trùng; id thiếu hoặc trùng được đánh lại theo thứ tự (V15). */
    private static List<String> assignIds(List<String> requested, String prefix) {
        return assignIds(requested, prefix, requested.size());
    }

    private static List<String> assignIds(List<String> requested, String prefix, int size) {
        List<String> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (String id : requested) {
            if (id != null && !id.isBlank() && used.add(id.trim())) {
                result.add(id.trim());
            } else {
                result.add(null);
            }
        }
        while (result.size() < size) result.add(null);
        int counter = 1;
        for (int index = 0; index < result.size(); index++) {
            if (result.get(index) == null) {
                while (used.contains(prefix + counter)) counter++;
                result.set(index, prefix + counter);
                used.add(prefix + counter);
            }
        }
        return result;
    }

    private static Optional<Map.Entry<String, MetadataDictionaryResponse.SpecEntry>> findSpec(
            MetadataDictionaryResponse.RoleEntry role, String specName) {
        if (specName == null) return Optional.empty();
        return role.getSpecs().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(specName.trim()))
                .findFirst();
    }

    private static Set<String> opsFor(SpecType type) {
        return switch (type) {
            case INT -> INT_OPS;
            case ENUM -> ENUM_OPS;
            case LIST -> LIST_OPS;
            case TEXT -> Set.of();
        };
    }

    static double[] interval(String op, Double value) {
        if (value == null) return new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
        return switch (op) {
            case "GTE" -> new double[]{value, Double.POSITIVE_INFINITY};
            case "LTE" -> new double[]{Double.NEGATIVE_INFINITY, value};
            default -> new double[]{value, value};
        };
    }

    // V6: vượt khoảng thực tế thì giữ mục và đánh dấu để solver hạ trước (DD 6.7)
    private static boolean isOutOfRange(String op, double value, Integer min, Integer max) {
        return switch (op) {
            case "GTE" -> max != null && value > max;
            case "LTE" -> min != null && value < min;
            default -> (max != null && value > max) || (min != null && value < min);
        };
    }

    // V9: cap không vượt max thực tế
    private static Double capOf(Double cap, Integer max) {
        if (cap == null || cap.isNaN() || cap <= 0) return null;
        return max != null && cap > max ? max.doubleValue() : cap;
    }

    private static String label(String specName, String op, double value, String unit) {
        return readable(specName) + " " + symbol(op) + " " + formatNumber(value) + (unit != null ? " " + unit : "");
    }

    /** "VRAM_GB" → "VRAM", "Core_Count" → "Core Count": bỏ hậu tố đơn vị, thay gạch dưới bằng khoảng trắng. */
    private static String readable(String specName) {
        if (ProfileMatcher.BRAND_SPEC.equals(specName)) return "Hãng";
        String name = specName.replaceAll("_(GB|MM|W|MHz|MBps|TBW)$", "");
        return name.replace('_', ' ');
    }

    private static String symbol(String op) {
        return switch (op) {
            case "GTE" -> "≥";
            case "LTE" -> "≤";
            default -> "=";
        };
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static boolean isInteger(Double value) {
        return value != null && !value.isNaN() && !value.isInfinite() && value == Math.rint(value);
    }

    private static int priority(Integer priority) {
        return priority == null ? 2 : Math.max(1, Math.min(3, priority));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static String normalizeRole(String role) {
        return role != null ? role.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static String upper(String value) {
        return value != null ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static String truncate(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() > MAX_TEXT ? trimmed.substring(0, MAX_TEXT) : trimmed;
    }

    private static List<String> truncateAll(List<String> values, int maxItems) {
        return nullSafe(values).stream().filter(Objects::nonNull).map(ProfileValidatorImpl::truncate)
                .filter(value -> !value.isEmpty()).limit(maxItems).collect(Collectors.toCollection(ArrayList::new));
    }

    private static Double findIgnoreCase(Map<String, Double> map, String key) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : List.of();
    }
}
