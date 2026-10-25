package com.project.cybershop.catalog.internal.service.impl;

import com.project.cybershop.catalog.api.dto.request.ConfigureCategorySpecRequest;
import com.project.cybershop.catalog.api.dto.request.CreateCategoryRequest;
import com.project.cybershop.catalog.api.dto.request.UpdateCategoryRequest;
import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.catalog.api.dto.response.CategorySpecResponse;
import com.project.cybershop.catalog.internal.entity.Category;
import com.project.cybershop.catalog.internal.entity.CategorySpecification;
import com.project.cybershop.catalog.internal.entity.Specification;
import com.project.cybershop.catalog.internal.mapper.CatalogMapper;
import com.project.cybershop.catalog.internal.repository.CategoryRepository;
import com.project.cybershop.catalog.internal.repository.CategorySpecificationRepository;
import com.project.cybershop.catalog.internal.repository.ProductRepository;
import com.project.cybershop.catalog.internal.repository.SpecificationRepository;
import com.project.cybershop.catalog.internal.service.CategoryService;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SpecificationRepository specificationRepository;
    private final CategorySpecificationRepository categorySpecRepository;
    private final ProductRepository productRepository;
    private final CatalogMapper catalogMapper;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String slug = request.slug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(request.name());
        } else {
            slug = toSlug(slug);
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

    @Override
    @Transactional
    public CategoryResponse updateCategory(Integer id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        String slug = request.slug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(request.name());
        } else {
            slug = toSlug(slug);
        }

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BusinessException("Slug danh mục đã tồn tại: " + slug);
        }

        Category parent = null;
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new BusinessException("Danh mục không thể làm cha của chính nó");
            }
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha với ID: " + request.parentId()));

            if (isDescendant(parent, id)) {
                throw new BusinessException("Không thể chọn danh mục con làm danh mục cha");
            }
        }

        category.setName(request.name());
        category.setSlug(slug);
        category.setParent(parent);
        category.setDescription(request.description());

        return catalogMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        if (categoryRepository.countByParentId(id) > 0) {
            throw new BusinessException("Không thể xóa danh mục đang chứa các danh mục con. Vui lòng xóa hoặc di chuyển danh mục con trước.");
        }

        if (productRepository.countByCategoryId(id) > 0) {
            throw new BusinessException("Không thể xóa danh mục đang có sản phẩm liên kết. Vui lòng xóa hoặc đổi danh mục cho sản phẩm trước.");
        }

        categorySpecRepository.deleteByCategoryId(id);
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        return categoryRepository.findAllRootCategoriesWithChildren().stream()
                .map(catalogMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getFlatCategories() {
        return categoryRepository.findAllOrderByName().stream()
                .map(catalogMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));
        return catalogMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với slug: " + slug));
        return catalogMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional
    public List<CategorySpecResponse> configureCategorySpecifications(Integer categoryId, List<ConfigureCategorySpecRequest> requests) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + categoryId));

        if (requests == null || requests.isEmpty()) {
            categorySpecRepository.deleteByCategoryId(categoryId);
            return Collections.emptyList();
        }

        Set<Integer> requestedSpecIds = requests.stream()
                .map(ConfigureCategorySpecRequest::specificationId)
                .collect(Collectors.toSet());

        // Xóa những thông số không còn nằm trong danh sách yêu cầu mới
        categorySpecRepository.deleteByCategoryIdAndSpecificationIdNotIn(categoryId, requestedSpecIds);

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

    private boolean isDescendant(Category current, Integer ancestorId) {
        Category parent = current.getParent();
        while (parent != null) {
            if (parent.getId().equals(ancestorId)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
