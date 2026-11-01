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
        return explainer.explain(profile, result, BigDecimal.valueOf(28_000_000), "Chơi game 2K", "", snapshot(), Instant.now().plusSeconds(10));
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
