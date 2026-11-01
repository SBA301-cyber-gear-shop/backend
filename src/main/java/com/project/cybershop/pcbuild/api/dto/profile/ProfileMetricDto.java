package com.project.cybershop.pcbuild.api.dto.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ràng buộc cứng trên cả bộ máy (metric DD 5.2). */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileMetricDto {
    private String id;
    private String metric;              // TOTAL_RAM_GB, TOTAL_STORAGE_GB, TOTAL_VRAM_GB, GPU_COUNT, CPU_COUNT, TOTAL_CPU_CORES
    private String op;                  // GTE, LTE, EQ
    private Double value;
    private Integer priority;
    private String reason;
    private String label;
    private Boolean satisfied;
    private Boolean unsatisfiable;
}
