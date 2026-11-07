package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.internal.model.BuildExplanationDraft;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.service.BuildExplainer.Explanation;
import com.project.cybershop.pcbuild.internal.service.BuildExplainer.Source;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import com.project.cybershop.pcbuild.internal.support.LlmClient;
import com.project.cybershop.pcbuild.internal.support.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;

import static com.project.cybershop.pcbuild.internal.service.impl.BuildFixtures.docxBuild;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Bước 8: AI chỉ diễn đạt; số trong câu phải khớp số code tính, sai hay AI lỗi thì dùng câu mẫu. */
class BuildExplainerImplTest {

    private static final long BUDGET = 20_000_000L;
    private static final long TOTAL = 19_900_000L;
    private static final String PURPOSE = "PC chơi game";
    /** Câu đúng khuôn docx, chỉ dùng tên món và số tiền có trong dữ liệu gửi AI. */
    private static final String GOOD_MESSAGE = "Dạ, Shop tư vấn cho bạn bộ PC chơi game trọn bộ với CPU-A, GPU-A, "
            + "2 thanh RAM DDR5 8GB, SSD 1TB, tản Tản-A, kèm Màn-A, Phím-A và Chuột-A. Tổng giá là 19.900.000đ, "
            + "nằm trong ngân sách 20.000.000đ của bạn và còn dư 100.000đ ạ.";

    private LlmClient llmClient;
    private BuildExplainerImpl explainer;
    private final List<BuildLine> lines = docxBuild();

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        explainer = new BuildExplainerImpl(llmClient, new RateLimiter(Clock.systemUTC(), 100), new ObjectMapper());
    }

    private void aiReplies(String message) {
        when(llmClient.generateJson(eq("explainBuild"), anyString(), anyString(), anyMap(), eq(BuildExplanationDraft.class)))
                .thenReturn(new BuildExplanationDraft(message));
    }

    @Test
    @DisplayName("Câu AI có đủ tổng/ngân sách/tiền dư và không có số lạ → dùng câu AI")
    void validAiMessage_isUsed() {
        aiReplies(GOOD_MESSAGE);

        Explanation explanation = explainer.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test");

        assertThat(explanation.source()).isEqualTo(Source.AI);
        assertThat(explanation.message()).isEqualTo(GOOD_MESSAGE);
    }

    @Test
    @DisplayName("AI chỉ nhận tổng, ngân sách, tiền dư dạng chữ và tên món; không nhận đơn giá từng món")
    void aiInput_onlyHasCodeComputedNumbers() {
        aiReplies(GOOD_MESSAGE);

        explainer.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test");

        ArgumentCaptor<String> input = ArgumentCaptor.forClass(String.class);
        verify(llmClient).generateJson(eq("explainBuild"), anyString(), input.capture(), anyMap(),
                eq(BuildExplanationDraft.class));
        assertThat(input.getValue())
                .contains("\"totalText\":\"19.900.000đ\"", "\"budgetText\":\"20.000.000đ\"", "\"leftoverText\":\"100.000đ\"",
                        "\"purpose\":\"PC chơi game\"", "\"name\":\"GPU-A\"")
                .doesNotContain("5.600.000", "5600000");
    }

    @Test
    @DisplayName("AI thêm số không có trong dữ liệu (144 FPS) → câu mẫu")
    void inventedNumber_fallsBackToTemplate() {
        aiReplies(GOOD_MESSAGE.replace("Tổng giá", "Chơi mượt 144 FPS. Tổng giá"));

        Explanation explanation = explainer.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test");

        assertThat(explanation.source()).isEqualTo(Source.TEMPLATE);
        assertThat(explanation.reason()).contains("144");
    }

    @Test
    @DisplayName("AI viết sai tổng tiền (19.500.000đ) → câu mẫu")
    void wrongTotal_fallsBackToTemplate() {
        aiReplies(GOOD_MESSAGE.replace("19.900.000đ", "19.500.000đ"));

        Explanation explanation = explainer.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test");

        assertThat(explanation.source()).isEqualTo(Source.TEMPLATE);
        assertThat(explanation.reason()).contains("19.900.000đ");
    }

    @Test
    @DisplayName("AI không phản hồi → câu mẫu đúng khuôn: giới thiệu bộ PC, tổng, ngân sách, còn dư")
    void aiUnavailable_usesTemplate() {
        when(llmClient.generateJson(anyString(), anyString(), anyString(), anyMap(), eq(BuildExplanationDraft.class)))
                .thenThrow(new AiUnavailableException("hết giờ hoặc lỗi kết nối"));

        Explanation explanation = explainer.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test");

        assertThat(explanation.source()).isEqualTo(Source.TEMPLATE);
        assertThat(explanation.message())
                .startsWith("Dạ, Shop tư vấn cho bạn bộ PC chơi game trọn bộ gồm MONITOR Màn-A")
                .contains("2 × RAM RAM DDR5 8GB")
                .endsWith("Tổng giá là 19.900.000đ, nằm trong ngân sách 20.000.000đ của bạn và còn dư 100.000đ ạ.");
    }

    @Test
    @DisplayName("Hết lượt gọi AI (429) → câu mẫu, không gọi Gemini, không làm hỏng lượt build")
    void rateLimited_usesTemplateWithoutCallingAi() {
        RateLimiter rateLimiter = new RateLimiter(Clock.systemUTC(), 1);
        rateLimiter.acquireAiCall("ip:test");   // lượt duy nhất đã dùng ở /requirements/analyze
        BuildExplainerImpl limited = new BuildExplainerImpl(llmClient, rateLimiter, new ObjectMapper());

        Explanation explanation = limited.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test");

        assertThat(explanation.source()).isEqualTo(Source.TEMPLATE);
        assertThat(explanation.reason()).contains("thao tác quá nhanh");
        verify(llmClient, never()).generateJson(anyString(), anyString(), anyString(), anyMap(), eq(BuildExplanationDraft.class));
    }

    @Test
    @DisplayName("AI trả JSON null hoặc message rỗng → câu mẫu")
    void emptyAiReply_usesTemplate() {
        when(llmClient.generateJson(eq("explainBuild"), anyString(), anyString(), anyMap(), eq(BuildExplanationDraft.class)))
                .thenReturn(null, new BuildExplanationDraft("   "));

        assertThat(explainer.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test").source()).isEqualTo(Source.TEMPLATE);
        assertThat(explainer.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test").reason()).isEqualTo("AI trả câu rỗng");
    }

    @Test
    @DisplayName("AI viết dài quá 1.000 ký tự → câu mẫu")
    void tooLongAiMessage_usesTemplate() {
        aiReplies(GOOD_MESSAGE + " Dạ".repeat(400));

        Explanation explanation = explainer.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test");

        assertThat(explanation.source()).isEqualTo(Source.TEMPLATE);
        assertThat(explanation.reason()).contains("1000");
    }

    @Test
    @DisplayName("AI quên nói số tiền còn dư → câu mẫu")
    void missingLeftover_fallsBackToTemplate() {
        aiReplies(GOOD_MESSAGE.replace(" và còn dư 100.000đ", ""));

        Explanation explanation = explainer.explain(PURPOSE, BUDGET, lines, TOTAL, "ip:test");

        assertThat(explanation.source()).isEqualTo(Source.TEMPLATE);
        assertThat(explanation.reason()).contains("100.000đ");
    }

    @Test
    @DisplayName("Tổng vừa đúng ngân sách: câu AI không nói tiền dư vẫn được dùng")
    void exactBudget_aiMessageWithoutLeftoverIsAccepted() {
        String message = "Dạ, Shop tư vấn cho bạn bộ PC chơi game trọn bộ với CPU-A và GPU-A. "
                + "Tổng giá là 19.900.000đ, vừa đúng ngân sách 19.900.000đ của bạn ạ.";
        aiReplies(message);

        Explanation explanation = explainer.explain(PURPOSE, TOTAL, lines, TOTAL, "ip:test");

        assertThat(explanation.source()).isEqualTo(Source.AI);
        assertThat(explanation.message()).isEqualTo(message);
    }

    @Test
    @DisplayName("Tổng đúng bằng ngân sách → câu mẫu nói 'vừa đúng ngân sách', không nói 'còn dư'")
    void exactBudget_templateHasNoLeftover() {
        String message = BuildExplainerImpl.template(PURPOSE, TOTAL, lines, TOTAL);

        assertThat(message).endsWith("Tổng giá là 19.900.000đ, vừa đúng ngân sách 19.900.000đ của bạn ạ.")
                .doesNotContain("còn dư");
    }

    @Test
    @DisplayName("Tách số: dấu chấm hàng nghìn gộp lại, số 0 đứng đầu bỏ đi")
    void numbers_parsesVietnameseFormat() {
        assertThat(BuildExplainerImpl.numbers("Tổng 19.900.000đ, 2 thanh DDR5 08GB"))
                .containsExactlyInAnyOrder("19900000", "2", "5", "8");
    }
}
