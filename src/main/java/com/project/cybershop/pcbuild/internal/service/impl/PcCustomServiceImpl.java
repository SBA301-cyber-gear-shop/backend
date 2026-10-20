package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.SavePcBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildDetailResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildSummaryResponse;
import com.project.cybershop.pcbuild.api.dto.response.SelectedPartResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.pcbuild.internal.entity.PcCustomItem;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.HardwareCompatibilityValidator;
import com.project.cybershop.pcbuild.internal.service.PcCustomService;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PcCustomServiceImpl implements PcCustomService {

    private final PcCustomRepository pcCustomRepository;
    private final CatalogPublicApi catalogPublicApi;
    private final HardwareCompatibilityValidator validator;

    @Override
    @Transactional
    public Integer saveNewBuild(SavePcBuildRequest request, Integer userId) {
        PcCustom custom = new PcCustom();
        custom.setUserId(userId);
        custom.setTotalPrice(request.getTotalPrice());

        if (request.getItems() != null) {
            for (SavePcBuildRequest.SavePartItem itemDto : request.getItems()) {
                PcCustomItem item = new PcCustomItem();
                item.setPcCustom(custom);
                item.setProductVariantId(itemDto.getVariantId());
                item.setComponentRole(itemDto.getRole());
                item.setQuantity(itemDto.getQuantity());
                item.setUnitPrice(itemDto.getUnitPrice());
                custom.getItems().add(item);
            }
        }

        PcCustom saved = pcCustomRepository.save(custom);
        return saved.getId();
    }

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
    public List<SavedPcBuildSummaryResponse> getMySavedBuilds(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<PcCustom> customs = pcCustomRepository.findByUserIdOrderByIdDesc(userId);
        if (customs == null || customs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> allVariantIds = customs.stream()
                .filter(c -> c.getItems() != null)
                .flatMap(c -> c.getItems().stream())
                .map(PcCustomItem::getProductVariantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, ComponentCandidateDTO> candidateMap = allVariantIds.isEmpty()
                ? Collections.emptyMap()
                : catalogPublicApi.getCandidateMapByIds(allVariantIds);

        return customs.stream().map(custom -> {
            String cpuName = null;
            String gpuName = null;
            String mainboardName = null;
            String ramSummary = null;

            if (custom.getItems() != null) {
                for (PcCustomItem item : custom.getItems()) {
                    String role = item.getComponentRole() != null ? item.getComponentRole().trim().toLowerCase() : "";
                    ComponentCandidateDTO candidate = candidateMap.get(item.getProductVariantId());
                    String pName = candidate != null ? candidate.getProductName() : ("Linh kiện #" + item.getProductVariantId());

                    switch (role) {
                        case "cpu" -> cpuName = pName;
                        case "gpu", "vga" -> gpuName = pName;
                        case "mainboard", "motherboard" -> mainboardName = pName;
                        case "ram" -> {
                            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                            ramSummary = pName + (qty > 1 ? " (x" + qty + ")" : "");
                        }
                        default -> {}
                    }
                }
            }

            String buildName = generateBuildName(custom.getId(), cpuName, gpuName);
            int itemCount = custom.getItems() != null ? custom.getItems().size() : 0;

            return SavedPcBuildSummaryResponse.builder()
                    .buildId(custom.getId())
                    .buildName(buildName)
                    .totalPrice(custom.getTotalPrice())
                    .orderId(custom.getOrderId())
                    .isOrdered(custom.getOrderId() != null)
                    .itemCount(itemCount)
                    .cpuName(cpuName)
                    .gpuName(gpuName)
                    .mainboardName(mainboardName)
                    .ramSummary(ramSummary)
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SavedPcBuildDetailResponse getSavedBuildDetail(Integer buildId, Integer userId) {
        PcCustom custom = pcCustomRepository.findById(buildId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình #" + buildId));

        if (!Objects.equals(custom.getUserId(), userId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập cấu hình này");
        }

        List<Integer> variantIds = custom.getItems() != null
                ? custom.getItems().stream().map(PcCustomItem::getProductVariantId).filter(Objects::nonNull).distinct().toList()
                : Collections.emptyList();

        Map<Integer, ComponentCandidateDTO> candidateMap = variantIds.isEmpty()
                ? Collections.emptyMap()
                : catalogPublicApi.getCandidateMapByIds(variantIds);

        String cpuName = null;
        String gpuName = null;
        List<SelectedPartResponse> items = new ArrayList<>();

        if (custom.getItems() != null) {
            for (PcCustomItem item : custom.getItems()) {
                String role = item.getComponentRole() != null ? item.getComponentRole().trim().toLowerCase() : "";
                ComponentCandidateDTO candidate = candidateMap.get(item.getProductVariantId());
                String productName = candidate != null ? candidate.getProductName() : ("Linh kiện #" + item.getProductVariantId());
                String variantName = candidate != null ? candidate.getVariantName() : null;
                String imageUrl = candidate != null ? candidate.getImageUrl() : null;
                Map<String, String> specs = candidate != null && candidate.getSpecs() != null ? candidate.getSpecs() : Collections.emptyMap();
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : (candidate != null && candidate.getPrice() != null ? candidate.getPrice() : BigDecimal.ZERO);
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));

                if ("cpu".equals(role)) {
                    cpuName = productName;
                } else if ("gpu".equals(role) || "vga".equals(role)) {
                    gpuName = productName;
                }

                items.add(SelectedPartResponse.builder()
                        .role(item.getComponentRole())
                        .variantId(item.getProductVariantId())
                        .productName(productName)
                        .variantName(variantName)
                        .imageUrl(imageUrl)
                        .unitPrice(unitPrice)
                        .quantity(qty)
                        .subtotal(subtotal)
                        .specs(specs)
                        .aiReason(null)
                        .build());
            }
        }

        String buildName = generateBuildName(custom.getId(), cpuName, gpuName);

        return SavedPcBuildDetailResponse.builder()
                .buildId(custom.getId())
                .buildName(buildName)
                .totalPrice(custom.getTotalPrice())
                .orderId(custom.getOrderId())
                .isOrdered(custom.getOrderId() != null)
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public void deleteSavedBuild(Integer buildId, Integer userId) {
        PcCustom custom = pcCustomRepository.findById(buildId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình #" + buildId));

        if (!Objects.equals(custom.getUserId(), userId)) {
            throw new AccessDeniedException("Bạn không có quyền xóa cấu hình này");
        }

        if (custom.getOrderId() != null) {
            throw new BusinessException("Không thể xóa cấu hình đã gắn liền với đơn hàng #" + custom.getOrderId());
        }

        pcCustomRepository.delete(custom);
    }

    private String generateBuildName(Integer buildId, String cpuName, String gpuName) {
        if (cpuName != null && !cpuName.isBlank()) {
            if (gpuName != null && !gpuName.isBlank()) {
                return "Bộ PC " + cpuName + " & " + gpuName;
            } else {
                return "Bộ PC " + cpuName + " (Đồ họa tích hợp)";
            }
        }
        return "Cấu hình PC Custom #" + buildId;
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
