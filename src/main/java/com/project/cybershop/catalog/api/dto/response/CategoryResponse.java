package com.project.cybershop.catalog.api.dto.response;

import java.util.List;

public record CategoryResponse(
    Integer id,
    String name,
    String slug,
    Integer parentId,
    String description,
    List<CategoryResponse> children,
    List<CategorySpecResponse> specifications
) {}
