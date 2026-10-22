package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.HardwareCompatibilityValidator;
import com.project.cybershop.pcbuild.internal.service.PcCustomService;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PcCustomServiceImpl implements PcCustomService {

    private final PcCustomRepository pcCustomRepository;
    private final CatalogPublicApi catalogPublicApi;
    private final HardwareCompatibilityValidator validator;

    @Override
    @Transactional
    public void saveBuildForUser(Integer buildId, Integer userId) {
        PcCustom custom = pcCustomRepository.findById(buildId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình #" + buildId));
        custom.setUserId(userId);
        pcCustomRepository.save(custom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompatibleReplacementResponse> getCompatibleReplacements(String role, Integer currentCpuId, Integer currentMbId) {
        List<ComponentCandidateDTO> candidates = catalogPublicApi.getActiveCandidatesForBuild();
        List<Integer> idsToFetch = new java.util.ArrayList<>();
        if (currentCpuId != null) idsToFetch.add(currentCpuId);
        if (currentMbId != null) idsToFetch.add(currentMbId);
        Map<Integer, ComponentCandidateDTO> map = catalogPublicApi.getCandidateMapByIds(idsToFetch);
        ComponentCandidateDTO cpu = currentCpuId != null ? map.get(currentCpuId) : null;
        ComponentCandidateDTO mb = currentMbId != null ? map.get(currentMbId) : null;

        return candidates.stream()
                .filter(c -> c.getCategorySlug().equalsIgnoreCase(role))
                .map(c -> {
                    boolean ok = true;
                    String note = "Tương thích tốt";

                    if ("mainboard".equalsIgnoreCase(role) && cpu != null) {
                        String cpuSocket = cpu.getSpec("Socket");
                        String mbSocket = c.getSpec("Socket");
                        if (cpuSocket != null && !cpuSocket.equalsIgnoreCase(mbSocket)) {
                            ok = false;
                            note = "Lệch socket với CPU (Cần " + cpuSocket + ")";
                        }
                    } else if ("cpu".equalsIgnoreCase(role) && mb != null) {
                        String mbSocket = mb.getSpec("Socket");
                        String cpuSocket = c.getSpec("Socket");
                        if (mbSocket != null && !mbSocket.equalsIgnoreCase(cpuSocket)) {
                            ok = false;
                            note = "Lệch socket với Bo mạch (Cần " + mbSocket + ")";
                        }
                    }

                    return CompatibleReplacementResponse.builder()
                            .variantId(c.getVariantId())
                            .productName(c.getProductName())
                            .variantName(c.getVariantName())
                            .price(c.getPrice())
                            .imageUrl(c.getImageUrl())
                            .stockQuantity(c.getStockQuantity())
                            .specs(c.getSpecs())
                            .isCompatible(ok)
                            .compatibilityNote(note)
                            .build();
                })
                .sorted(Comparator.comparing(CompatibleReplacementResponse::isCompatible).reversed()
                        .thenComparing(CompatibleReplacementResponse::getPrice))
                .toList();
    }
}
