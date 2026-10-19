package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.dto.request.AttributeValueInputDto;
import com.project.cybershop.catalog.api.dto.request.CreateVariantDto;
import com.project.cybershop.catalog.api.dto.request.UpdateVariantRequest;
import com.project.cybershop.catalog.api.dto.response.ProductVariantResponse;
import com.project.cybershop.catalog.internal.entity.*;
import com.project.cybershop.catalog.internal.mapper.CatalogMapper;
import com.project.cybershop.catalog.internal.repository.*;
import com.project.cybershop.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SpecificationRepository specificationRepository;

    @Mock
    private SpecificationOptionRepository specificationOptionRepository;

    @Mock
    private CategorySpecificationRepository categorySpecRepository;

    @Mock
    private SpecificationValueRepository specificationValueRepository;

    @Mock
    private CatalogMapper catalogMapper;

    @Mock
    private EntityManager em;

    @InjectMocks
    private ProductService productService;

    private Product mockProduct;
    private ProductVariant mockVariant;

    @BeforeEach
    void setUp() {
        Brand brand = new Brand("Kingston", null, "Kingston Tech");
        brand.setId(1);
        Category category = new Category("RAM", "ram", null, "Memory");
        category.setId(1);

        mockProduct = new Product("RAM Kingston Fury", brand, category, 36, "High performance RAM");
        mockProduct.setId(100);
        mockProduct.setStatus(ProductStatus.ACTIVE);

        mockVariant = new ProductVariant(
                mockProduct,
                "KST-DDR5-16",
                "16GB DDR5 5600",
                "kst-ddr5-16",
                BigDecimal.valueOf(1500000),
                BigDecimal.valueOf(1800000),
                10,
                "https://image.url"
        );
        mockVariant.setId(200);
    }

    @Test
    @DisplayName("Thêm biến thể thất bại khi giá bán <= 0")
    void testAddVariant_PriceZeroOrNegative_ThrowsException() {
        when(productRepository.findById(100)).thenReturn(Optional.of(mockProduct));

        CreateVariantDto req = new CreateVariantDto(
                "SKU-001", "Variant 1", null, BigDecimal.ZERO, null, 10, null, null
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> productService.addVariant(100, req));
        assertTrue(ex.getMessage().contains("Giá bán"));
    }

    @Test
    @DisplayName("Thêm biến thể thất bại khi tồn kho âm")
    void testAddVariant_NegativeStock_ThrowsException() {
        when(productRepository.findById(100)).thenReturn(Optional.of(mockProduct));

        CreateVariantDto req = new CreateVariantDto(
                "SKU-001", "Variant 1", null, BigDecimal.valueOf(100000), null, -5, null, null
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> productService.addVariant(100, req));
        assertTrue(ex.getMessage().contains("tồn kho"));
    }

    @Test
    @DisplayName("Thêm biến thể thất bại khi giá gốc nhỏ hơn giá bán")
    void testAddVariant_OriginalPriceLowerThanPrice_ThrowsException() {
        when(productRepository.findById(100)).thenReturn(Optional.of(mockProduct));

        CreateVariantDto req = new CreateVariantDto(
                "SKU-001", "Variant 1", null, BigDecimal.valueOf(200000), BigDecimal.valueOf(150000), 5, null, null
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> productService.addVariant(100, req));
        assertTrue(ex.getMessage().contains("Giá gốc"));
    }

    @Test
    @DisplayName("Thêm biến thể thất bại khi SKU đã tồn tại")
    void testAddVariant_DuplicateSku_ThrowsException() {
        when(productRepository.findById(100)).thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.existsBySerialCode("KST-DDR5-16")).thenReturn(true);

        CreateVariantDto req = new CreateVariantDto(
                "KST-DDR5-16", "Variant 1", null, BigDecimal.valueOf(200000), null, 5, null, null
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> productService.addVariant(100, req));
        assertTrue(ex.getMessage().contains("Mã SKU"));
    }

    @Test
    @DisplayName("Thêm biến thể thành công với Option ID")
    void testAddVariant_WithOption_Success() {
        when(productRepository.findById(100)).thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.existsBySerialCode("SKU-NEW")).thenReturn(false);

        Specification spec = new Specification("Chuẩn RAM");
        spec.setId(10);
        when(specificationRepository.findById(10)).thenReturn(Optional.of(spec));

        SpecificationOption opt = new SpecificationOption(spec, "DDR5", "DDR5");
        opt.setId(50);
        when(specificationOptionRepository.findById(50)).thenReturn(Optional.of(opt));

        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(specificationValueRepository.save(any(SpecificationValue.class))).thenAnswer(inv -> inv.getArgument(0));

        when(catalogMapper.toVariantResponse(any())).thenReturn(new ProductVariantResponse(
                300, 100, "SKU-NEW", "Variant New", null, BigDecimal.valueOf(100000), null, 5, null, List.of()
        ));

        CreateVariantDto req = new CreateVariantDto(
                "SKU-NEW", "Variant New", null, BigDecimal.valueOf(100000), null, 5, null,
                List.of(new AttributeValueInputDto(10, 50, null))
        );

        ProductVariantResponse res = productService.addVariant(100, req);
        assertNotNull(res);
        assertEquals("SKU-NEW", res.serialCode());
    }

    @Test
    @DisplayName("Cập nhật biến thể thành công")
    void testUpdateVariant_Success() {
        when(productRepository.existsById(100)).thenReturn(true);
        when(productVariantRepository.findWithAttributesById(200)).thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.findBySerialCode("KST-DDR5-16-UPDATED")).thenReturn(Optional.empty());
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateVariantRequest updateReq = new UpdateVariantRequest(
                "KST-DDR5-16-UPDATED",
                "16GB DDR5 6000",
                "kst-ddr5-16-updated",
                BigDecimal.valueOf(1700000),
                BigDecimal.valueOf(2000000),
                20,
                "https://image.url/updated",
                List.of()
        );

        when(catalogMapper.toVariantResponse(any())).thenReturn(new ProductVariantResponse(
                200, 100, "KST-DDR5-16-UPDATED", "16GB DDR5 6000", "kst-ddr5-16-updated",
                BigDecimal.valueOf(1700000), BigDecimal.valueOf(2000000), 20, "https://image.url/updated", List.of()
        ));

        ProductVariantResponse response = productService.updateVariant(100, 200, updateReq);
        assertNotNull(response);
        assertEquals("KST-DDR5-16-UPDATED", response.serialCode());
    }

    @Test
    @DisplayName("Xóa biến thể thất bại khi đã có trong đơn hàng")
    void testDeleteVariant_InOrderItems_ThrowsException() {
        when(productRepository.existsById(100)).thenReturn(true);
        when(productVariantRepository.findById(200)).thenReturn(Optional.of(mockVariant));

        Query mockQuery = mock(Query.class);
        when(em.createNativeQuery(contains("order_items"))).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("variantId"), eq(200))).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(2L); // 2 đơn hàng

        BusinessException ex = assertThrows(BusinessException.class, () -> productService.deleteVariant(100, 200));
        assertTrue(ex.getMessage().contains("đã phát sinh"));
        verify(productVariantRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa biến thể thành công và chuyển trạng thái Product sang DRAFT nếu hết variant")
    void testDeleteVariant_Success_SwitchesProductToDraft() {
        when(productRepository.existsById(100)).thenReturn(true);
        when(productVariantRepository.findById(200)).thenReturn(Optional.of(mockVariant));

        Query mockOrderQuery = mock(Query.class);
        when(em.createNativeQuery(contains("order_items"))).thenReturn(mockOrderQuery);
        when(mockOrderQuery.setParameter(eq("variantId"), eq(200))).thenReturn(mockOrderQuery);
        when(mockOrderQuery.getSingleResult()).thenReturn(0L);

        Query mockCartQuery = mock(Query.class);
        when(em.createNativeQuery(contains("cart_items"))).thenReturn(mockCartQuery);
        when(mockCartQuery.setParameter(eq("variantId"), eq(200))).thenReturn(mockCartQuery);

        // Sau khi xóa, danh sách variant rỗng
        when(productVariantRepository.findByProductId(100)).thenReturn(new ArrayList<>());
        when(productRepository.findById(100)).thenReturn(Optional.of(mockProduct));

        productService.deleteVariant(100, 200);

        verify(productVariantRepository).delete(mockVariant);
        assertEquals(ProductStatus.DRAFT, mockProduct.getStatus());
        verify(productRepository).save(mockProduct);
    }
}
