package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.catalog.internal.entity.ProductVariant;
import com.project.cybershop.catalog.internal.entity.SpecificationValue;
import com.project.cybershop.catalog.internal.repository.ProductVariantRepository;
import com.project.cybershop.catalog.internal.repository.SpecificationValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogPublicApiImpl implements CatalogPublicApi {

    private final ProductVariantRepository variantRepository;
    private final SpecificationValueRepository specValueRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ComponentCandidateDTO> getActiveCandidatesForBuild() {
        List<ProductVariant> variants = variantRepository.findAllActiveInStock();
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
    public Map<Integer, ComponentCandidateDTO> getCandidateMapByIds(List<Integer> variantIds) {
        return getActiveCandidatesForBuild().stream()
                .filter(dto -> variantIds.contains(dto.getVariantId()))
                .collect(Collectors.toMap(ComponentCandidateDTO::getVariantId, dto -> dto));
    }
}
