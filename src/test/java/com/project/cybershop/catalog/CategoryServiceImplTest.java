package com.project.cybershop.catalog;

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
import com.project.cybershop.catalog.internal.service.impl.CategoryServiceImpl;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SpecificationRepository specificationRepository;

    @Mock
    private CategorySpecificationRepository categorySpecRepository;

    @Mock
    private ProductRepository productRepository;

    @Spy
    private CatalogMapper catalogMapper = new CatalogMapper();

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category("Bộ vi xử lý", "bo-vi-xu-ly", null, "CPU máy tính");
        sampleCategory.setId(1);
    }

    @Test
    void testCreateCategory_Success() {
        CreateCategoryRequest request = new CreateCategoryRequest("Bộ vi xử lý", null, null, "CPU");
        when(categoryRepository.existsBySlug("bo-vi-xu-ly")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(10);
            return cat;
        });

        CategoryResponse res = categoryService.createCategory(request);

        assertNotNull(res);
        assertEquals(10, res.id());
        assertEquals("bo-vi-xu-ly", res.slug());
        assertEquals("Bộ vi xử lý", res.name());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testCreateCategory_DuplicateSlugThrows() {
        CreateCategoryRequest request = new CreateCategoryRequest("Bộ vi xử lý", "cpu", null, "CPU");
        when(categoryRepository.existsBySlug("cpu")).thenReturn(true);

        assertThrows(BusinessException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testUpdateCategory_Success() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("CPU Intel & AMD", "cpu", null, "Mô tả mới");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.existsBySlugAndIdNot("cpu", 1)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        CategoryResponse res = categoryService.updateCategory(1, request);

        assertNotNull(res);
        assertEquals("CPU Intel & AMD", sampleCategory.getName());
        assertEquals("cpu", sampleCategory.getSlug());
    }

    @Test
    void testUpdateCategory_SelfParentThrows() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("CPU", "cpu", 1, "Mô tả");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));

        assertThrows(BusinessException.class, () -> categoryService.updateCategory(1, request));
    }

    @Test
    void testDeleteCategory_ThrowsWhenHasChildren() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.countByParentId(1)).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.deleteCategory(1));
        assertTrue(ex.getMessage().contains("chứa các danh mục con"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void testDeleteCategory_ThrowsWhenHasProducts() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.countByParentId(1)).thenReturn(0L);
        when(productRepository.countByCategoryId(1)).thenReturn(5L);

        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.deleteCategory(1));
        assertTrue(ex.getMessage().contains("sản phẩm liên kết"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void testDeleteCategory_Success() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.countByParentId(1)).thenReturn(0L);
        when(productRepository.countByCategoryId(1)).thenReturn(0L);

        categoryService.deleteCategory(1);

        verify(categorySpecRepository).deleteByCategoryId(1);
        verify(categoryRepository).delete(sampleCategory);
    }

    @Test
    void testConfigureCategorySpecifications_RemovesOldSpecs() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        Specification spec = new Specification("Socket");
        spec.setId(100);
        when(specificationRepository.findById(100)).thenReturn(Optional.of(spec));
        when(categorySpecRepository.findByCategoryIdAndSpecificationId(1, 100)).thenReturn(Optional.empty());
        when(categorySpecRepository.save(any(CategorySpecification.class))).thenAnswer(i -> i.getArgument(0));

        List<ConfigureCategorySpecRequest> requests = List.of(
                new ConfigureCategorySpecRequest(100, true, true, true, true)
        );

        List<CategorySpecResponse> result = categoryService.configureCategorySpecifications(1, requests);

        verify(categorySpecRepository).deleteByCategoryIdAndSpecificationIdNotIn(eq(1), eq(Set.of(100)));
        assertEquals(1, result.size());
        assertEquals("Socket", result.get(0).specificationName());
    }
}
