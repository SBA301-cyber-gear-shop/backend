package com.project.cybershop.pcbuild.api.dto.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Ràng buộc cứng trên từng ứng viên của một vai trò (DD 5.3). */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileConstraintDto {
    private String id;
    private String role;
    private String spec;
    private String op;                  // INT: GTE, LTE, EQ; ENUM: IN, NOT_IN; LIST: CONTAINS
    private Double value;
    @Builder.Default
    private List<String> values = new ArrayList<>();
    private Integer priority;           // 1 khách nói rõ, 2 suy ra, 3 nên có
    private String reason;
    private String label;               // Java sinh, ví dụ "VRAM ≥ 12 GB"
    private Boolean satisfied;          // kết quả trên bộ máy đề xuất
    private Boolean unsatisfiable;      // V6: vượt khoảng thực tế
}
