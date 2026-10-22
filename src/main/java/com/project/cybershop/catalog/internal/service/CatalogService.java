package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.dto.response.CatalogProductResponse;
import java.util.List;

public interface CatalogService {
    List<CatalogProductResponse> list();
}
