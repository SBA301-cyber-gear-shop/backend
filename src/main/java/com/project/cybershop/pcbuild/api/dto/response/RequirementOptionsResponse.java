package com.project.cybershop.pcbuild.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Các danh sách giá trị hợp lệ của bước 1–3, đọc từ metadata trong DB. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementOptionsResponse {

    @Schema(description = "Mục đích trong budget_profiles, kèm ngân sách tối thiểu")
    private List<PurposeOptionResponse> purposes;

    @Schema(description = "Mức nhu cầu hợp lệ; rỗng khi chưa có bảng mức nhu cầu")
    private List<String> levels;

    @Schema(description = "Cỡ main hợp lệ (thông số Form_Factor của bo mạch chủ)")
    private List<Choice> formFactors;

    @Schema(description = "Hãng CPU đang còn hàng", example = "[\"AMD\", \"Intel\"]")
    private List<String> brands;

    @Schema(description = "Giá bộ PC cao cấp nhất ráp được từ kho (đồng), là cận trên của ngân sách")
    private Long maxBuildPrice;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {

        @Schema(example = "MICRO_ATX")
        private String code;

        @Schema(example = "Micro-ATX (mATX)")
        private String label;
    }
}
