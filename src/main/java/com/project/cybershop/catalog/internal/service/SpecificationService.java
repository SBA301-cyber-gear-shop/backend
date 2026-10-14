package com.project.cybershop.catalog.internal.service;

import com.project.cybershop.catalog.api.dto.request.CreateSpecificationRequest;
import com.project.cybershop.catalog.api.dto.response.SpecificationResponse;
import com.project.cybershop.catalog.internal.entity.Specification;
import com.project.cybershop.catalog.internal.mapper.CatalogMapper;
import com.project.cybershop.catalog.internal.repository.SpecificationRepository;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecificationService {

    private final SpecificationRepository specificationRepository;
    private final CatalogMapper catalogMapper;

    @Transactional
    public SpecificationResponse createSpecification(CreateSpecificationRequest request) {
        if (specificationRepository.existsByName(request.name())) {
            throw new BusinessException("Thông số kỹ thuật đã tồn tại: " + request.name());
        }
        Specification spec = new Specification(request.name());
        return catalogMapper.toSpecResponse(specificationRepository.save(spec));
    }

    @Transactional(readOnly = true)
    public List<SpecificationResponse> getAllSpecifications() {
        return specificationRepository.findAll().stream()
                .map(catalogMapper::toSpecResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpecificationResponse getSpecificationById(Integer id) {
        Specification spec = specificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông số với ID: " + id));
        return catalogMapper.toSpecResponse(spec);
    }
}
