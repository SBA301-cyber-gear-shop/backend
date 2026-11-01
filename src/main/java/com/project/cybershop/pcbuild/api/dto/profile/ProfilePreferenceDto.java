package com.project.cybershop.pcbuild.api.dto.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Ưu tiên mềm dùng để chấm điểm (DD 6.2): nhắm một thông số của role hoặc một metric. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfilePreferenceDto {
    private String id;
    private String role;
    private String spec;
    private String metric;
    private String direction;           // MAX, MIN (INT/metric); PREFER (ENUM/LIST)
    @Builder.Default
    private List<String> values = new ArrayList<>();
    private Double weight;
    private Double cap;                 // điểm bão hòa: vượt cap không cộng thêm điểm
    private String reason;

    /**
     * Preference nhắm metric hay spec. Gemini có lúc điền cả ba trường (role, spec, metric): có đủ role + spec thì tính
     * theo spec, metric bị bỏ qua. Mọi nơi đọc preference dùng chung quy tắc này để không hiểu lệch nhau.
     */
    public boolean targetsMetric() {
        boolean hasMetric = metric != null && !metric.isBlank();
        boolean hasRoleSpec = role != null && !role.isBlank() && spec != null && !spec.isBlank();
        return hasMetric && !hasRoleSpec;
    }
}
