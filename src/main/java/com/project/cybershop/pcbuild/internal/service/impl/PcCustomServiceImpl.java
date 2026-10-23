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
    public List<CompatibleReplacementResponse> getCompatibleReplacements(
            String role,
            Integer currentCpuId,
            Integer currentMbId,
            Integer currentRamId,
            Integer currentGpuId,
            Integer currentCaseId,
            Integer currentCoolerId,
            Integer currentPsuId
    ) {
        List<ComponentCandidateDTO> candidates = catalogPublicApi.getActiveCandidatesForBuild();
        List<Integer> idsToFetch = new java.util.ArrayList<>();
        if (currentCpuId != null) idsToFetch.add(currentCpuId);
        if (currentMbId != null) idsToFetch.add(currentMbId);
        if (currentRamId != null) idsToFetch.add(currentRamId);
        if (currentGpuId != null) idsToFetch.add(currentGpuId);
        if (currentCaseId != null) idsToFetch.add(currentCaseId);
        if (currentCoolerId != null) idsToFetch.add(currentCoolerId);
        if (currentPsuId != null) idsToFetch.add(currentPsuId);

        Map<Integer, ComponentCandidateDTO> map = idsToFetch.isEmpty()
                ? Collections.emptyMap()
                : catalogPublicApi.getCandidateMapByIds(idsToFetch);

        ComponentCandidateDTO cpu = currentCpuId != null ? map.get(currentCpuId) : null;
        ComponentCandidateDTO mb = currentMbId != null ? map.get(currentMbId) : null;
        ComponentCandidateDTO ram = currentRamId != null ? map.get(currentRamId) : null;
        ComponentCandidateDTO gpu = currentGpuId != null ? map.get(currentGpuId) : null;
        ComponentCandidateDTO pcCase = currentCaseId != null ? map.get(currentCaseId) : null;
        ComponentCandidateDTO cooler = currentCoolerId != null ? map.get(currentCoolerId) : null;
        ComponentCandidateDTO psu = currentPsuId != null ? map.get(currentPsuId) : null;

        return candidates.stream()
                .filter(c -> c.getCategorySlug().equalsIgnoreCase(role))
                .map(c -> {
                    boolean ok = true;
                    String note = "Tương thích tốt";

                    // 1. Kiểm tra Socket CPU vs Mainboard
                    if ("mainboard".equalsIgnoreCase(role) && cpu != null) {
                        String cpuSocket = cpu.getSpec("Socket");
                        String mbSocket = c.getSpec("Socket");
                        if (cpuSocket != null && mbSocket != null && !cpuSocket.equalsIgnoreCase(mbSocket)) {
                            ok = false;
                            note = "Lệch socket với CPU (Cần " + cpuSocket + ")";
                        }
                    } else if ("cpu".equalsIgnoreCase(role) && mb != null) {
                        String mbSocket = mb.getSpec("Socket");
                        String cpuSocket = c.getSpec("Socket");
                        if (mbSocket != null && cpuSocket != null && !mbSocket.equalsIgnoreCase(cpuSocket)) {
                            ok = false;
                            note = "Lệch socket với Bo mạch (Cần " + mbSocket + ")";
                        }
                    }

                    // 2. Kiểm tra chuẩn RAM (Memory_Type: DDR4 / DDR5)
                    if (ok && "ram".equalsIgnoreCase(role) && mb != null) {
                        String mbRamType = mb.getSpec("Memory_Type");
                        String ramType = c.getSpec("Memory_Type");
                        if (mbRamType != null && ramType != null && !mbRamType.equalsIgnoreCase(ramType)) {
                            ok = false;
                            note = "Lệch chuẩn RAM: Bo mạch dùng " + mbRamType + " (RAM này là " + ramType + ")";
                        }
                    } else if (ok && "mainboard".equalsIgnoreCase(role) && ram != null) {
                        String mbRamType = c.getSpec("Memory_Type");
                        String ramType = ram.getSpec("Memory_Type");
                        if (mbRamType != null && ramType != null && !mbRamType.equalsIgnoreCase(ramType)) {
                            ok = false;
                            note = "Lệch chuẩn RAM: Bo mạch này dùng " + mbRamType + " (RAM đang chọn là " + ramType + ")";
                        }
                    }

                    // 3. Kiểm tra kích thước VGA trong Case
                    if (ok && "gpu".equalsIgnoreCase(role) && pcCase != null) {
                        try {
                            int gpuLen = Integer.parseInt(c.getSpecOrDefault("Length_MM", "0"));
                            int maxGpu = Integer.parseInt(pcCase.getSpecOrDefault("Max_GPU_Length_MM", "9999"));
                            if (gpuLen > maxGpu && maxGpu > 0) {
                                ok = false;
                                note = "Card dài " + gpuLen + "mm vượt quá vỏ case (" + maxGpu + "mm)";
                            }
                        } catch (NumberFormatException ignored) {}
                    } else if (ok && "case".equalsIgnoreCase(role) && gpu != null) {
                        try {
                            int gpuLen = Integer.parseInt(gpu.getSpecOrDefault("Length_MM", "0"));
                            int maxGpu = Integer.parseInt(c.getSpecOrDefault("Max_GPU_Length_MM", "9999"));
                            if (gpuLen > maxGpu && maxGpu > 0) {
                                ok = false;
                                note = "Vỏ case hỗ trợ VGA tối đa " + maxGpu + "mm (Card hiện tại dài " + gpuLen + "mm)";
                            }
                        } catch (NumberFormatException ignored) {}
                    }

                    // 4. Kiểm tra chiều cao Tản nhiệt CPU trong Case (cho tản khí)
                    if (ok && "cooler".equalsIgnoreCase(role) && pcCase != null) {
                        String coolerType = c.getSpecOrDefault("Cooler_Type", "Air");
                        if (!"Liquid".equalsIgnoreCase(coolerType)) {
                            try {
                                int coolerHeight = Integer.parseInt(c.getSpecOrDefault("Height_MM", "0"));
                                int maxCooler = Integer.parseInt(pcCase.getSpecOrDefault("Max_Cooler_Height_MM", "9999"));
                                if (coolerHeight > maxCooler && maxCooler > 0) {
                                    ok = false;
                                    note = "Tản nhiệt cao " + coolerHeight + "mm cấn nắp vỏ case (Tối đa " + maxCooler + "mm)";
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    } else if (ok && "case".equalsIgnoreCase(role) && cooler != null) {
                        String coolerType = cooler.getSpecOrDefault("Cooler_Type", "Air");
                        if (!"Liquid".equalsIgnoreCase(coolerType)) {
                            try {
                                int coolerHeight = Integer.parseInt(cooler.getSpecOrDefault("Height_MM", "0"));
                                int maxCooler = Integer.parseInt(c.getSpecOrDefault("Max_Cooler_Height_MM", "9999"));
                                if (coolerHeight > maxCooler && maxCooler > 0) {
                                    ok = false;
                                    note = "Vỏ case hỗ trợ tản tối đa " + maxCooler + "mm (Tản hiện tại cao " + coolerHeight + "mm)";
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    // 5. Kiểm tra Tản nhiệt có hỗ trợ Socket của CPU không
                    if (ok && "cooler".equalsIgnoreCase(role) && cpu != null) {
                        String supportedSockets = c.getSpecOrDefault("Supported_Sockets", "");
                        String cpuSocket = cpu.getSpec("Socket");
                        if (cpuSocket != null && !supportedSockets.isBlank() && !supportedSockets.toUpperCase().contains(cpuSocket.toUpperCase())) {
                            ok = false;
                            note = "Tản nhiệt không hỗ trợ socket " + cpuSocket;
                        }
                    }

                    // 6. Kiểm tra Kích thước Mainboard (Form_Factor) có vừa Case không
                    if (ok && "case".equalsIgnoreCase(role) && mb != null) {
                        String supportedMb = c.getSpecOrDefault("Supported_Motherboards", "");
                        String mbFormFactor = mb.getSpec("Form_Factor");
                        if (mbFormFactor != null && !supportedMb.isBlank() && !supportedMb.toUpperCase().contains(mbFormFactor.toUpperCase())) {
                            ok = false;
                            note = "Vỏ case không hỗ trợ bo mạch chuẩn " + mbFormFactor;
                        }
                    } else if (ok && "mainboard".equalsIgnoreCase(role) && pcCase != null) {
                        String supportedMb = pcCase.getSpecOrDefault("Supported_Motherboards", "");
                        String mbFormFactor = c.getSpec("Form_Factor");
                        if (mbFormFactor != null && !supportedMb.isBlank() && !supportedMb.toUpperCase().contains(mbFormFactor.toUpperCase())) {
                            ok = false;
                            note = "Bo mạch chuẩn " + mbFormFactor + " không vừa với vỏ case hiện tại";
                        }
                    }

                    // 7. Kiểm tra Công suất nguồn (PSU)
                    if (ok && "psu".equalsIgnoreCase(role)) {
                        int cpuP = cpu != null ? Integer.parseInt(cpu.getSpecOrDefault("TDP_Power", "65")) : 65;
                        int gpuP = gpu != null ? Integer.parseInt(gpu.getSpecOrDefault("TDP_Power", "115")) : 0;
                        int estLoad = cpuP + gpuP + 80;
                        int recWatt = (int) Math.round(estLoad * 1.30);
                        try {
                            int psuWatt = Integer.parseInt(c.getSpecOrDefault("Wattage", "0"));
                            if (psuWatt > 0 && psuWatt < recWatt) {
                                ok = false;
                                note = "Nguồn " + psuWatt + "W không đủ tải an toàn (Cần tối thiểu " + recWatt + "W)";
                            }
                        } catch (NumberFormatException ignored) {}
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
