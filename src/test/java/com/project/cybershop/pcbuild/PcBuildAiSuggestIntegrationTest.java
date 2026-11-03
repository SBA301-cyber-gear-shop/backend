package com.project.cybershop.pcbuild;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.BuildTier;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Luồng /ai-suggest đầy đủ (DD 3.3, 13.2) trên H2 với biến thể 1–118: LLM được mock, solver/engine/từ điển chạy thật.
 * DB H2 riêng (pcbuild-ai) vì context này có @MockitoBean nên không dùng chung context với các test pcbuild khác.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pcbuild-ai;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:purchase-schema.sql,classpath:pcbuild/pcbuild-schema.sql",
        "spring.sql.init.data-locations=classpath:pcbuild-seed.sql"
})
@AutoConfigureMockMvc
@DisplayName("POST /ai-suggest - tích hợp hồ sơ → solver → giải thích với LLM giả lập")
class PcBuildAiSuggestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfilePresets presets;

    @Autowired
    private CandidateRepositoryFacade facade;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @MockitoBean
    private LlmClientService llm;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void resetLlm() {
        Mockito.reset(llm);
    }

    // Giải thích giả lập: lý do cho mọi món của bộ máy + 1 variantId bịa (phải bị bỏ → PARTIAL)
    private void stubExplanation() {
        when(llm.explainBuild(anyString(), anyString(), any())).thenAnswer(invocation -> {
            JsonNode context = json.readTree((String) invocation.getArgument(1));
            LlmClientService.ExplanationDraft draft = new LlmClientService.ExplanationDraft();
            draft.setBuildName("Máy chơi game 2K");
            draft.setAiReasoning("Bộ máy cân bằng, còn dư " + context.path("build").path("leftover").asText() + " ₫.");
            draft.setPerformanceHighlight("Chơi tốt ở 2K thiết lập cao.");
            List<LlmClientService.ExplanationDraft.PartReason> reasons = new ArrayList<>();
            for (JsonNode part : context.path("build").path("parts")) {
                LlmClientService.ExplanationDraft.PartReason reason = new LlmClientService.ExplanationDraft.PartReason();
                reason.setVariantId(part.path("variantId").asInt());
                reason.setReason("Hợp nhu cầu");
                reasons.add(reason);
            }
            LlmClientService.ExplanationDraft.PartReason invented = new LlmClientService.ExplanationDraft.PartReason();
            invented.setVariantId(99999);
            invented.setReason("Món AI bịa");
            reasons.add(invented);
            draft.setPartReasons(reasons);
            return draft;
        });
    }

    private RequirementProfileDto gamingDraft() {
        RequirementProfileDto draft = presets.preset(Archetype.GAMING, BuildTier.T3);
        draft.setSummary("Chơi game 2K, thỉnh thoảng stream");
        return draft;
    }

    @Test
    @DisplayName("AI trích hồ sơ GAMING → bộ máy tương thích, tổng ≤ B, budgetFit, profile có label/satisfied, aiStatus PARTIAL")
    void suggestsCompatibleBuildWithinBudget() throws Exception {
        when(llm.extractProfile(anyString(), anyString(), any())).thenReturn(gamingDraft());
        stubExplanation();

        MvcResult result = mockMvc.perform(post("/api/v1/pc-build/ai-suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":30000000,\"purpose\":\"Chơi game 2K, thỉnh thoảng stream\",\"userPrompt\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCompatible").value(true))
                .andExpect(jsonPath("$.data.buildName").value("Máy chơi game 2K"))
                .andExpect(jsonPath("$.data.budgetFit.status").value("IN_WINDOW"))
                .andExpect(jsonPath("$.data.profile.source").value("AI"))
                .andExpect(jsonPath("$.data.profile.constraints[0].label").value("Perf Tier ≥ 3"))
                .andExpect(jsonPath("$.data.profile.constraints[0].satisfied").value(true))
                .andExpect(jsonPath("$.data.metadata.extractStatus").value("OK"))
                .andExpect(jsonPath("$.data.metadata.aiStatus").value("PARTIAL"))
                .andExpect(jsonPath("$.data.metadata.model").value("gemini-3.5-flash-lite"))
                .andReturn();

        JsonNode data = json.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("totalPrice").decimalValue()).isLessThanOrEqualTo(new java.math.BigDecimal("30000000"));
        assertThat(data.path("items")).allSatisfy(item -> assertThat(item.path("aiReason").asText()).isEqualTo("Hợp nhu cầu"));
        assertThat(data.path("checks")).noneSatisfy(check -> assertThat(check.path("status").asText()).isEqualTo("fail"));
    }

    @Test
    @DisplayName("Gửi lại cùng câu nhập → CACHED, AI trích hồ sơ chỉ được gọi 1 lần; cache ấm thì 0 câu SQL (AC13)")
    void secondRequestUsesCacheWithoutSql() throws Exception {
        when(llm.extractProfile(anyString(), anyString(), any())).thenReturn(gamingDraft());
        stubExplanation();
        String body = "{\"budget\":31000000,\"purpose\":\"Game eSport FPS cao\"}";
        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        facade.snapshot();
        statistics.clear();
        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":31000000,\"purpose\":\"  GAME esport   fps CAO \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metadata.extractStatus").value("CACHED"));

        verify(llm, times(1)).extractProfile(anyString(), anyString(), any());
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    private RequirementProfileDto maxPriceDraft() {
        RequirementProfileDto draft = presets.preset(Archetype.AI_DATA, BuildTier.T7);
        draft.setSummary("Máy đắt nhất");
        draft.setObjective("MAX_PRICE");
        return draft;
    }

    @Test
    @DisplayName("Khách nói rõ \"đắt nhất\" → chế độ MAX_PRICE, báo MAX_PRICE_REACHED, AI giải thích được biết objective")
    void explicitMostExpensiveEnablesMaxPrice() throws Exception {
        when(llm.extractProfile(anyString(), anyString(), any())).thenReturn(maxPriceDraft());
        stubExplanation();

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":4000000000,\"purpose\":\"Training AI\",\"userPrompt\":\"Lấy toàn linh kiện ĐẮT TIỀN nhất\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reasonCode").value("MAX_PRICE_REACHED"))
                .andExpect(jsonPath("$.data.budgetFit.status").value("UNDER_MAX_PRICE"))
                .andExpect(jsonPath("$.data.profile.objective").value("MAX_PRICE"));

        org.mockito.ArgumentCaptor<String> context = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(llm).explainBuild(anyString(), context.capture(), any());
        assertThat(json.readTree(context.getValue()).path("profile").path("objective").asText()).isEqualTo("MAX_PRICE");
    }

    @Test
    @DisplayName("Khách chỉ nói \"tốt nhất\", AI vẫn đặt MAX_PRICE → Java bỏ objective, chọn theo nhu cầu như thường")
    void bestIsNotMostExpensive() throws Exception {
        when(llm.extractProfile(anyString(), anyString(), any())).thenReturn(maxPriceDraft());
        stubExplanation();

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":4000000000,\"purpose\":\"Training AI\",\"userPrompt\":\"Cho tôi cấu hình tốt nhất, mạnh nhất\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.objective").doesNotExist())
                .andExpect(result -> assertThat(json.readTree(result.getResponse().getContentAsString())
                        .path("data").path("reasonCode").asText()).isNotEqualTo("MAX_PRICE_REACHED"));
    }

    @Test
    @DisplayName("Khách gửi lại hồ sơ đã sửa có MAX_PRICE → giữ chế độ, không cần nhắc lại \"đắt nhất\"")
    void editedProfileKeepsMaxPrice() throws Exception {
        stubExplanation();
        String body = json.writeValueAsString(java.util.Map.of("budget", 4000000000L, "profile", maxPriceDraft()));

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reasonCode").value("MAX_PRICE_REACHED"));
    }

    @Test
    @DisplayName("Khách gửi profile đã sửa → không gọi AI trích hồ sơ, source USER_EDITED")
    void userProfileSkipsExtraction() throws Exception {
        stubExplanation();
        RequirementProfileDto profile = gamingDraft();
        profile.setSource("AI");
        String body = json.writeValueAsString(java.util.Map.of("budget", 30000000, "profile", profile));

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.source").value("USER_EDITED"))
                .andExpect(jsonPath("$.data.metadata.extractStatus").value("USER_PROFILE"));
        verify(llm, never()).extractProfile(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("AI giải thích lỗi → 503 AI_UNAVAILABLE với header Retry-After, không trả bộ máy")
    void aiFailureReturns503() throws Exception {
        when(llm.extractProfile(anyString(), anyString(), any())).thenReturn(gamingDraft());
        when(llm.explainBuild(anyString(), anyString(), any()))
                .thenThrow(new AiUnavailableException(AiUnavailableException.Detail.TIMEOUT));

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":45000000,\"purpose\":\"Chơi game\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.detail").value("TIMEOUT"))
                .andExpect(jsonPath("$.data.items").doesNotExist());
    }

    @Test
    @DisplayName("Nhu cầu mơ hồ (confidence 0,2) → 422 NEEDS_UNCLEAR kèm clarifyingQuestion")
    void unclearNeeds() throws Exception {
        RequirementProfileDto vague = RequirementProfileDto.builder().summary("Máy tốt").archetype("GENERAL").confidence(0.2)
                .clarifyingQuestion("Bạn dùng máy chủ yếu để làm gì?").build();
        when(llm.extractProfile(anyString(), anyString(), any())).thenReturn(vague);

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":30000000,\"purpose\":\"Máy tốt\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NEEDS_UNCLEAR"))
                .andExpect(jsonPath("$.data.clarifyingQuestion").value("Bạn dùng máy chủ yếu để làm gì?"));
        verify(llm, never()).explainBuild(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("AC4: 7.999.999 → 400 BUDGET_BELOW_FLOOR; 10.000.000.000 → 400 BUDGET_TOO_HIGH; purpose rỗng → NEEDS_REQUIRED")
    void inputValidation() throws Exception {
        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":7999999,\"purpose\":\"Chơi game\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUDGET_BELOW_FLOOR"))
                .andExpect(jsonPath("$.data.minBudget").value(8000000));
        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":10000000000,\"purpose\":\"Chơi game\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUDGET_TOO_HIGH"));
        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":30000000,\"purpose\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NEEDS_REQUIRED"));
        verify(llm, never()).extractProfile(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("GET /options: minBudget, floorPrice, maxBudget, từ điển có role và metric, 0 câu SQL khi cache ấm")
    void optionsFromSnapshot() throws Exception {
        facade.snapshot();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/v1/pc-build/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minBudget").value(8000000))
                .andExpect(jsonPath("$.data.floorPrice").value(6778000))
                .andExpect(jsonPath("$.data.maxBudget").value(9999999999L))
                .andExpect(jsonPath("$.data.dictionary.roles.gpu.specs.VRAM_GB.type").value("INT"))
                .andExpect(jsonPath("$.data.dictionary.roles.case.specs.Color.values.WHITE").value("Trắng"))
                .andExpect(jsonPath("$.data.dictionary.metrics.TOTAL_RAM_GB.max").exists())
                .andExpect(jsonPath("$.data.examples[0]").exists())
                .andExpect(jsonPath("$.data.tiers[2].code").value("T3"));
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }
}
