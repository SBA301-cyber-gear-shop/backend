package com.project.cybershop.pcbuild.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một mục đích trong budget_profiles, kèm nhãn hiển thị và ngân sách tối thiểu. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurposeOptionResponse {

    @Schema(example = "OFFICE")
    private String purpose;

    @Schema(example = "PC văn phòng")
    private String label;

    @Schema(example = "Văn phòng, học tập, Excel, họp online; không cần card đồ họa rời")
    private String description;

    @Schema(description = "Ngân sách tối thiểu (đồng)", example = "10500000")
    private Long minBudget;
}
