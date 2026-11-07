package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nhu cầu đã chuẩn hóa; frontend gửi nguyên {budget, purpose} này cho /builds/auto để chia tiền và build. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementSummary {
    private Long budget;
    private String purpose;
}

