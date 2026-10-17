package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.AiBuildResponse;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;
import com.project.cybershop.pcbuild.api.dto.response.SelectedPartResponse;
import com.project.cybershop.pcbuild.internal.entity.*;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.AiPcBuilderService;
import com.project.cybershop.pcbuild.internal.service.BudgetHeuristicService;
import com.project.cybershop.pcbuild.internal.service.HardwareCompatibilityValidator;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPcBuilderServiceImpl implements AiPcBuilderService {

    private final CatalogPublicApi catalogPublicApi;
    private final BudgetHeuristicService heuristicService;
    private final LlmClientService llmClientService;
    private final HardwareCompatibilityValidator validator;
    private final PcCustomRepository pcCustomRepository;
    private final IdentityApi identityApi;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        BẠN LÀ MỘT KỸ SƯ TRƯỞNG THIẾT KẾ PHẦN CỨNG MÁY TÍNH CHO CYBERSHOP.
        NHIỆM VỤ: Chọn ra 1 cấu hình PC tối ưu nhất từ DANH MỤC LINH KIỆN CÓ SẴN TRONG KHO theo yêu cầu khách hàng.
        QUY TẮC BẮT BUỘC:
        1. CHỈ ĐƯỢC CHỌN 'variantId' có trong danh sách được cấp. TUYỆT ĐỐI KHÔNG BỊA ID HOẶC TÊN LINH KIỆN.
        2. TỔNG GIÁ TIỀN phải <= Ngân sách của khách hàng.
        3. TUÂN THỦ TƯƠNG THÍCH:
           - CPU và Mainboard PHẢI TRÙNG KHỚP Socket.
           - RAM phải trùng chuẩn DDR4/DDR5 với Mainboard.
           - Nguồn (PSU) >= (TDP CPU + TDP GPU + 80W) * 1.30.
           - Vỏ Case phải vừa bo mạch chủ và vừa độ dài card đồ họa.
        4. Cấu trúc JSON trả về bắt buộc gồm:
           - buildName: Tên đặt cho cấu hình (String)
           - aiReasoning: Lời nhận xét phân tích vì sao chọn các linh kiện này (String)
           - performanceHighlight: Tóm tắt hiệu năng chơi game / làm việc (String)
           - selectedParts: Mảng chứa các linh kiện [role, variantId, quantity, reason]
        """;

    @Override
    @Transactional
    public AiBuildResponse processAiBuild(AiBuildRequest request, Integer currentUserId) {
        // 1. Lấy toàn bộ linh kiện đang có trong kho
        List<ComponentCandidateDTO> allCandidates = catalogPublicApi.getActiveCandidatesForBuild();
        if (allCandidates.isEmpty()) {
            throw new IllegalStateException("Kho hàng hiện chưa có linh kiện nào đang hoạt động!");
        }

        // 2. Thu hẹp ứng viên theo Heuristic để tiết kiệm token
        List<ComponentCandidateDTO> candidates = heuristicService.filterCandidates(
                allCandidates, request.getBudget(), request.getPurpose()
        );

        Map<Integer, ComponentCandidateDTO> candidateMap = new HashMap<>();
        for (ComponentCandidateDTO c : allCandidates) {
            candidateMap.put(c.getVariantId(), c);
        }

        // 3. Chuẩn bị User Context gửi cho AI
        String userContext;
        try {
            Map<String, Object> contextObj = Map.of(
                "budget", request.getBudget(),
                "purpose", request.getPurpose(),
                "ramTargetGb", request.getRamGb(),
                "scope", request.getScope(),
                "userNotes", request.getUserPrompt() != null ? request.getUserPrompt() : "",
                "availablePartsInStock", candidates
            );
            userContext = objectMapper.writeValueAsString(contextObj);
        } catch (Exception e) {
            userContext = "{}";
        }

        // 4. Gọi AI sinh cấu hình nháp
        LlmClientService.AiDraftProposal proposal = llmClientService.generateBuildProposal(SYSTEM_PROMPT, userContext);

        // 5. Map linh kiện đã chọn sang map kiểm định
        Map<String, ComponentCandidateDTO> selectedMap = new HashMap<>();
        List<SelectedPartResponse> selectedParts = new ArrayList<>();
        BigDecimal totalCalculated = BigDecimal.ZERO;

        for (LlmClientService.AiDraftProposal.PartSelection ps : proposal.getSelectedParts()) {
            ComponentCandidateDTO cand = candidateMap.get(ps.getVariantId());
            if (cand != null) {
                cand.setQuantity(ps.getQuantity() != null ? ps.getQuantity() : 1);
                selectedMap.put(ps.getRole(), cand);

                BigDecimal subtotal = cand.getPrice().multiply(BigDecimal.valueOf(cand.getQuantity()));
                totalCalculated = totalCalculated.add(subtotal);

                selectedParts.add(SelectedPartResponse.builder()
                        .role(ps.getRole())
                        .variantId(cand.getVariantId())
                        .productName(cand.getProductName())
                        .variantName(cand.getVariantName())
                        .imageUrl(cand.getImageUrl())
                        .unitPrice(cand.getPrice())
                        .quantity(cand.getQuantity())
                        .subtotal(subtotal)
                        .specs(cand.getSpecs())
                        .aiReason(ps.getReason())
                        .build());
            }
        }

        // 6. Kiểm định cứng qua Rule Engine
        HardwareCompatibilityValidator.ValidationReport report = validator.validate(selectedMap, request.getBudget());

        // 7. Xác định userId hợp lệ (do bảng pc_customs có constraint user_id NOT NULL)
        Integer effectiveUserId = currentUserId;
        if (effectiveUserId == null) {
            effectiveUserId = identityApi.getUserByEmail("customer@techzone.demo")
                    .map(u -> u.getId())
                    .orElse(1);
        }

        // 8. Lưu cấu hình vào bảng pc_customs
        PcCustom custom = new PcCustom();
        custom.setUserId(effectiveUserId);
        custom.setTotalPrice(totalCalculated);

        for (SelectedPartResponse p : selectedParts) {
            PcCustomItem item = new PcCustomItem();
            item.setPcCustom(custom);
            item.setProductVariantId(p.getVariantId());
            item.setComponentRole(p.getRole());
            item.setQuantity(p.getQuantity());
            item.setUnitPrice(p.getUnitPrice());
            custom.getItems().add(item);
        }

        PcCustom saved = pcCustomRepository.save(custom);

        return AiBuildResponse.builder()
                .buildId(saved.getId())
                .buildName(proposal.getBuildName())
                .purpose(request.getPurpose())
                .totalPrice(saved.getTotalPrice())
                .budget(request.getBudget())
                .aiReasoning(proposal.getAiReasoning())
                .performanceHighlight(proposal.getPerformanceHighlight())
                .isCompatible(report.isValid())
                .checks(report.checks())
                .items(selectedParts)
                .build();
    }
}
