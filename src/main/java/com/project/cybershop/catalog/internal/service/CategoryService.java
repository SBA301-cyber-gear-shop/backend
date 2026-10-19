package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.catalog.api.dto.response.CategorySpecResponse;
import com.project.cybershop.catalog.api.dto.request.ConfigureCategorySpecRequest;
import com.project.cybershop.catalog.api.dto.request.CreateCategoryRequest;
import com.project.cybershop.catalog.internal.entity.Category;
import com.project.cybershop.catalog.internal.entity.CategorySpecification;
import com.project.cybershop.catalog.internal.entity.Specification;
import com.project.cybershop.catalog.internal.mapper.CatalogMapper;
import com.project.cybershop.catalog.internal.repository.CategoryRepository;
import com.project.cybershop.catalog.internal.repository.CategorySpecificationRepository;
import com.project.cybershop.catalog.internal.repository.SpecificationRepository;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SpecificationRepository specificationRepository;
    private final CategorySpecificationRepository categorySpecRepository;
    private final CatalogMapper catalogMapper;

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String slug = request.slug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(request.name());
        }

        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessException("Slug danh mục đã tồn tại: " + slug);
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha với ID: " + request.parentId()));
        }

        Category category = new Category(request.name(), slug, parent, request.description());
        return catalogMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public List<CategorySpecResponse> configureCategorySpecifications(Integer categoryId, List<ConfigureCategorySpecRequest> requests) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + categoryId));

        List<CategorySpecification> savedSpecs = new ArrayList<>();
        for (ConfigureCategorySpecRequest req : requests) {
            Specification spec = specificationRepository.findById(req.specificationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông số với ID: " + req.specificationId()));

            CategorySpecification catSpec = categorySpecRepository
                    .findByCategoryIdAndSpecificationId(categoryId, req.specificationId())
                    .orElse(new CategorySpecification(category, spec, req.isFilterable(), req.isPrimary(), req.showInMenu()));

            catSpec.setIsFilterable(req.isFilterable() != null ? req.isFilterable() : false);
            catSpec.setIsPrimary(req.isPrimary() != null ? req.isPrimary() : false);
            catSpec.setShowInMenu(req.showInMenu() != null ? req.showInMenu() : false);
            catSpec.setIsCompatibility(req.isCompatibility() != null ? req.isCompatibility() : false);

            savedSpecs.add(categorySpecRepository.save(catSpec));
        }

        return savedSpecs.stream().map(catalogMapper::toCategorySpecResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        return categoryRepository.findAllRootCategoriesWithChildren().stream()
                .map(catalogMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));
        return catalogMapper.toCategoryResponse(category);
    }

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
