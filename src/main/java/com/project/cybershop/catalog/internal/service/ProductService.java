package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.dto.*;
import com.project.cybershop.catalog.internal.entity.*;
import com.project.cybershop.catalog.internal.repository.*;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SpecificationRepository specificationRepository;
    private final CategorySpecificationRepository categorySpecRepository;
    private final SpecificationValueRepository specificationValueRepository;
    private final CatalogMapper catalogMapper;

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
        if (productVariantRepository.existsBySerialCode(variantDto.serialCode())) {
            throw new BusinessException("Mã SKU (serial_code) đã tồn tại: " + variantDto.serialCode());
        }

        ProductVariant variant = new ProductVariant(
                product,
                variantDto.serialCode(),
                variantDto.variantName(),
                variantDto.slug(),
                variantDto.price(),
                variantDto.originalPrice(),
                variantDto.stockQuantity(),
                variantDto.imageUrl()
        );
        ProductVariant savedVariant = productVariantRepository.save(variant);

        if (variantDto.attributes() != null && !variantDto.attributes().isEmpty()) {
            for (AttributeValueInputDto attrDto : variantDto.attributes()) {
                Specification spec = specificationRepository.findById(attrDto.specificationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông số kỹ thuật ID: " + attrDto.specificationId()));

                SpecificationValue specValue = new SpecificationValue(savedVariant, spec, attrDto.value());
                savedVariant.addAttributeValue(specificationValueRepository.save(specValue));
            }
        }

        return savedVariant;
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
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .map(catalogMapper::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProductsByStatus(ProductStatus status) {
        return productRepository.findByStatus(status).stream()
                .map(catalogMapper::toProductResponse)
                .toList();
    }
}
