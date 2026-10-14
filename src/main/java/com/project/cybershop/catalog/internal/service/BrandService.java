package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.dto.response.BrandResponse;
import com.project.cybershop.catalog.api.dto.request.CreateBrandRequest;
import com.project.cybershop.catalog.internal.entity.Brand;
import com.project.cybershop.catalog.internal.mapper.CatalogMapper;
import com.project.cybershop.catalog.internal.repository.BrandRepository;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final CatalogMapper catalogMapper;

    @Transactional
    public BrandResponse createBrand(CreateBrandRequest request) {
        if (brandRepository.existsByName(request.name())) {
            throw new BusinessException("Thương hiệu đã tồn tại: " + request.name());
        }
        Brand brand = new Brand(request.name(), request.logoUrl(), request.description());
        return catalogMapper.toBrandResponse(brandRepository.save(brand));
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(catalogMapper::toBrandResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + id));
        return catalogMapper.toBrandResponse(brand);
    }
}
