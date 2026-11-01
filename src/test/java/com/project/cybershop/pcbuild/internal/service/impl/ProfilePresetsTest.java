package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileRoleRefDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.BuildTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hồ sơ mẫu (DD 5.5). Fixture src/test/resources/pcbuild/profiles/{archetype}-{tier}.json là JSON của preset(),
 * sinh bằng: .\mvnw.cmd test "-Dtest=ProfilePresetsTest" "-Dpcbuild.writeFixtures=true" rồi commit.
 */
@DisplayName("ProfilePresets - hồ sơ mẫu tất định và 42 fixture")
class ProfilePresetsTest {

    private static final Path FIXTURES = Path.of("src", "test", "resources", "pcbuild", "profiles");
    private final ProfilePresetsImpl presets = new ProfilePresetsImpl();
    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    @DisplayName("Với cả 42 cặp (archetype, tier), preset() trùng khớp file fixture")
    void presetsMatchFixtures() throws Exception {
        boolean write = Boolean.getBoolean("pcbuild.writeFixtures");
        for (Archetype archetype : Archetype.values()) {
            for (BuildTier tier : BuildTier.values()) {
                String content = json.writeValueAsString(presets.preset(archetype, tier)) + System.lineSeparator();
                Path file = FIXTURES.resolve(archetype.name().toLowerCase(Locale.ROOT) + "-" + tier.name() + ".json");
                if (write) {
                    Files.createDirectories(FIXTURES);
                    Files.writeString(file, content, StandardCharsets.UTF_8);
                }
                assertThat(file).as("thiếu fixture, chạy với -Dpcbuild.writeFixtures=true").exists();
                assertThat(Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n"))
                        .as(file.toString()).isEqualTo(content.replace("\r\n", "\n"));
            }
        }
    }

    @Test
    @DisplayName("Biên mức: 19.999.999 → T2, 20.000.000 → T3, 300.000.000 → T7")
    void tierBoundaries() {
        assertThat(BuildTier.of(new BigDecimal("19999999"))).isEqualTo(BuildTier.T2);
        assertThat(BuildTier.of(new BigDecimal("20000000"))).isEqualTo(BuildTier.T3);
        assertThat(BuildTier.of(new BigDecimal("300000000"))).isEqualTo(BuildTier.T7);
        assertThat(BuildTier.of(new BigDecimal("8000000"))).isEqualTo(BuildTier.T1);
    }

    @Test
    @DisplayName("GAMING: T1 không có requiredRoles gpu và balancePairs; T3 Perf_Tier ≥ 3, T4 ≥ 5, T5 ≥ 8")
    void gamingMatrix() {
        RequirementProfileDto t1 = presets.preset(Archetype.GAMING, BuildTier.T1);
        assertThat(t1.getRequiredRoles()).isEmpty();
        assertThat(t1.getBalancePairs()).isEmpty();
        assertThat(constraint(presets.preset(Archetype.GAMING, BuildTier.T3), "Perf_Tier")).hasValueSatisfying(c -> assertThat(c.getValue()).isEqualTo(3.0));
        assertThat(constraint(presets.preset(Archetype.GAMING, BuildTier.T4), "Perf_Tier")).hasValueSatisfying(c -> assertThat(c.getValue()).isEqualTo(5.0));
        assertThat(constraint(presets.preset(Archetype.GAMING, BuildTier.T5), "Perf_Tier")).hasValueSatisfying(c -> assertThat(c.getValue()).isEqualTo(8.0));
        RequirementProfileDto t3 = presets.preset(Archetype.GAMING, BuildTier.T3);
        assertThat(t3.getMetrics()).extracting(ProfileMetricDto::getMetric).containsExactly("TOTAL_RAM_GB", "GPU_COUNT");
        assertThat(t3.getSummary()).isEqualTo("Hồ sơ mẫu GAMING T3");
        assertThat(t3.getConstraints()).allMatch(c -> c.getPriority() == 2 && "Hồ sơ mẫu GAMING T3".equals(c.getReason()));
    }

    @Test
    @DisplayName("CREATIVE T5 có Core_Count ≥ 8 và TOTAL_RAM_GB ≥ 96; AI_DATA mọi mức ưu tiên TOTAL_VRAM_GB MAX 0,6")
    void creativeAndAiData() {
        RequirementProfileDto creative = presets.preset(Archetype.CREATIVE, BuildTier.T5);
        assertThat(constraint(creative, "Core_Count")).hasValueSatisfying(c -> assertThat(c.getValue()).isEqualTo(8.0));
        assertThat(creative.getMetrics()).anyMatch(m -> m.getMetric().equals("TOTAL_RAM_GB") && m.getValue() == 96.0);
        for (BuildTier tier : BuildTier.values()) {
            List<ProfilePreferenceDto> gpu = presets.preset(Archetype.AI_DATA, tier).getPreferences().stream()
                    .filter(p -> "TOTAL_VRAM_GB".equals(p.getMetric()) || "gpu".equals(p.getRole())).toList();
            assertThat(gpu).anyMatch(p -> "TOTAL_VRAM_GB".equals(p.getMetric()) && p.getWeight() == 0.6);
            assertThat(gpu).noneMatch(p -> "VRAM_GB".equals(p.getSpec()));
        }
    }

    @Test
    @DisplayName("DEV dùng cap TOTAL_RAM_GB 128, Core_Count 16; OFFICE không có preference cho gpu; id đánh theo thứ tự")
    void devAndOffice() {
        RequirementProfileDto dev = presets.preset(Archetype.DEV, BuildTier.T4);
        assertThat(dev.getPreferences()).anyMatch(p -> "TOTAL_RAM_GB".equals(p.getMetric()) && p.getCap() == 128.0);
        assertThat(dev.getPreferences()).anyMatch(p -> "Core_Count".equals(p.getSpec()) && p.getCap() == 16.0);
        RequirementProfileDto office = presets.preset(Archetype.OFFICE, BuildTier.T3);
        assertThat(office.getRoleWeights().get("gpu")).isZero();
        assertThat(office.getPreferences()).noneMatch(p -> "gpu".equals(p.getRole()));
        assertThat(office.getExcludedRoles()).extracting(r -> r.getRole()).containsExactly("gpu");
        assertThat(office.getPreferences()).extracting(ProfilePreferenceDto::getId).startsWith("p1", "p2", "p3");
    }

    @Test
    @DisplayName("fillMissing: thiếu roleWeights + OFFICE → điền trọng số OFFICE; thiếu preference psu → dùng ưu tiên mặc định")
    void fillMissing() {
        RequirementProfileDto ai = RequirementProfileDto.builder().archetype("OFFICE")
                .roleWeights(new LinkedHashMap<>())
                .preferences(new ArrayList<>(List.of(ProfilePreferenceDto.builder().role("cpu").spec("Core_Count")
                        .direction("MAX").weight(1.0).build())))
                .build();

        RequirementProfileDto filled = presets.fillMissing(ai, BuildTier.T3);

        assertThat(filled.getRoleWeights()).isEqualTo(presets.roleWeights(Archetype.OFFICE));
        assertThat(filled.getPreferences()).anyMatch(p -> "Efficiency_Rating".equals(p.getSpec()));
        assertThat(filled.getPreferences()).filteredOn(p -> "cpu".equals(p.getRole())).hasSize(1)
                .allSatisfy(p -> assertThat(p.getCap()).isEqualTo(8.0));   // cap OFFICE gắn vào preference của AI
        assertThat(ai.getRoleWeights()).isEmpty();                      // không sửa đầu vào
        assertThat(filled.getPreferences()).noneMatch(p -> "gpu".equals(p.getRole()));
    }

    private static RequirementProfileDto aiProfile(String archetype, Map<String, Double> weights) {
        return RequirementProfileDto.builder().archetype(archetype).roleWeights(new LinkedHashMap<>(weights)).build();
    }

    @Test
    @DisplayName("Gemini bỏ khóa (đúng phản hồi thô): khóa thiếu bù từ hồ sơ mẫu, khóa có giá trị giữ nguyên số của AI")
    void missingWeightKeysAreFilledFromPreset() {
        RequirementProfileDto ai = aiProfile("GAMING", Map.of("cpu", 0.3, "mainboard", 0.05, "ram", 0.1, "ssd", 0.08));

        Map<String, Double> weights = presets.fillMissing(ai, BuildTier.T7).getRoleWeights();

        assertThat(weights).containsEntry("cpu", 0.3).containsEntry("gpu", 0.45).containsEntry("psu", 0.04)
                .containsEntry("case", 0.03).containsEntry("cooler", 0.03);
        assertThat(ai.getRoleWeights()).doesNotContainKey("gpu");   // không sửa đầu vào
    }

    @Test
    @DisplayName("Khóa có giá trị 0 là chủ đích: giữ nguyên khi vai trò không được yêu cầu (văn phòng không cần card rời)")
    void explicitZeroWeightIsKept() {
        RequirementProfileDto ai = aiProfile("GAMING", Map.of("cpu", 0.5, "gpu", 0.0));

        assertThat(presets.fillMissing(ai, BuildTier.T3).getRoleWeights()).containsEntry("gpu", 0.0);
    }

    @Test
    @DisplayName("Khóa viết hoa hoặc có khoảng trắng được quy về chuẩn, không tạo hai khóa song song; khóa lạ giữ để validator báo")
    void weightKeysAreNormalized() {
        RequirementProfileDto ai = aiProfile("GAMING", Map.of(" GPU ", 0.6, "monitor", 0.1));

        Map<String, Double> weights = presets.fillMissing(ai, BuildTier.T3).getRoleWeights();

        assertThat(weights).containsEntry("gpu", 0.6).containsEntry("monitor", 0.1).doesNotContainKey(" GPU ");
    }

    @Test
    @DisplayName("Vai trò được yêu cầu mà trọng số 0 → lấy từ hồ sơ mẫu: qua requiredRoles, metric hoặc constraint")
    void demandedRoleWithZeroWeightUsesPreset() {
        RequirementProfileDto viaRequired = aiProfile("GAMING", Map.of("cpu", 0.5, "gpu", 0.0));
        viaRequired.getRequiredRoles().add(ProfileRoleRefDto.builder().role("GPU").priority(1).build());
        RequirementProfileDto viaMetric = aiProfile("AI_DATA", Map.of("cpu", 0.5, "gpu", 0.0));
        viaMetric.getMetrics().add(ProfileMetricDto.builder().metric("TOTAL_VRAM_GB").op("GTE").value(48.0).priority(2).build());
        RequirementProfileDto viaConstraint = aiProfile("CREATIVE", Map.of("cpu", 0.5, "gpu", 0.0));
        viaConstraint.getConstraints().add(ProfileConstraintDto.builder().role("gpu").spec("VRAM_GB").op("GTE").value(12.0).priority(2).build());

        assertThat(presets.fillMissing(viaRequired, BuildTier.T3).getRoleWeights()).containsEntry("gpu", 0.45);
        assertThat(presets.fillMissing(viaMetric, BuildTier.T3).getRoleWeights()).containsEntry("gpu", 0.50);
        assertThat(presets.fillMissing(viaConstraint, BuildTier.T3).getRoleWeights()).containsEntry("gpu", 0.25);
    }

    @Test
    @DisplayName("Hồ sơ mẫu cũng có gpu = 0 (văn phòng cần card rời) → dùng trọng số GENERAL")
    void demandedRoleFallsBackToGeneralWhenPresetIsZero() {
        RequirementProfileDto ai = aiProfile("OFFICE", Map.of("cpu", 0.5));
        ai.getRequiredRoles().add(ProfileRoleRefDto.builder().role("gpu").priority(1).build());

        assertThat(presets.fillMissing(ai, BuildTier.T3).getRoleWeights()).containsEntry("gpu", 0.25);
    }

    @Test
    @DisplayName("Không coi là yêu cầu: vai trò nằm trong excludedRoles; metric GPU_COUNT = 0 hoặc chỉ giới hạn trên")
    void notDemandedRolesKeepZeroWeight() {
        RequirementProfileDto excluded = aiProfile("GAMING", Map.of("cpu", 0.5, "gpu", 0.0));
        excluded.getExcludedRoles().add(ProfileRoleRefDto.builder().role("gpu").build());
        excluded.getRequiredRoles().add(ProfileRoleRefDto.builder().role("gpu").priority(2).build());
        RequirementProfileDto zeroCount = aiProfile("GAMING", Map.of("cpu", 0.5, "gpu", 0.0));
        zeroCount.getMetrics().add(ProfileMetricDto.builder().metric("GPU_COUNT").op("EQ").value(0.0).priority(1).build());
        RequirementProfileDto upperBound = aiProfile("GAMING", Map.of("cpu", 0.5, "gpu", 0.0));
        upperBound.getMetrics().add(ProfileMetricDto.builder().metric("GPU_COUNT").op("LTE").value(1.0).priority(1).build());

        assertThat(presets.fillMissing(excluded, BuildTier.T3).getRoleWeights()).containsEntry("gpu", 0.0);
        assertThat(presets.fillMissing(zeroCount, BuildTier.T3).getRoleWeights()).containsEntry("gpu", 0.0);
        assertThat(presets.fillMissing(upperBound, BuildTier.T3).getRoleWeights()).containsEntry("gpu", 0.0);
    }

    @Test
    @DisplayName("Preference có cả spec lẫn metric (Gemini gửi thật): tính theo spec khi kiểm preference đã có và khi gắn cap")
    void preferenceWithSpecAndMetricIsTreatedAsSpec() {
        RequirementProfileDto ai = aiProfile("GAMING", Map.of("cpu", 0.2, "gpu", 0.5));
        ai.getPreferences().add(ProfilePreferenceDto.builder().role("gpu").spec("VRAM_GB").metric("GPU_COUNT")
                .direction("MAX").weight(1.0).build());

        List<ProfilePreferenceDto> preferences = presets.fillMissing(ai, BuildTier.T7).getPreferences();

        assertThat(preferences).filteredOn(p -> "gpu".equals(p.getRole())).singleElement()
                .satisfies(p -> assertThat(p.getCap()).isEqualTo(32.0));
    }

    @Test
    @DisplayName("Preference chỉ có metric lệch vai trò không che mất preference mẫu của vai trò đó")
    void metricPreferenceCountsForItsOwnRoleOnly() {
        RequirementProfileDto ai = aiProfile("GAMING", Map.of("cpu", 0.2, "gpu", 0.5));
        ai.getPreferences().add(ProfilePreferenceDto.builder().role("cpu").metric("GPU_COUNT").direction("MAX").weight(1.0).build());

        List<ProfilePreferenceDto> preferences = presets.fillMissing(ai, BuildTier.T7).getPreferences();

        assertThat(preferences).filteredOn(p -> "cpu".equals(p.getRole()) && p.getSpec() != null).isNotEmpty();
        assertThat(preferences).filteredOn(p -> "gpu".equals(p.getRole())).isEmpty();   // GPU_COUNT đã là preference của gpu
    }

    @Test
    @DisplayName("Cap VRAM 32 GB chỉ áp cho GAMING; CREATIVE và AI_DATA vẫn ưu tiên VRAM lớn")
    void vramCapAppliesToGamingOnly() {
        for (BuildTier tier : BuildTier.values()) {
            assertThat(presets.preset(Archetype.GAMING, tier).getPreferences())
                    .filteredOn(p -> "VRAM_GB".equals(p.getSpec())).singleElement()
                    .satisfies(p -> assertThat(p.getCap()).isEqualTo(32.0));
        }
        assertThat(presets.preset(Archetype.CREATIVE, BuildTier.T7).getPreferences())
                .filteredOn(p -> "VRAM_GB".equals(p.getSpec())).singleElement()
                .satisfies(p -> assertThat(p.getCap()).isNull());
    }

    private static Optional<ProfileConstraintDto> constraint(RequirementProfileDto profile, String spec) {
        return profile.getConstraints().stream().filter(c -> spec.equals(c.getSpec())).findFirst();
    }
}
