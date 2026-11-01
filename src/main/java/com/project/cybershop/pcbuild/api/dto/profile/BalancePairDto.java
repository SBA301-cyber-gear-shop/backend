package com.project.cybershop.pcbuild.api.dto.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cặp vai trò cần cân đối điểm, ví dụ CPU và GPU khi chơi game (DD 6.3). */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalancePairDto {
    private String a;
    private String b;
    private Double maxGap;
}
