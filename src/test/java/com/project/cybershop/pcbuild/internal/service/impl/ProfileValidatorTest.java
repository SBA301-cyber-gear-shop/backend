package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.profile.DroppedItemDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileRoleRefDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.ProfileValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.project.cybershop.pcbuild.internal.PcBuildTestData.part;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProfileValidator - kiểm hồ sơ theo metadata (DD 5.4, V1–V15)")
class ProfileValidatorTest {

    private final ProfileValidatorImpl validator = new ProfileValidatorImpl(new ProfilePresetsImpl());
    private final MetadataDictionaryResponse dictionary = dictionary();

    private static MetadataDictionaryResponse dictionary() {
        List<CategorySpecDto> catalog = List.of(
                CategorySpecDto.builder().categoryId(1).categorySlug("cpu").categoryName("CPU").specificationId(22).specificationName("Core_Count").build(),
                CategorySpecDto.builder().categoryId(4).categorySlug("gpu").categoryName("GPU").specificationId(12).specificationName("VRAM_GB").build(),
                CategorySpecDto.builder().categoryId(4).categorySlug("gpu").categoryName("GPU").specificationId(46).specificationName("Compute_Stack")
                        .options(List.of(new CategorySpecDto.Option("CUDA", "NVIDIA CUDA"), new CategorySpecDto.Option("ROCM", "AMD ROCm"))).build(),
                CategorySpecDto.builder().categoryId(7).categorySlug("case").categoryName("Vỏ").specificationId(61).specificationName("Color")
                        .options(List.of(new CategorySpecDto.Option("BLACK", "Đen"), new CategorySpecDto.Option("WHITE", "Trắng"))).build(),
                CategorySpecDto.builder().categoryId(7).categorySlug("case").categoryName("Vỏ").specificationId(64).specificationName("Price_Source").build());
        List<ComponentCandidateDTO> candidates = new ArrayList<>(List.of(
                part(1, "cpu", "CPU", 1, Map.of(22, "6")), part(2, "cpu", "CPU", 1, Map.of(22, "16")),
                part(3, "gpu", "GPU", 1, Map.of(12, "8", 46, "CUDA")), part(4, "gpu", "GPU", 1, Map.of(12, "96", 46, "ROCM")),
                part(5, "case", "Vỏ", 1, Map.of(61, "WHITE", 64, "Shop"))));
        candidates.set(0, withBrand(candidates.get(0), "Intel"));
        Map<Integer, ParsedSpecs> parsed = new HashMap<>();
        candidates.forEach(candidate -> parsed.put(candidate.getVariantId(), ParsedSpecs.of(candidate)));
        return new MetadataDictionaryServiceImpl(new ObjectMapper()).build(candidates, parsed, catalog, null);
    }

    private static ComponentCandidateDTO withBrand(ComponentCandidateDTO candidate, String brand) {
        candidate.setBrandName(brand);
        return candidate;
    }

    @Test
    @DisplayName("objective: giữ MAX_PRICE, bỏ giá trị lạ")
    void objectiveAllowsOnlyMaxPrice() {
        RequirementProfileDto maxPrice = base();
        maxPrice.setObjective("MAX_PRICE");
        assertThat(validate(maxPrice).profile().getObjective()).isEqualTo("MAX_PRICE");

        RequirementProfileDto unknown = base();
        unknown.setObjective("FREE_PC");
        assertThat(validate(unknown).profile().getObjective()).isNull();
    }

    @Test
    @DisplayName("Preference có cả role + spec lẫn metric (Gemini gửi thật): giữ role + spec, bỏ metric, không đổi nghĩa")
    void preferenceWithSpecAndMetricKeepsSpec() {
        RequirementProfileDto profile = base();
        profile.getPreferences().add(ProfilePreferenceDto.builder().role("gpu").spec("VRAM_GB").metric("GPU_COUNT")
                .direction("MAX").weight(0.8).build());

        assertThat(validate(profile).profile().getPreferences()).singleElement().satisfies(item -> {
            assertThat(item.getRole()).isEqualTo("gpu");
            assertThat(item.getSpec()).isEqualTo("VRAM_GB");
            assertThat(item.getMetric()).isNull();
        });
    }

    @Test
    @DisplayName("Preference chỉ có metric vẫn là metric, kể cả khi kèm role; có spec sai thì bỏ với UNKNOWN_SPEC, không rơi về metric")
    void preferenceTargetRules() {
        RequirementProfileDto metricOnly = base();
        metricOnly.getPreferences().add(ProfilePreferenceDto.builder().role("gpu").metric("GPU_COUNT").direction("MAX").weight(0.8).build());
        assertThat(validate(metricOnly).profile().getPreferences()).singleElement().satisfies(item -> {
            assertThat(item.getMetric()).isEqualTo("GPU_COUNT");
            assertThat(item.getSpec()).isNull();
        });

        RequirementProfileDto badSpec = base();
        badSpec.getPreferences().add(ProfilePreferenceDto.builder().role("gpu").spec("Khong_Co").metric("GPU_COUNT").direction("MAX").weight(0.8).build());
        ValidatedProfile validated = validate(badSpec);
        assertThat(validated.profile().getPreferences()).isEmpty();
        assertThat(validated.profile().getDroppedItems()).extracting("reason").contains("UNKNOWN_SPEC");
    }

    private ValidatedProfile validate(RequirementProfileDto profile) {
        return validator.validate(profile, dictionary, ProfileValidator.SOURCE_AI, null);
    }

    private static RequirementProfileDto base() {
        return RequirementProfileDto.builder().archetype("CREATIVE").summary("Dựng phim")
                .roleWeights(new LinkedHashMap<>(Map.of("cpu", 0.5, "gpu", 0.5))).build();
    }

    private static ProfileConstraintDto constraint(String role, String spec, String op, Double value, int priority, String... values) {
        return ProfileConstraintDto.builder().role(role).spec(spec).op(op).value(value).priority(priority)
                .values(new ArrayList<>(List.of(values))).reason("lý do").build();
    }

    private static List<String> droppedReasons(ValidatedProfile profile) {
        return profile.profile().getDroppedItems().stream().map(DroppedItemDto::getReason).toList();
    }

    @Test
    @DisplayName("V1/V2/V3: role lạ, spec không thuộc role, GTE trên ENUM bị bỏ với mã lý do")
    void unknownRoleSpecAndOperatorDropped() {
        RequirementProfileDto profile = base();
        profile.getConstraints().add(constraint("monitor", "Size", "GTE", 27.0, 2));
        profile.getConstraints().add(constraint("cpu", "VRAM_GB", "GTE", 8.0, 2));
        profile.getConstraints().add(constraint("gpu", "Compute_Stack", "GTE", 1.0, 2));
        profile.getConstraints().add(constraint("case", "Price_Source", "IN", null, 2, "Shop"));

        ValidatedProfile validated = validate(profile);

        assertThat(validated.constraints()).isEmpty();
        assertThat(droppedReasons(validated)).containsExactly("UNKNOWN_ROLE", "UNKNOWN_SPEC", "OP_TYPE_MISMATCH", "UNKNOWN_SPEC");
    }

    @Test
    @DisplayName("V4: \"trắng\", \"Trắng\", \"white\" đều → WHITE; giá trị lạ bị bỏ")
    void enumValuesMatchCodeOrDisplay() {
        for (String value : List.of("trắng", "Trắng", "white", "WHITE")) {
            RequirementProfileDto profile = base();
            profile.getConstraints().add(constraint("case", "Color", "IN", null, 1, value, "hồng"));
            ValidatedProfile validated = validate(profile);
            assertThat(validated.constraints()).singleElement().satisfies(item -> {
                assertThat(item.getValues()).containsExactly("WHITE");
                assertThat(item.getLabel()).isEqualTo("Color: Trắng");
            });
            assertThat(droppedReasons(validated)).containsExactly("UNKNOWN_VALUE");
        }
    }

    @Test
    @DisplayName("ENUM với EQ + values (Gemini thật trả \"Color EQ [WHITE]\") được chuẩn hóa thành IN, không mất yêu cầu priority 1")
    void enumEqualsNormalizedToIn() {
        RequirementProfileDto profile = base();
        profile.getConstraints().add(constraint("case", "Color", "EQ", null, 1, "WHITE"));

        ValidatedProfile validated = validate(profile);

        assertThat(validated.constraints()).singleElement().satisfies(item -> {
            assertThat(item.getOp()).isEqualTo("IN");
            assertThat(item.getPriority()).isEqualTo(1);
        });
        assertThat(validated.warnings()).contains("OP_NORMALIZED:c1:EQ→IN");
        assertThat(droppedReasons(validated)).isEmpty();
    }

    @Test
    @DisplayName("Ràng buộc tích cực trên gpu (VRAM ≥ 8) mà thiếu requiredRoles → suy ra gpu bắt buộc cùng priority")
    void positiveGpuConstraintImpliesRequiredGpu() {
        RequirementProfileDto profile = base();
        profile.getConstraints().add(constraint("gpu", "VRAM_GB", "GTE", 8.0, 2));

        ValidatedProfile validated = validate(profile);

        assertThat(validated.isRequired("gpu")).isTrue();
        assertThat(validated.profile().getRequiredRoles()).singleElement()
                .satisfies(ref -> assertThat(ref.getPriority()).isEqualTo(2));
        assertThat(validated.warnings()).contains("REQUIRED_ROLE_IMPLIED:gpu");

        RequirementProfileDto excluded = base();
        excluded.getConstraints().add(constraint("gpu", "VRAM_GB", "GTE", 8.0, 2));
        excluded.getExcludedRoles().add(ProfileRoleRefDto.builder().role("gpu").build());
        assertThat(validate(excluded).isRequired("gpu")).isFalse();
    }

    @Test
    @DisplayName("V5/V6: số không nguyên bị bỏ; VRAM_GB GTE 200 (max 96) giữ lại với unsatisfiable")
    void numericChecks() {
        RequirementProfileDto profile = base();
        profile.getConstraints().add(constraint("gpu", "VRAM_GB", "GTE", 12.5, 2));
        profile.getConstraints().add(constraint("gpu", "VRAM_GB", "GTE", 200.0, 2));

        ValidatedProfile validated = validate(profile);

        assertThat(droppedReasons(validated)).containsExactly("NOT_A_NUMBER");
        assertThat(validated.constraints()).singleElement().satisfies(item -> {
            assertThat(item.getUnsatisfiable()).isTrue();
            assertThat(item.getLabel()).isEqualTo("VRAM ≥ 200 GB");
        });
        assertThat(validated.warnings()).anyMatch(warning -> warning.startsWith("CONSTRAINT_UNSATISFIABLE"));
    }

    @Test
    @DisplayName("V7/V8: metric lạ bị bỏ; trọng số âm ép về 0, tổng chuẩn hóa = 1; tổng = 0 thì lấy trọng số hồ sơ mẫu")
    void metricsAndWeights() {
        RequirementProfileDto profile = base();
        profile.setRoleWeights(new LinkedHashMap<>(Map.of("cpu", 2.0, "gpu", -1.0, "ram", 0.5)));
        profile.getMetrics().add(ProfileMetricDto.builder().metric("NOISE_DB").op("LTE").value(30.0).priority(2).build());

        ValidatedProfile validated = validate(profile);

        assertThat(droppedReasons(validated)).contains("UNKNOWN_METRIC");
        assertThat(validated.weight("gpu")).isZero();
        assertThat(validated.weight("cpu")).isCloseTo(1 / 1.5, org.assertj.core.api.Assertions.within(1e-9));
        assertThat(validated.profile().getRoleWeights().values().stream().mapToDouble(Double::doubleValue).sum())
                .isCloseTo(1.0, org.assertj.core.api.Assertions.within(1e-9));

        RequirementProfileDto zero = base();
        zero.setRoleWeights(new LinkedHashMap<>(Map.of("cpu", 0.0)));
        assertThat(validate(zero).weight("gpu")).isEqualTo(0.25); // CREATIVE
    }

    @Test
    @DisplayName("V11: TOTAL_RAM_GB GTE 32 (p2) + LTE 16 (p1) → giữ mục priority nhỏ hơn, bỏ mục còn lại với CONFLICT")
    void conflictKeepsMoreImportant() {
        RequirementProfileDto profile = base();
        profile.getMetrics().add(ProfileMetricDto.builder().metric("TOTAL_RAM_GB").op("GTE").value(32.0).priority(2).build());
        profile.getMetrics().add(ProfileMetricDto.builder().metric("TOTAL_RAM_GB").op("LTE").value(16.0).priority(1).build());

        ValidatedProfile validated = validate(profile);

        assertThat(validated.metrics()).singleElement().satisfies(item -> assertThat(item.getOp()).isEqualTo("LTE"));
        assertThat(droppedReasons(validated)).containsExactly("CONFLICT");
    }

    @Test
    @DisplayName("V12: gpu vừa bắt buộc vừa loại → giữ mục priority 1; V10: thương hiệu lạ bị bỏ, tên chuẩn hóa")
    void roleConflictAndBrands() {
        RequirementProfileDto profile = base();
        profile.getRequiredRoles().add(ProfileRoleRefDto.builder().role("gpu").priority(1).build());
        profile.getExcludedRoles().add(ProfileRoleRefDto.builder().role("GPU").build());
        profile.getBrands().setPrefer(new ArrayList<>(List.of("intel", "Noname")));

        ValidatedProfile validated = validate(profile);

        assertThat(validated.isRequired("gpu")).isTrue();
        assertThat(validated.isExcluded("gpu")).isFalse();
        assertThat(validated.profile().getBrands().getPrefer()).containsExactly("Intel");
        assertThat(droppedReasons(validated)).contains("CONFLICT", "BRAND_NOT_AVAILABLE");
    }

    @Test
    @DisplayName("V13/V14/V15: quá 20 constraint bị cắt theo priority; ramGb = 64 thành metric priority 1; id trùng đánh lại")
    void limitsLegacyRamAndIds() {
        RequirementProfileDto profile = base();
        for (int index = 0; index < 22; index++) {
            ProfileConstraintDto item = constraint("cpu", "Core_Count", "GTE", 6.0, index < 2 ? 3 : 2);
            item.setId("dup");
            profile.getConstraints().add(item);
        }
        profile.getMetrics().add(ProfileMetricDto.builder().metric("TOTAL_RAM_GB").op("GTE").value(16.0).priority(2).build());

        ValidatedProfile validated = validator.validate(profile, dictionary, ProfileValidator.SOURCE_USER_EDITED, 64);

        assertThat(validated.constraints()).hasSize(20).allMatch(item -> item.getPriority() == 2);
        assertThat(validated.constraints()).extracting(ProfileConstraintDto::getId).doesNotHaveDuplicates();
        assertThat(validated.metrics()).singleElement().satisfies(item -> {
            assertThat(item.getValue()).isEqualTo(64.0);
            assertThat(item.getPriority()).isEqualTo(1);
        });
        assertThat(validated.profile().getSource()).isEqualTo("USER_EDITED");
    }

    @Test
    @DisplayName("Preference: cap vượt max bị ép về max (V9); PREFER trên INT bị bỏ; metric dùng MAX/MIN")
    void preferencesValidated() {
        RequirementProfileDto profile = base();
        profile.getPreferences().add(ProfilePreferenceDto.builder().role("cpu").spec("Core_Count").direction("MAX").weight(1.5).cap(512.0).build());
        profile.getPreferences().add(ProfilePreferenceDto.builder().role("gpu").spec("VRAM_GB").direction("PREFER").weight(0.5).build());
        profile.getPreferences().add(ProfilePreferenceDto.builder().metric("TOTAL_RAM_GB").direction("MAX").weight(0.8).build());

        ValidatedProfile validated = validate(profile);

        assertThat(validated.profile().getPreferences()).hasSize(2);
        assertThat(validated.profile().getPreferences().get(0).getCap()).isEqualTo(16.0);
        assertThat(validated.profile().getPreferences().get(0).getWeight()).isEqualTo(1.0);
        assertThat(droppedReasons(validated)).containsExactly("OP_TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Hồ sơ client chứa variantId hay trường lạ: bị bỏ qua khi đọc JSON (không có trong DTO)")
    void unknownJsonFieldsIgnored() throws Exception {
        String json = "{\"summary\":\"x\",\"variantId\":77,\"price\":0,\"constraints\":[{\"role\":\"gpu\",\"spec\":\"VRAM_GB\",\"op\":\"GTE\",\"value\":8,\"variantId\":5}]}";
        RequirementProfileDto profile = new ObjectMapper().readValue(json, RequirementProfileDto.class);

        ValidatedProfile validated = validate(profile);

        assertThat(validated.constraints()).singleElement().satisfies(item -> assertThat(item.getValue()).isEqualTo(8.0));
    }
}
