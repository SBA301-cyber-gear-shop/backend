package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.internal.entity.Brand;
import com.project.cybershop.catalog.internal.entity.Category;
import com.project.cybershop.catalog.internal.entity.Product;
import com.project.cybershop.catalog.internal.entity.ProductVariant;
import com.project.cybershop.catalog.internal.entity.Specification;
import com.project.cybershop.catalog.internal.entity.SpecificationValue;
import com.project.cybershop.catalog.internal.repository.BrandRepository;
import com.project.cybershop.catalog.internal.repository.ProductVariantRepository;
import com.project.cybershop.catalog.internal.repository.SpecificationValueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogPublicApiImpl - Kiểm thử truy vấn ứng viên linh kiện kho và ghép thông số kỹ thuật")
class CatalogPublicApiImplTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private SpecificationValueRepository specValueRepository;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private CatalogPublicApiImpl catalogPublicApi;

    @Test
    @DisplayName("Khi không có biến thể nào còn hàng -> Trả về danh sách rỗng")
    void shouldReturnEmptyListWhenNoVariantsInStock() {
        when(variantRepository.findAllActiveInStock()).thenReturn(List.of());

        var result = catalogPublicApi.getActiveCandidatesForBuild();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Ghép đúng thông số kỹ thuật và category slug từ thực thể CSDL sang DTO")
    void shouldMapVariantsAndSpecsCorrectly() {
        Category category = new Category();
        category.setId(1);
        category.setSlug("cpu");
        category.setName("Bộ vi xử lý");

        Brand brand = new Brand();
        brand.setId(1);
        brand.setName("Intel");

        Product product = new Product();
        product.setId(1);
        product.setCategory(category);
        product.setBrand(brand);
        product.setName("Intel Core i5-13400F");

        ProductVariant pv = new ProductVariant();
        pv.setId(101);
        pv.setProduct(product);
        pv.setVariantName("Tray");
        pv.setPrice(new BigDecimal("4690000"));
        pv.setStockQuantity(50);

        Specification spec = new Specification();
        spec.setId(1);
        spec.setName("Socket");

        SpecificationValue sv = new SpecificationValue();
        sv.setId(1001);
        sv.setProductVariant(pv);
        sv.setSpecification(spec);
        sv.setValue("LGA1700");

        when(variantRepository.findAllActiveInStock()).thenReturn(List.of(pv));
        when(specValueRepository.findByProductVariantIdIn(List.of(101))).thenReturn(List.of(sv));

        var candidates = catalogPublicApi.getActiveCandidatesForBuild();

        assertThat(candidates).hasSize(1);
        var dto = candidates.get(0);
        assertThat(dto.getVariantId()).isEqualTo(101);
        assertThat(dto.getCategorySlug()).isEqualTo("cpu");
        assertThat(dto.getProductName()).isEqualTo("Intel Core i5-13400F");
        assertThat(dto.getBrandId()).isEqualTo(1);
        assertThat(dto.getBrandName()).isEqualTo("Intel");
        assertThat(dto.getSpec("Socket")).isEqualTo("LGA1700");
    }

    @Test
    @DisplayName("getCandidateMapByIds: Lọc và trả về Map ứng viên theo danh sách ID yêu cầu")
    void shouldReturnCandidateMapByIds() {
        Category category = new Category();
        category.setId(4);
        category.setSlug("gpu");
        category.setName("Card màn hình");

        Product product = new Product();
        product.setId(1);
        product.setCategory(category);
        product.setName("RTX 4060");

        ProductVariant pv = new ProductVariant();
        pv.setId(104);
        pv.setProduct(product);
        pv.setPrice(new BigDecimal("7990000"));

        when(variantRepository.findAllActiveInStock()).thenReturn(List.of(pv));
        when(specValueRepository.findByProductVariantIdIn(anyList())).thenReturn(List.of());

        var map = catalogPublicApi.getCandidateMapByIds(List.of(104));

        assertThat(map).containsKey(104);
        assertThat(map.get(104).getProductName()).isEqualTo("RTX 4060");
    }

    @Test
    @DisplayName("getAllBrandNames: Lấy toàn bộ tên thương hiệu từ Database bảng brands")
    void shouldReturnAllBrandNamesFromDatabase() {
        Brand b1 = new Brand(); b1.setId(1); b1.setName("Intel");
        Brand b2 = new Brand(); b2.setId(2); b2.setName("Kingston");
        when(brandRepository.findAll()).thenReturn(List.of(b1, b2));

        var brands = catalogPublicApi.getAllBrandNames();

        assertThat(brands).containsExactly("Intel", "Kingston");
    }
}
