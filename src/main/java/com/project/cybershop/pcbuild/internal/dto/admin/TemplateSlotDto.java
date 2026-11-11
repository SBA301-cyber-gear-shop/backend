package com.project.cybershop.pcbuild.internal.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSlotDto {
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private String categorySlug;
    private Integer stepOrder;
    private boolean required;
    private Integer minQty;
    private Integer maxQty;

    // Điều kiện kích hoạt bổ sung (nếu có)
    private Integer requiredIfCategoryId;
    private String requiredIfCategoryName;
    private Integer requiredIfSpecId;
    private String requiredIfSpecName;
    private String requiredIfValue;
}

