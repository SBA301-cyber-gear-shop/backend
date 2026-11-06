package com.project.cybershop.pcbuild.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bước 1: yêu cầu của khách. Hai cách gửi:
 * - Chat/ô nhu cầu: gửi message (có thể kèm budget, brandPreference); để trống purpose thì AI đọc message.
 * - Khách đã chọn hoặc đồng ý mục đích (form, popup gợi ý): gửi purpose cùng các giá trị đã có; server không gọi AI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu của khách ở bước 1")
public class RequirementAnalyzeRequest {

    @Size(max = 500, message = "Nhu cầu tối đa 500 ký tự")
    @Schema(description = "Câu chat hoặc ô mô tả nhu cầu. Để trống khi gửi từ form.",
            example = "Mình có 15 củ, muốn build máy chơi game, thích AMD")
    private String message;

    @Positive(message = "Ngân sách phải lớn hơn 0")
    @Max(value = 9_999_999_999L, message = "Ngân sách quá lớn")
    @Schema(description = "Ngân sách nhập ở ô riêng (đồng). Có giá trị thì AI không đọc ngân sách trong câu chat.",
            example = "15000000")
    private Long budget;

    @Size(max = 50)
    @Schema(description = "Hãng ưu tiên chọn trên form (lấy trong danh sách brands của /options). Để trống = không quan trọng.",
            example = "AMD")
    private String brandPreference;

    @Size(max = 30)
    @Schema(description = "Mục đích khách đã chọn hoặc đồng ý (ví dụ sau popup gợi ý). Có giá trị thì server không gọi AI.",
            example = "OFFICE")
    private String purpose;

    @Size(max = 30)
    @Schema(description = "Mức nhu cầu đã có từ lần phân tích trước (gửi kèm khi có purpose)")
    private String level;

    @Size(max = 30)
    @Schema(description = "Cỡ main đã có từ lần phân tích trước (gửi kèm khi có purpose)", example = "MICRO_ATX")
    private String formFactor;

    @Min(value = 0, message = "clarifyCount không hợp lệ")
    @Max(value = 10, message = "clarifyCount không hợp lệ")
    @Schema(description = "Số lần hệ thống đã hỏi lại khách (lấy từ clarifyCount của phản hồi trước). Lần đầu gửi 0.",
            example = "0")
    private Integer clarifyCount;
}
