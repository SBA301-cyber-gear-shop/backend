package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiBuildRequest {
    @NotNull(message = "Vui lòng nhập ngân sách")
    @DecimalMin(value = "5000000", message = "Ngân sách tối thiểu từ 5.000.000 ₫")
    private BigDecimal budget;

    @Builder.Default
    private String purpose = "gaming"; // gaming, creative, work

    @Builder.Default
    private Integer ramGb = 16;

    @Builder.Default
    private String scope = "tower";    // tower, full

    private String userPrompt;         // Ví dụ: "Thích case trắng, tản êm, chơi Black Myth Wukong"
}
