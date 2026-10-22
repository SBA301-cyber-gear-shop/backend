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
import com.project.cybershop.shared.exception.BusinessException;
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
        1. BẮT BUỘC PHẢI CHỌN ĐỦ CÁC NHÓM LINH KIỆN ĐỂ TẠO THÀNH BỘ MÁY HOÀN CHỈNH HOẠT ĐỘNG ĐƯỢC:
           - 1 cpu (categorySlug: "cpu")
           - 1 mainboard (categorySlug: "mainboard")
           - 1 hoặc 2 thanh ram (categorySlug: "ram", đạt mục tiêu ramTargetGb)
           - 1 ổ cứng ssd (categorySlug: "ssd", lưu trữ và cài hệ điều hành)
           - 1 nguồn psu (categorySlug: "psu", công suất đủ gánh hệ thống)
           - 1 vỏ case (categorySlug: "case", chứa vừa các linh kiện)
           - 1 tản nhiệt cooler (categorySlug: "cooler", làm mát CPU)
           - 1 card đồ họa gpu (categorySlug: "gpu", BẮT BUỘC với mục đích 'gaming', 'creative' hoặc CPU dòng F không có card tích hợp).
           TUYỆT ĐỐI KHÔNG ĐƯỢC CHỈ CHỌN 1 LINH KIỆN HOẶC BỎ SÓT CÁC LINH KIỆN CỐT LÕI TRÊN!
        2. QUY TẮC PHÂN LOẠI DANH MỤC (NGHIÊM NGẶT):
           - MỖI ROLE BẮT BUỘC PHẢI CHỌN ĐÚNG LINH KIỆN CÓ 'categorySlug' TƯƠNG ỨNG:
             + role "ram": CHỈ ĐƯỢC CHỌN linh kiện có categorySlug == "ram". TUYỆT ĐỐI KHÔNG ĐƯỢC CHỌN ổ cứng SSD, GPU hay linh kiện khác cho role "ram"!
             + role "ssd": CHỈ ĐƯỢC CHỌN linh kiện có categorySlug == "ssd".
             + role "cpu": CHỈ ĐƯỢC CHỌN linh kiện có categorySlug == "cpu".
             + role "mainboard": CHỈ ĐƯỢC CHỌN linh kiện có categorySlug == "mainboard".
             + role "gpu": CHỈ ĐƯỢC CHỌN linh kiện có categorySlug == "gpu".
             + role "psu": CHỈ ĐƯỢC CHỌN linh kiện có categorySlug == "psu".
             + role "case": CHỈ ĐƯỢC CHỌN linh kiện có categorySlug == "case".
             + role "cooler": CHỈ ĐƯỢC CHỌN linh kiện có categorySlug == "cooler".
           - NẾU KHÁCH HÀNG YÊU CẦU MỘT THƯƠNG HIỆU MÀ TRONG KHO LINH KIỆN (availablePartsInStock) CỦA DANH MỤC ĐÓ KHÔNG CÓ:
             Hãy chọn linh kiện tốt nhất hiện có trong danh mục đó và giải thích rõ trong 'aiReasoning', tuyệt đối không được lấy sản phẩm khác danh mục để thay thế!
        3. CHỈ ĐƯỢC CHỌN 'variantId' có trong danh sách được cấp. TUYỆT ĐỐI KHÔNG BỊA ID HOẶC TÊN LINH KIỆN.
        4. TỔNG GIÁ TIỀN của toàn bộ linh kiện phải <= Ngân sách của khách hàng.
        5. TUÂN THỦ TƯƠNG THÍCH KỸ THUẬT:
           - CPU và Mainboard PHẢI TRÙNG KHỚP Socket.
           - RAM phải trùng chuẩn DDR4/DDR5 với Mainboard.
           - Nguồn (PSU) >= (TDP CPU + TDP GPU + 80W) * 1.30.
           - Vỏ Case phải vừa bo mạch chủ và vừa độ dài card đồ họa.
        6. QUY ĐỊNH ĐỊNH DẠNG ĐẦU RA (BẮT BUỘC):
           - CHỈ TRẢ VỀ DUY NHẤT 1 ĐỐI TƯỢNG JSON HỢP LỆ THEO ĐÚNG CẤU TRÚC SAU.
           - TUYỆT ĐỐI KHÔNG VIẾT THÊM BẤT KỲ VĂN BẢN, LỜI MỞ ĐẦU, KẾT LUẬN HAY KÝ TỰ NÀO BÊN NGOÀI JSON:
           {
             "buildName": "Tên cấu hình tối ưu",
             "aiReasoning": "Lời nhận xét phân tích vì sao chọn các linh kiện này",
             "performanceHighlight": "Tóm tắt hiệu năng chơi game hoặc làm việc",
             "selectedParts": [
               { "role": "cpu", "variantId": 101, "quantity": 1, "reason": "Lý do lựa chọn CPU" },
               { "role": "mainboard", "variantId": 118, "quantity": 1, "reason": "Lý do lựa chọn Mainboard" },
               { "role": "ram", "variantId": 311, "quantity": 2, "reason": "Lý do lựa chọn RAM" },
               { "role": "gpu", "variantId": 122, "quantity": 1, "reason": "Lý do lựa chọn GPU" },
               { "role": "ssd", "variantId": 523, "quantity": 1, "reason": "Lý do lựa chọn SSD" },
               { "role": "psu", "variantId": 424, "quantity": 1, "reason": "Lý do lựa chọn PSU" },
               { "role": "case", "variantId": 125, "quantity": 1, "reason": "Lý do lựa chọn Case" },
               { "role": "cooler", "variantId": 126, "quantity": 1, "reason": "Lý do lựa chọn Cooler" }
             ]
           }
        """;

    @Override
    @Transactional
    public AiBuildResponse processAiBuild(AiBuildRequest request, Integer currentUserId) {
        // 1. Lấy toàn bộ linh kiện đang có trong kho & danh sách thương hiệu từ DB
        List<ComponentCandidateDTO> allCandidates = catalogPublicApi.getActiveCandidatesForBuild();
        if (allCandidates.isEmpty()) {
            throw new IllegalStateException("Kho hàng hiện chưa có linh kiện nào đang hoạt động!");
        }
        List<String> allBrandNames = catalogPublicApi.getAllBrandNames();

        // 2. Thu hẹp ứng viên theo Heuristic để tiết kiệm token
        List<ComponentCandidateDTO> candidates = heuristicService.filterCandidates(
                allCandidates, request.getBudget(), request.getPurpose(), request.getUserPrompt(), request.getRamGb(), allBrandNames
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

        if (proposal == null || proposal.getSelectedParts() == null || proposal.getSelectedParts().isEmpty()) {
            throw new BusinessException("AI chưa tìm thấy bộ linh kiện phù hợp với mức ngân sách và yêu cầu này trong kho. Vui lòng điều chỉnh ngân sách hoặc mô tả yêu cầu!");
        }

        // 5. Map linh kiện đã chọn sang map kiểm định
        Map<String, ComponentCandidateDTO> selectedMap = new HashMap<>();
        List<SelectedPartResponse> selectedParts = new ArrayList<>();
        BigDecimal totalCalculated = BigDecimal.ZERO;

        for (LlmClientService.AiDraftProposal.PartSelection ps : proposal.getSelectedParts()) {
            if (ps == null || ps.getVariantId() == null) continue;
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

        if (selectedParts.isEmpty()) {
            throw new BusinessException("AI chưa tìm thấy linh kiện tương thích trong kho đáp ứng đủ cấu hình này. Vui lòng thử lại với ngân sách hoặc nhu cầu khác!");
        }

        // 6. Kiểm định cứng qua Rule Engine
        HardwareCompatibilityValidator.ValidationReport report = validator.validate(selectedMap, request.getBudget());

        // Đề xuất AI chỉ trả về kết quả xem trước, KHÔNG tự ý lưu vào Database pc_customs
        return AiBuildResponse.builder()
                .buildId(null)
                .buildName(proposal.getBuildName())
                .purpose(request.getPurpose())
                .totalPrice(totalCalculated)
                .budget(request.getBudget())
                .aiReasoning(proposal.getAiReasoning())
                .performanceHighlight(proposal.getPerformanceHighlight())
                .isCompatible(report.isValid())
                .checks(report.checks())
                .items(selectedParts)
                .build();
    }
}
