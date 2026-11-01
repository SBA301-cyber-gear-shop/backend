package com.project.cybershop.catalog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Một dòng category_specifications kèm danh mục, thông số và các option ENUM của thông số đó.
 * Dùng cho pcbuild dựng danh sách thông số bắt buộc (is_compatibility) và nhãn hiển thị option.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpecDto {
    private Integer categoryId;
    private String categorySlug;
    private String categoryName;
    private Integer specificationId;
    private String specificationName;
    private boolean compatibility;
    private boolean primary;
    private boolean filterable;

    @Builder.Default
    private List<Option> options = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String code;
        private String displayValue;
    }
}
