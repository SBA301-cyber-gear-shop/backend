package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Kết quả lưu bộ PC (pc_customs.id) cùng trạng thái lúc lưu. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveBuildResponse {
    private Integer buildId;
    /** VALID, hoặc OVER_BUDGET khi khách đã xác nhận lưu dù vượt ngân sách. */
    private BuildStatus status;
    private Long total;
    /** Tổng trừ ngân sách (đồng), âm là còn dư. */
    private Long difference;
}
