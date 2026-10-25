package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.dto.request.AttributeValueInputDto;
import com.project.cybershop.catalog.api.dto.request.CreateProductRequest;
import com.project.cybershop.catalog.api.dto.request.CreateVariantDto;
import com.project.cybershop.catalog.api.dto.request.UpdateProductRequest;
import com.project.cybershop.catalog.api.dto.request.UpdateVariantRequest;
import com.project.cybershop.catalog.api.dto.response.ProductResponse;
import com.project.cybershop.catalog.api.dto.response.ProductVariantResponse;
import com.project.cybershop.catalog.internal.entity.*;
import com.project.cybershop.catalog.internal.mapper.CatalogMapper;
import com.project.cybershop.catalog.internal.repository.*;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SpecificationRepository specificationRepository;
    private final SpecificationOptionRepository specificationOptionRepository;
    private final CategorySpecificationRepository categorySpecRepository;
    private final SpecificationValueRepository specificationValueRepository;
    private final CatalogMapper catalogMapper;
    private final EntityManager em;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            SpecificationRepository specificationRepository,
            SpecificationOptionRepository specificationOptionRepository,
            CategorySpecificationRepository categorySpecRepository,
            SpecificationValueRepository specificationValueRepository,
            CatalogMapper catalogMapper,
            EntityManager em) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.specificationRepository = specificationRepository;
        this.specificationOptionRepository = specificationOptionRepository;
        this.categorySpecRepository = categorySpecRepository;
        this.specificationValueRepository = specificationValueRepository;
        this.catalogMapper = catalogMapper;
        this.em = em;
    }

    private void validateVariantPriceAndStock(BigDecimal price, BigDecimal originalPrice, Integer stockQuantity) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Giá bán phiên bản phải lớn hơn 0");
        }
        if (stockQuantity != null && stockQuantity < 0) {
            throw new BusinessException("Số lượng tồn kho không được âm");
        }
        if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0 && originalPrice.compareTo(price) < 0) {
            throw new BusinessException("Giá gốc (giá tham chiếu) không được nhỏ hơn giá bán");
        }
    }

    private void attachAttributes(ProductVariant variant, List<AttributeValueInputDto> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        for (AttributeValueInputDto attrDto : attributes) {
            Specification spec = specificationRepository.findById(attrDto.specificationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông số kỹ thuật ID: " + attrDto.specificationId()));

            SpecificationOption option = null;
            String val = attrDto.value();
            if (attrDto.optionId() != null) {
                option = specificationOptionRepository.findById(attrDto.optionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tùy chọn thông số ID: " + attrDto.optionId()));
                if (!option.getSpecification().getId().equals(spec.getId())) {
                    throw new BusinessException("Tùy chọn '" + option.getDisplayValue() + "' không thuộc thông số '" + spec.getName() + "'");
                }
                if (val == null || val.isBlank()) {
                    val = option.getDisplayValue();
                }
            } else if (val == null || val.isBlank()) {
                throw new BusinessException("Giá trị thông số '" + spec.getName() + "' không được để trống");
            }

            SpecificationValue specValue = new SpecificationValue(variant, spec, option, val.trim());
            variant.addAttributeValue(specificationValueRepository.save(specValue));
        }
    }

    @Transactional
    public ProductResponse createProductDraft(CreateProductRequest request) {
        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + request.brandId()));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.categoryId()));

        Product product = new Product(
                request.name(),
                brand,
                category,
                request.warrantyPeriodMonths(),
                request.description()
        );
        product.setStatus(ProductStatus.DRAFT);
        Product savedProduct = productRepository.save(product);

        if (request.variants() != null && !request.variants().isEmpty()) {
            for (CreateVariantDto varDto : request.variants()) {
                addVariantInternal(savedProduct, varDto);
            }
        }

        return catalogMapper.toProductResponse(productRepository.findWithDetailsById(savedProduct.getId()).orElse(savedProduct));
    }

    @Transactional
    public ProductVariantResponse addVariant(Integer productId, CreateVariantDto variantDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        ProductVariant variant = addVariantInternal(product, variantDto);
        return catalogMapper.toVariantResponse(variant);
    }

    private ProductVariant addVariantInternal(Product product, CreateVariantDto variantDto) {
        validateVariantPriceAndStock(variantDto.price(), variantDto.originalPrice(), variantDto.stockQuantity());

        if (productVariantRepository.existsBySerialCode(variantDto.serialCode())) {
            throw new BusinessException("Mã SKU (serial_code) đã tồn tại: " + variantDto.serialCode());
        }

        if (variantDto.slug() != null && !variantDto.slug().isBlank() && productVariantRepository.existsBySlug(variantDto.slug())) {
            throw new BusinessException("Slug biến thể đã tồn tại: " + variantDto.slug());
        }

        ProductVariant variant = new ProductVariant(
                product,
                variantDto.serialCode().trim(),
                variantDto.variantName(),
                variantDto.slug(),
                variantDto.price(),
                variantDto.originalPrice(),
                variantDto.stockQuantity(),
                variantDto.imageUrl()
        );
        ProductVariant savedVariant = productVariantRepository.save(variant);

        attachAttributes(savedVariant, variantDto.attributes());

        return savedVariant;
    }

    @Transactional
    public ProductVariantResponse updateVariant(Integer productId, Integer variantId, UpdateVariantRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }

        ProductVariant variant = productVariantRepository.findWithAttributesById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể với ID: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException("Biến thể ID " + variantId + " không thuộc sản phẩm ID " + productId);
        }

        validateVariantPriceAndStock(request.price(), request.originalPrice(), request.stockQuantity());

        String newSku = request.serialCode().trim();
        Optional<ProductVariant> existingWithSku = productVariantRepository.findBySerialCode(newSku);
        if (existingWithSku.isPresent() && !existingWithSku.get().getId().equals(variantId)) {
            throw new BusinessException("Mã SKU (serial_code) '" + newSku + "' đã tồn tại trên một biến thể khác");
        }

        if (request.slug() != null && !request.slug().isBlank()) {
            Optional<ProductVariant> existingWithSlug = productVariantRepository.findBySlug(request.slug().trim());
            if (existingWithSlug.isPresent() && !existingWithSlug.get().getId().equals(variantId)) {
                throw new BusinessException("Slug '" + request.slug() + "' đã tồn tại trên một biến thể khác");
            }
        }

        variant.setSerialCode(newSku);
        variant.setVariantName(request.variantName());
        variant.setSlug(request.slug() != null ? request.slug().trim() : null);
        variant.setPrice(request.price());
        variant.setOriginalPrice(request.originalPrice());
        variant.setStockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0);
        variant.setImageUrl(request.imageUrl());

        variant.getAttributeValues().clear();
        attachAttributes(variant, request.attributes());

        ProductVariant saved = productVariantRepository.save(variant);
        return catalogMapper.toVariantResponse(saved);
    }

    @Transactional
    public void deleteVariant(Integer productId, Integer variantId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể với ID: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException("Biến thể ID " + variantId + " không thuộc sản phẩm ID " + productId);
        }

        Number orderItemCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM order_items WHERE product_variant_id = :variantId"
        ).setParameter("variantId", variantId).getSingleResult();

        if (orderItemCount != null && orderItemCount.longValue() > 0) {
            throw new BusinessException("Không thể xóa biến thể '" + variant.getVariantName() +
                    "' (SKU: " + variant.getSerialCode() + ") vì đã phát sinh trong " + orderItemCount + " đơn hàng. Vui lòng cập nhật tồn kho về 0 thay vì xóa.");
        }

        try {
            em.createNativeQuery("DELETE FROM cart_items WHERE product_variant_id = :variantId")
                    .setParameter("variantId", variantId)
                    .executeUpdate();
        } catch (Exception ignored) {}

        productVariantRepository.delete(variant);
        productVariantRepository.flush();

        List<ProductVariant> remaining = productVariantRepository.findByProductId(productId);
        if (remaining.isEmpty()) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null && product.getStatus() == ProductStatus.ACTIVE) {
                product.setStatus(ProductStatus.DRAFT);
                productRepository.save(product);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariantsByProductId(Integer productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }
        return productVariantRepository.findWithAttributesByProductId(productId).stream()
                .map(catalogMapper::toVariantResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantById(Integer productId, Integer variantId) {
        ProductVariant variant = productVariantRepository.findWithAttributesById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể với ID: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException("Biến thể ID " + variantId + " không thuộc sản phẩm ID " + productId);
        }
        return catalogMapper.toVariantResponse(variant);
    }

    @Transactional
    public ProductResponse publishProduct(Integer productId) {
        Product product = productRepository.findWithDetailsById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            throw new BusinessException("Sản phẩm phải có ít nhất 1 phiên bản (variant) để đăng bán");
        }

        List<CategorySpecification> primarySpecs = categorySpecRepository
                .findPrimarySpecsByCategoryId(product.getCategory().getId());

        if (!primarySpecs.isEmpty()) {
            Set<Integer> requiredSpecIds = primarySpecs.stream()
                    .map(cs -> cs.getSpecification().getId())
                    .collect(Collectors.toSet());

            for (ProductVariant variant : product.getVariants()) {
                Set<Integer> filledSpecIds = variant.getAttributeValues().stream()
                        .map(av -> av.getSpecification().getId())
                        .collect(Collectors.toSet());

                for (CategorySpecification cs : primarySpecs) {
                    if (!filledSpecIds.contains(cs.getSpecification().getId())) {
                        throw new BusinessException("Phiên bản '" + variant.getVariantName() +
                                "' còn thiếu thông số nổi bật bắt buộc: " + cs.getSpecification().getName());
                    }
                }
            }
        }

        product.setStatus(ProductStatus.ACTIVE);
        return catalogMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        return catalogMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts() {
        return listActiveProducts(null, null);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts(Integer categoryId, String categorySlug) {
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE);
        } else if (categorySlug != null && !categorySlug.isBlank()) {
            products = productRepository.findByCategorySlugAndStatus(categorySlug.trim().toLowerCase(), ProductStatus.ACTIVE);
        } else {
            products = productRepository.findByStatus(ProductStatus.ACTIVE);
        }
        return products.stream()
                .map(catalogMapper::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listAllProducts() {
        return productRepository.findAllWithDetails().stream()
                .map(catalogMapper::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProductsByStatus(ProductStatus status) {
        return productRepository.findByStatusWithDetails(status).stream()
                .map(catalogMapper::toProductResponse)
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(Integer productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + request.brandId()));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.categoryId()));

        product.setName(request.name().trim());
        product.setBrand(brand);
        product.setCategory(category);
        product.setWarrantyPeriodMonths(request.warrantyPeriodMonths());
        product.setDescription(request.description());
        if (request.status() != null) {
            product.setStatus(request.status());
        }

        Product saved = productRepository.save(product);
        return catalogMapper.toProductResponse(productRepository.findWithDetailsById(saved.getId()).orElse(saved));
    }

    @Transactional
    public void deleteProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        Number orderCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM order_items oi JOIN product_variants pv ON oi.product_variant_id = pv.id WHERE pv.product_id = :productId"
        ).setParameter("productId", productId).getSingleResult();

        if (orderCount != null && orderCount.longValue() > 0) {
            throw new BusinessException("Không thể xóa sản phẩm '" + product.getName() +
                    "' vì đã có biến thể phát sinh trong " + orderCount + " đơn hàng. Vui lòng chuyển trạng thái sang ARCHIVED thay vì xóa.");
        }

        try {
            em.createNativeQuery("DELETE FROM cart_items WHERE product_variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId)")
                    .setParameter("productId", productId).executeUpdate();
        } catch (Exception ignored) {}

        productRepository.delete(product);
    }
}
