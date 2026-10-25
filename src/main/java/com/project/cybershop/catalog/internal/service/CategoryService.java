package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.dto.request.ConfigureCategorySpecRequest;
import com.project.cybershop.catalog.api.dto.request.CreateCategoryRequest;
import com.project.cybershop.catalog.api.dto.request.UpdateCategoryRequest;
import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.catalog.api.dto.response.CategorySpecResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);
    CategoryResponse updateCategory(Integer id, UpdateCategoryRequest request);
    void deleteCategory(Integer id);
    List<CategoryResponse> getCategoryTree();
    List<CategoryResponse> getFlatCategories();
    CategoryResponse getCategoryById(Integer id);
    CategoryResponse getCategoryBySlug(String slug);
    List<CategorySpecResponse> configureCategorySpecifications(Integer categoryId, List<ConfigureCategorySpecRequest> requests);
}
