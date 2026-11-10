package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Một bộ PC khách đã lưu (pc_customs + pc_custom_items). Bảng không có cột tên, ngày lưu, ngân sách nên không trả.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedBuildResponse {
    /** pc_customs.id */
    private Integer buildId;
    /** Tổng tiền lúc lưu (đồng). */
    private Long total;
    /** Món theo thứ tự lúc lưu; `price` là giá lúc lưu, `maxQty` luôn null (mở lại thì /validate trả số thật). */
    @Builder.Default
    private List<BuildItemResponse> items = new ArrayList<>();
}
