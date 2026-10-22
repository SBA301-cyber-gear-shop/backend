package com.project.cybershop.catalog.internal.mapper;

import com.project.cybershop.catalog.api.dto.response.BrandResponse;
import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.catalog.api.dto.response.CategorySpecResponse;
import com.project.cybershop.catalog.api.dto.response.ProductResponse;
import com.project.cybershop.catalog.api.dto.response.ProductVariantResponse;
import com.project.cybershop.catalog.api.dto.response.SpecificationResponse;
import com.project.cybershop.catalog.api.dto.response.SpecificationValueResponse;
import com.project.cybershop.catalog.internal.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogMapper {

    public BrandResponse toBrandResponse(Brand brand) {
        if (brand == null) return null;
        return new BrandResponse(brand.getId(), brand.getName(), brand.getLogoUrl(), brand.getDescription());
    }

    public SpecificationResponse toSpecResponse(Specification spec) {
        if (spec == null) return null;
        return new SpecificationResponse(spec.getId(), spec.getName());
    }

    public CategorySpecResponse toCategorySpecResponse(CategorySpecification cs) {
        if (cs == null) return null;
        return new CategorySpecResponse(
                cs.getId(),
                cs.getSpecification().getId(),
                cs.getSpecification().getName(),
                cs.getIsFilterable(),
                cs.getIsPrimary(),
                cs.getShowInMenu(),
                cs.getIsCompatibility()
        );
    }

    public com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse toSpecOptionResponse(SpecificationOption opt) {
        if (opt == null) return null;
        return new com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse(
                opt.getId(),
                opt.getSpecification().getId(),
                opt.getCode(),
                opt.getDisplayValue()
        );
    }

    public CategoryResponse toCategoryResponse(Category category) {
        if (category == null) return null;

        List<CategoryResponse> children = category.getChildren() != null
                ? category.getChildren().stream().map(this::toCategoryResponse).toList()
                : List.of();

        List<CategorySpecResponse> specs = category.getCategorySpecifications() != null
                ? category.getCategorySpecifications().stream().map(this::toCategorySpecResponse).toList()
                : List.of();

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getDescription(),
                children,
                specs
        );
    }

    public SpecificationValueResponse toSpecValueResponse(SpecificationValue sv) {
        if (sv == null) return null;
        Integer optionId = sv.getOption() != null ? sv.getOption().getId() : null;
        String optionCode = sv.getOption() != null ? sv.getOption().getCode() : null;
        return new SpecificationValueResponse(
                sv.getId(),
                sv.getSpecification().getId(),
                sv.getSpecification().getName(),
                optionId,
                optionCode,
                sv.getValue()
        );
    }

    public ProductVariantResponse toVariantResponse(ProductVariant variant) {
        if (variant == null) return null;

        List<SpecificationValueResponse> attributes = variant.getAttributeValues() != null
                ? variant.getAttributeValues().stream().map(this::toSpecValueResponse).toList()
                : List.of();

        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct() != null ? variant.getProduct().getId() : null,
                variant.getSerialCode(),
                variant.getVariantName(),
                variant.getSlug(),
                variant.getPrice(),
                variant.getOriginalPrice(),
                variant.getStockQuantity(),
                variant.getImageUrl(),
                attributes
        );
    }

    public ProductResponse toProductResponse(Product product) {
        if (product == null) return null;

        List<ProductVariantResponse> variants = product.getVariants() != null
                ? product.getVariants().stream().map(this::toVariantResponse).toList()
                : List.of();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand() != null ? product.getBrand().getId() : null,
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getWarrantyPeriodMonths(),
                product.getDescription(),
                product.getStatus() != null ? product.getStatus().name() : null,
                variants
        );
    }
}
