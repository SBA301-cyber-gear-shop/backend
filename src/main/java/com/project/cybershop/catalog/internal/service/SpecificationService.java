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
    private final com.project.cybershop.catalog.internal.repository.SpecificationOptionRepository specificationOptionRepository;
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

    @Transactional
    public com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse addOption(
            Integer specificationId,
            com.project.cybershop.catalog.api.dto.request.CreateSpecificationOptionRequest request) {
        Specification spec = specificationRepository.findById(specificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông số với ID: " + specificationId));

        if (specificationOptionRepository.existsBySpecificationIdAndCode(specificationId, request.code())) {
            throw new BusinessException("Mã giá trị (code) '" + request.code() + "' đã tồn tại cho thông số này.");
        }

        com.project.cybershop.catalog.internal.entity.SpecificationOption option =
                new com.project.cybershop.catalog.internal.entity.SpecificationOption(spec, request.code(), request.displayValue());
        return catalogMapper.toSpecOptionResponse(specificationOptionRepository.save(option));
    }

    @Transactional(readOnly = true)
    public List<com.project.cybershop.catalog.api.dto.response.SpecificationOptionResponse> getOptionsBySpecificationId(Integer specificationId) {
        if (!specificationRepository.existsById(specificationId)) {
            throw new ResourceNotFoundException("Không tìm thấy thông số với ID: " + specificationId);
        }
        return specificationOptionRepository.findBySpecificationId(specificationId).stream()
                .map(catalogMapper::toSpecOptionResponse)
                .toList();
    }
}
