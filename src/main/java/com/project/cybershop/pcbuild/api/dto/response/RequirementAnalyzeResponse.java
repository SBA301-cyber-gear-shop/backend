package com.project.cybershop.pcbuild.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Kết quả bước 1–3: yêu cầu đã hiểu, trạng thái kiểm tra và câu báo cho khách. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementAnalyzeResponse {

    @Schema(description = "Kết quả kiểm tra; chỉ READY mới đi tiếp bước 4")
    private RequirementStatus status;

    @Schema(description = "Câu hiển thị cho khách (nội dung popup hoặc câu báo)")
    private String message;

    private Requirement requirement;

    @Schema(description = "Ngân sách tối thiểu của mục đích đã chọn (đồng)", example = "14500000")
    private Long minBudget;

    @Schema(description = "Giá bộ PC cao cấp nhất ráp được từ kho, dùng làm cận trên (đồng)")
    private Long maxBuildPrice;

    @Schema(description = "Mục đích vừa ngân sách, dùng cho popup khi BELOW_MIN_BUDGET")
    private List<PurposeOptionResponse> suggestedPurposes;

    @Schema(description = "Ghi chú truy vết: AI lỗi, hoặc giá trị AI trả về đã bị code đổi hoặc bỏ")
    private List<String> notes;

    /** Yêu cầu đã chuẩn hóa: mọi giá trị đều nằm trong danh sách hợp lệ hoặc null. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Requirement {

        @Schema(example = "15000000")
        private Long budget;

        @Schema(description = "Mục đích AI chọn; GENERAL khi không rõ hoặc AI lỗi", example = "GAMING")
        private String purpose;

        @Schema(description = "Mức nhu cầu trong mục đích; null khi khách không nói hoặc chưa có bảng mức")
        private String level;

        @Schema(description = "Cỡ main (mã Form_Factor); null khi khách không nói rõ", example = "MICRO_ATX")
        private String formFactor;

        @Schema(description = "Hãng CPU ưu tiên; null = không quan trọng", example = "AMD")
        private String brandPreference;
    }
}
