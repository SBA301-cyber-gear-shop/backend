package com.project.cybershop.pcbuild.internal.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataCategoryOptionDto {
    private List<CategoryItem> categories;
    private List<SpecOptionItem> specOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryItem {
        private Integer id;
        private String name;
        private String slug;
        private boolean alreadyHasSlot;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecOptionItem {
        private Integer categoryId;
        private Integer specId;
        private String specName;
        private String code;
        private String displayValue;
    }
}

