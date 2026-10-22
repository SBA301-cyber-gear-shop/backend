package com.project.cybershop.catalog.internal.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductStatusConverter implements AttributeConverter<ProductStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProductStatus attribute) {
        if (attribute == null) {
            return ProductStatus.DRAFT.name();
        }
        return attribute.name();
    }

    @Override
    public ProductStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return ProductStatus.DRAFT;
        }
        try {
            return ProductStatus.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ProductStatus.DRAFT;
        }
    }
}
