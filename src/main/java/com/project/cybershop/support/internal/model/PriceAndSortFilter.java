package com.project.cybershop.support.internal.model;

public record PriceAndSortFilter(
        Double minPrice,
        Double maxPrice,
        String orderByClause,
        String categoryHint
) {}
