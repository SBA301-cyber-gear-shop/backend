package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.catalog.api.dto.response.CategoryResponse;
import com.project.cybershop.catalog.internal.entity.Brand;
import com.project.cybershop.catalog.internal.entity.CategorySpecification;
import com.project.cybershop.catalog.internal.entity.ProductVariant;
import com.project.cybershop.catalog.internal.entity.SpecificationOption;
import com.project.cybershop.catalog.internal.entity.SpecificationValue;
import com.project.cybershop.catalog.internal.repository.BrandRepository;
import com.project.cybershop.catalog.internal.repository.CategorySpecificationRepository;
import com.project.cybershop.catalog.internal.repository.ProductVariantRepository;
import com.project.cybershop.catalog.internal.repository.SpecificationOptionRepository;
import com.project.cybershop.catalog.internal.repository.SpecificationValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogPublicApiImpl implements CatalogPublicApi {

    private final ProductVariantRepository variantRepository;
    private final SpecificationValueRepository specValueRepository;
    private final BrandRepository brandRepository;
    private final CategorySpecificationRepository categorySpecificationRepository;
    private final SpecificationOptionRepository specificationOptionRepository;
    private final CategoryService categoryService;

    @Override
    @Transactional(readOnly = true)
    public List<ComponentCandidateDTO> getActiveCandidatesForBuild() {
        return toCandidates(variantRepository.findAllActiveInStock());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, ComponentCandidateDTO> getCandidateMapByIds(List<Integer> variantIds) {
        return getCandidatesByIds(variantIds).stream()
                .collect(Collectors.toMap(ComponentCandidateDTO::getVariantId, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentCandidateDTO> getCandidatesByIds(List<Integer> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) return Collections.emptyList();
        // Truy vấn theo id, không lọc tồn kho (sửa B18); product/category/brand nạp theo lô nhờ default_batch_fetch_size
        return toCandidates(variantRepository.findAllById(new LinkedHashSet<>(variantIds)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySpecDto> getSpecificationCatalog() {
        Map<Integer, List<CategorySpecDto.Option>> optionsBySpecId = new HashMap<>();
        for (SpecificationOption option : specificationOptionRepository.findAll()) {
            optionsBySpecId.computeIfAbsent(option.getSpecification().getId(), k -> new ArrayList<>())
                    .add(new CategorySpecDto.Option(option.getCode(), option.getDisplayValue()));
        }

        List<CategorySpecDto> result = new ArrayList<>();
        for (CategorySpecification cs : categorySpecificationRepository.findAll()) {
            Integer specId = cs.getSpecification().getId();
            result.add(CategorySpecDto.builder()
                    .categoryId(cs.getCategory().getId())
                    .categorySlug(cs.getCategory().getSlug())
                    .categoryName(cs.getCategory().getName())
                    .specificationId(specId)
                    .specificationName(cs.getSpecification().getName())
                    .compatibility(Boolean.TRUE.equals(cs.getIsCompatibility()))
                    .primary(Boolean.TRUE.equals(cs.getIsPrimary()))
                    .filterable(Boolean.TRUE.equals(cs.getIsFilterable()))
                    .options(optionsBySpecId.getOrDefault(specId, List.of()))
                    .build());
        }
        return result;
    }

    private List<ComponentCandidateDTO> toCandidates(List<ProductVariant> variants) {
        if (variants.isEmpty()) return Collections.emptyList();

        List<Integer> variantIds = variants.stream().map(ProductVariant::getId).toList();
        List<SpecificationValue> specValues = specValueRepository.findByProductVariantIdIn(variantIds);

        Map<Integer, Map<String, String>> specMap = new HashMap<>();
        Map<Integer, Map<Integer, String>> specIdMap = new HashMap<>();
        for (SpecificationValue sv : specValues) {
            specMap.computeIfAbsent(sv.getProductVariant().getId(), k -> new HashMap<>())
                   .put(sv.getSpecification().getName(), sv.getValue());
            if (sv.getSpecification() != null && sv.getSpecification().getId() != null) {
                specIdMap.computeIfAbsent(sv.getProductVariant().getId(), k -> new HashMap<>())
                       .put(sv.getSpecification().getId(), sv.getValue());
            }
        }

        return variants.stream().map(pv -> ComponentCandidateDTO.builder()
                .variantId(pv.getId())
                .productId(pv.getProduct().getId())
                .brandId(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getId() : null)
                .brandName(pv.getProduct().getBrand() != null ? pv.getProduct().getBrand().getName() : null)
                .categoryId(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getId() : null)
                .categorySlug(pv.getProduct().getCategory() != null ? pv.getProduct().getCategory().getSlug() : "other")
                .productName(pv.getProduct().getName())
                .variantName(pv.getVariantName())
                .price(pv.getPrice())
                .stockQuantity(pv.getStockQuantity())
                .imageUrl(pv.getImageUrl())
                .quantity(1)
                .specs(specMap.getOrDefault(pv.getId(), Collections.emptyMap()))
                .specValuesById(specIdMap.getOrDefault(pv.getId(), Collections.emptyMap()))
                .build()
        ).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllBrandNames() {
        return brandRepository.findAll().stream()
                .map(Brand::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getFlatCategories();
    }
}
