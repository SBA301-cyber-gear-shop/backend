package com.project.cybershop.pcbuild.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bước 1: khách chỉ nhập ngân sách và nhu cầu chính. Mục đích, hãng, cỡ main do AI đọc từ câu nhu cầu. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu của khách ở bước 1")
public class RequirementAnalyzeRequest {

    @NotNull(message = "Vui lòng nhập ngân sách")
    @Positive(message = "Ngân sách phải lớn hơn 0")
    @Max(value = 9_999_999_999L, message = "Ngân sách quá lớn")
    @Schema(description = "Ngân sách (đồng)", example = "15000000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long budget;

    @Size(max = 500, message = "Nhu cầu tối đa 500 ký tự")
    @Schema(description = "Nhu cầu chính. Để trống thì Shop build cấu hình đa dụng (GENERAL).",
            example = "Muốn build máy chơi game, thích AMD")
    private String message;
}
