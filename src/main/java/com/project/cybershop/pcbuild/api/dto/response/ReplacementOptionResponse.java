package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một lựa chọn trong dropdown đổi món (bước 9): chỉ gồm món tương thích với phần còn lại của bộ PC. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplacementOptionResponse {
    /** product_variants.id */
    private Integer id;
    private String name;
    private String brand;
    /** Đơn giá (đồng). */
    private Long price;
    /** Số lượng sẽ dùng khi chọn món này: giữ số lượng hiện tại, kẹp trong [min_qty, maxQty]. */
    private Integer qty;
    /** Số lượng tối đa thật với phần còn lại của bộ PC; frontend khóa ô số lượng theo số này. */
    private Integer maxQty;
    /** true nếu đây là món khách đang chọn. */
    private boolean current;
}
