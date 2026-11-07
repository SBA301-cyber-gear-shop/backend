package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.SlotMeta;

/**
 * Một dòng của bộ PC: slot của khuôn, món đã chọn và số lượng.
 *
 * @param slot null khi món thuộc loại không có trong khuôn (chỉ xảy ra ở API validate)
 */
public record BuildLine(SlotMeta slot, ComponentCandidateDTO part, int quantity) {

    /** Đơn giá theo đồng; giá trong kho là số chẵn đồng, có phần lẻ thì ném ArithmeticException (D10). */
    public long unitPrice() {
        return part.getPrice().longValueExact();
    }

    public long lineTotal() {
        return unitPrice() * quantity;
    }

    public Integer categoryId() {
        return part.getCategoryId();
    }

    public String displayName() {
        return part.getProductName();
    }
}
