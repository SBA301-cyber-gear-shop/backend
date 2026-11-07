package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Kết quả build / kiểm tra bộ PC (docx mục 6, "Kết quả trả về frontend"). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildResultResponse {
    private BuildStatus status;
    private Long budget;
    /** Mục đích đã dùng để build; null ở API validate. */
    private String purpose;
    private Long total;
    /** Tổng trừ ngân sách (đồng), âm là còn dư. */
    private Long difference;
    @Builder.Default
    private List<BuildItemResponse> items = new ArrayList<>();
    @Builder.Default
    private List<BuildViolationResponse> violations = new ArrayList<>();
    /** Câu cảnh báo khi OVER_BUDGET (bước 7), null khi không vượt. */
    private String budgetWarning;
    /** Lời giải thích của AI (bước 8), chưa làm nên luôn null. */
    private String message;
}
