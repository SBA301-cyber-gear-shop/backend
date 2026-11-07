package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một món trong bộ PC trả về frontend (docx mục 6). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildItemResponse {
    /** Mã slot: MONITOR, KEYBOARD, MOUSE, CPU, MAINBOARD, RAM, SSD, GPU, COOLER, CASE, PSU. */
    private String category;
    /** product_variants.id */
    private Integer id;
    private String name;
    private Integer qty;
    /** Số lượng tối đa thật = min(max_qty của slot, thông số các món khác); frontend dùng để khóa ô số lượng. */
    private Integer maxQty;
    /** Đơn giá (đồng). */
    private Long price;
}
