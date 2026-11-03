package com.project.cybershop.pcbuild.api.dto.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hồ sơ yêu cầu (DD 5.3): đầu ra của AI, hồ sơ khách sửa gửi lại, và đầu vào duy nhất của solver về nhu cầu.
 * Là dữ liệu không tin cậy cho tới khi qua ProfileValidator. Trường lạ (ví dụ variantId) bị bỏ qua.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequirementProfileDto {
    private Integer profileVersion;
    private String source;              // AI | USER_EDITED (server ghi đè)
    private String summary;
    private String archetype;           // GAMING | CREATIVE | AI_DATA | OFFICE | DEV | GENERAL
    @Builder.Default
    private List<String> useCases = new ArrayList<>();
    @Builder.Default
    private Map<String, Double> roleWeights = new LinkedHashMap<>();
    @Builder.Default
    private List<ProfileRoleRefDto> requiredRoles = new ArrayList<>();
    @Builder.Default
    private List<ProfileRoleRefDto> excludedRoles = new ArrayList<>();
    @Builder.Default
    private List<ProfileConstraintDto> constraints = new ArrayList<>();
    @Builder.Default
    private List<ProfileMetricDto> metrics = new ArrayList<>();
    @Builder.Default
    private List<ProfilePreferenceDto> preferences = new ArrayList<>();
    @Builder.Default
    private List<BalancePairDto> balancePairs = new ArrayList<>();
    @Builder.Default
    private Brands brands = new Brands();
    @Builder.Default
    private List<String> unmetNeeds = new ArrayList<>();
    private String objective;           // null | MAX_PRICE (khách nói rõ muốn bộ đắt nhất)
    private Double confidence;
    private String clarifyingQuestion;
    @Builder.Default
    private List<DroppedItemDto> droppedItems = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Brands {
        private List<String> prefer = new ArrayList<>();
        private List<String> avoid = new ArrayList<>();
    }
}
