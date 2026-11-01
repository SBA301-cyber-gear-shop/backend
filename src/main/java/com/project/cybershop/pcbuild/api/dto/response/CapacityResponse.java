package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mức dùng tài nguyên của bộ máy; giá trị null khi thiếu linh kiện hoặc thiếu thông số tương ứng. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityResponse {
    private Integer ramSlotsUsed;
    private Integer ramSlotsMax;
    private Integer ramGb;
    private Integer ramMaxGb;
    private Integer m2Used;
    private Integer m2Max;
    private Integer gpuCount;
    private Integer gpuMax;
}
