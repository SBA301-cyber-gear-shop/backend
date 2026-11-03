package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.EngineReport;
import com.project.cybershop.pcbuild.internal.model.SolverResult;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.BuildExplainer;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.project.cybershop.pcbuild.internal.PcBuildTestData.lines;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.snapshot;
import static com.project.cybershop.pcbuild.internal.PcBuildTestData.validDesktop;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("BuildExplainer - AI chỉ viết lời giải thích cho bộ máy solver đã chọn (DD 8.4)")
class BuildExplainerTest {

    private final LlmClientService llm = mock(LlmClientService.class);
    private final BuildExplainerImpl explainer = new BuildExplainerImpl(llm, new ObjectMapper());

    private final List<BuildLine> build = lines(validDesktop());
    private final SolverResult result = new SolverResult(build, BigDecimal.valueOf(27_000_000), BigDecimal.valueOf(1_000_000), 0.8,
            EngineReport.of(List.of(), 650, null, BigDecimal.valueOf(27_000_000)), null, List.of(), null, 5, List.of(), null);
    private final ValidatedProfile profile = new ValidatedProfile(RequirementProfileDto.builder().summary("Chơi game").build(),
            Archetype.GAMING, List.of());

    private BuildExplainer.Explanation explain() {
        return explainer.explain(profile, result, BigDecimal.valueOf(28_000_000), "Chơi game 2K", "", snapshot(), Instant.now().plusSeconds(10), null);
    }

    private static LlmClientService.ExplanationDraft draft(Map<Integer, String> reasons) {
        LlmClientService.ExplanationDraft draft = new LlmClientService.ExplanationDraft();
        draft.setBuildName("Máy chơi game 2K rất dài vượt quá sáu mươi ký tự để kiểm tra việc cắt tên bộ máy");
        draft.setAiReasoning("Bộ máy cân bằng.");
        draft.setPerformanceHighlight("Chơi tốt 2K.");
        reasons.forEach((variantId, reason) -> {
            LlmClientService.ExplanationDraft.PartReason partReason = new LlmClientService.ExplanationDraft.PartReason();
            partReason.setVariantId(variantId);
            partReason.setReason(reason);
            draft.getPartReasons().add(partReason);
        });
        return draft;
    }

    @Test
    @DisplayName("variantId lạ trong partReasons bị bỏ → aiStatus PARTIAL; tên bộ máy cắt còn ≤ 60 ký tự")
    void unknownVariantIdsDropped() {
        when(llm.explainBuild(anyString(), anyString(), any())).thenReturn(draft(Map.of(1, "CPU mạnh", 99999, "Món AI bịa")));

        BuildExplainer.Explanation explanation = explain();

        assertThat(explanation.aiStatus()).isEqualTo("PARTIAL");
        assertThat(explanation.partReasons()).containsOnlyKeys(1);
        assertThat(explanation.buildName()).hasSizeLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("Đủ lý do cho các món của bộ máy → OK; context gửi AI có tổng, tiền dư và keySpecs, không có variantId ngoài bộ máy")
    void validExplanation() {
        when(llm.explainBuild(anyString(), anyString(), any())).thenAnswer(invocation -> {
            String context = invocation.getArgument(1);
            assertThat(context).contains("\"total\":27000000").contains("\"leftover\":1000000").contains("\"fitPercent\":96.4");
            return draft(Map.of(1, "CPU", 4, "GPU"));
        });

        assertThat(explain().aiStatus()).isEqualTo("OK");
    }

    @Test
    @DisplayName("B1: không có MAX_PRICE thì prompt không chứa quy tắc 'hết linh kiện đắt hơn'; có MAX_PRICE thì có")
    void maxPriceRuleOnlyWhenObjectiveIsSet() {
        java.util.List<String> prompts = new java.util.ArrayList<>();
        when(llm.explainBuild(anyString(), anyString(), any())).thenAnswer(invocation -> {
            prompts.add(invocation.getArgument(0));
            return draft(Map.of(1, "CPU"));
        });

        explain();
        ValidatedProfile maxPrice = new ValidatedProfile(RequirementProfileDto.builder().summary("Đắt nhất").objective("MAX_PRICE").build(),
                Archetype.GAMING, List.of());
        explainer.explain(maxPrice, result, BigDecimal.valueOf(28_000_000), "Máy đắt nhất", "", snapshot(), Instant.now().plusSeconds(10), null);

        assertThat(prompts.get(0)).doesNotContain("hết linh kiện đắt hơn").contains("CÓ DẤU");
        assertThat(prompts.get(1)).contains("hết linh kiện đắt hơn");
    }

    @Test
    @DisplayName("B1: context kèm nghĩa chính xác của reasonCode; NEEDS_SATURATED cấm nói cửa hàng hết linh kiện")
    void contextExplainsReasonCode() {
        SolverResult saturated = new SolverResult(build, BigDecimal.valueOf(20_000_000), BigDecimal.valueOf(8_000_000), 0.8,
                EngineReport.of(List.of(), 650, null, BigDecimal.valueOf(20_000_000)), "NEEDS_SATURATED", List.of(), null, 5, List.of(), null);
        when(llm.explainBuild(anyString(), anyString(), any())).thenAnswer(invocation -> {
            assertThat((String) invocation.getArgument(1)).contains("reasonMeaning").contains("KHÔNG nói cửa hàng đã hết linh kiện");
            return draft(Map.of(1, "CPU"));
        });

        explainer.explain(profile, saturated, BigDecimal.valueOf(28_000_000), "Chơi game 2K", "", snapshot(), Instant.now().plusSeconds(10), null);
    }

    @Test
    @DisplayName("B4: Trọn bộ -> context có ngân sách gốc của khách và phần dành cho thùng máy, đã định dạng tiền")
    void fullSetBudgetIsExplained() {
        when(llm.explainBuild(anyString(), anyString(), any())).thenAnswer(invocation -> {
            String context = invocation.getArgument(1);
            assertThat(context).contains("\"fullSetBudget\":35000000").contains("35.000.000 ₫").contains("28.000.000 ₫");
            return draft(Map.of(1, "CPU"));
        });

        explainer.explain(profile, result, BigDecimal.valueOf(28_000_000), "Chơi game 2K", "", snapshot(), Instant.now().plusSeconds(10),
                BigDecimal.valueOf(35_000_000));
    }

    @Test
    @DisplayName("B4: chỉ thùng máy -> context không có fullSetBudget")
    void noFullSetBudgetForPcOnly() {
        when(llm.explainBuild(anyString(), anyString(), any())).thenAnswer(invocation -> {
            assertThat((String) invocation.getArgument(1)).doesNotContain("fullSetBudget");
            return draft(Map.of(1, "CPU"));
        });

        explain();
    }

    @Test
    @DisplayName("B8: số tiền AI viết lộn xộn được chuẩn hóa về dạng 1.234.567 ₫ trong mọi đoạn văn")
    void moneyIsNormalized() {
        LlmClientService.ExplanationDraft messy = draft(Map.of(1, "Giá 3690000 VNĐ hợp lý"));
        messy.setAiReasoning("Tổng 11,968,000đ trong ngân sách 12,000,000 VND, dư 32000 dong; RAM 32 GB, năm 2026.");
        messy.setPerformanceHighlight("Còn 1.500.000 vnđ và 250,000 đồng.");
        when(llm.explainBuild(anyString(), anyString(), any())).thenReturn(messy);

        BuildExplainer.Explanation explanation = explain();

        assertThat(explanation.aiReasoning())
                .isEqualTo("Tổng 11.968.000 ₫ trong ngân sách 12.000.000 ₫, dư 32.000 ₫; RAM 32 GB, năm 2026.");
        assertThat(explanation.performanceHighlight()).isEqualTo("Còn 1.500.000 ₫ và 250.000 ₫.");
        assertThat(explanation.partReasons().get(1)).isEqualTo("Giá 3.690.000 ₫ hợp lý");
    }

    private static LlmClientService.ExplanationDraft text(String reasoning) {
        LlmClientService.ExplanationDraft draft = draft(Map.of(1, "CPU"));
        draft.setAiReasoning(reasoning);
        return draft;
    }

    @Test
    @DisplayName("B8: AI viết không dấu -> gọi lại đúng một lần và dùng bản có dấu")
    void rewritesTextWithoutDiacritics() {
        when(llm.explainBuild(anyString(), anyString(), any()))
                .thenReturn(text("Chao ban, bo may nay duoc xay dung phu hop voi nhu cau van phong va hoc online cua ban."))
                .thenReturn(text("Chào bạn, bộ máy này được xây dựng phù hợp với nhu cầu văn phòng và học online của bạn."));

        assertThat(explain().aiReasoning()).startsWith("Chào bạn");
        org.mockito.Mockito.verify(llm, org.mockito.Mockito.times(2)).explainBuild(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("B8: lần gọi lại hết thời gian -> giữ bản đầu, không trả lỗi cho khách")
    void keepsFirstDraftWhenRewriteFails() {
        when(llm.explainBuild(anyString(), anyString(), any()))
                .thenReturn(text("Chao ban, bo may nay duoc xay dung phu hop voi nhu cau van phong va hoc online cua ban."))
                .thenThrow(new AiUnavailableException(AiUnavailableException.Detail.DEADLINE_EXCEEDED));

        assertThat(explain().aiReasoning()).startsWith("Chao ban");
    }

    @Test
    @DisplayName("B8: văn bản có dấu bình thường -> chỉ gọi AI một lần")
    void noRewriteForNormalText() {
        when(llm.explainBuild(anyString(), anyString(), any())).thenReturn(text("Bộ máy cân bằng, chơi tốt game 2K."));

        explain();

        org.mockito.Mockito.verify(llm, org.mockito.Mockito.times(1)).explainBuild(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("AI lỗi (timeout 2 lần) → AiUnavailableException, không có lời giải thích dự phòng")
    void failurePropagates() {
        when(llm.explainBuild(anyString(), anyString(), any())).thenThrow(new AiUnavailableException(AiUnavailableException.Detail.TIMEOUT));

        assertThatThrownBy(this::explain).isInstanceOf(AiUnavailableException.class);
    }

    @Test
    @DisplayName("AI trả JSON thiếu buildName/aiReasoning → INVALID_RESPONSE")
    void blankExplanationRejected() {
        LlmClientService.ExplanationDraft empty = new LlmClientService.ExplanationDraft();
        when(llm.explainBuild(anyString(), anyString(), any())).thenReturn(empty);

        assertThatThrownBy(this::explain).isInstanceOfSatisfying(AiUnavailableException.class,
                error -> assertThat(error.detail()).isEqualTo(AiUnavailableException.Detail.INVALID_RESPONSE));
    }
}
