package com.project.cybershop.pcbuild.internal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.shared.config.JwtUtil;
import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.AiBuildResponse;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.internal.controller.PcBuildController;
import com.project.cybershop.pcbuild.internal.service.AiPcBuilderService;
import com.project.cybershop.pcbuild.internal.service.PcCustomService;
import com.project.cybershop.identity.api.dto.response.UserDto;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildDetailResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildSummaryResponse;
import com.project.cybershop.pcbuild.api.dto.request.ReplacementQueryRequest;
import com.project.cybershop.pcbuild.api.dto.response.BudgetFitResponse;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;
import com.project.cybershop.pcbuild.api.dto.response.ValidateBuildResponse;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import com.project.cybershop.pcbuild.internal.support.RateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PcBuildController.class, properties = {
        "app.pcbuild.rate-limit-per-minute=6",
        "app.pcbuild.rate-limit-check-per-minute=30"
})
@AutoConfigureMockMvc(addFilters = false) // Tắt filter bảo mật để tập trung test logic endpoint Controller
@Import({RateLimiter.class, PcBuildControllerTest.ClockConfig.class})
@DisplayName("PcBuildController - Kiểm thử 100% các API Endpoints và Validation")
class PcBuildControllerTest {

    // RateLimiter thật với hạn mức thật (DD 11.1); mỗi ca 429 dùng IP riêng vì bộ đếm sống suốt context
    @TestConfiguration
    static class ClockConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AiPcBuilderService aiPcBuilderService;

    @MockitoBean
    private PcCustomService pcCustomService;

    @MockitoBean
    private IdentityApi identityApi;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /api/v1/pc-build/ai-suggest: Thành công với dữ liệu hợp lệ -> HTTP 200")
    void shouldReturn200WhenSuggestBuildRequestIsValid() throws Exception {
        AiBuildRequest request = new AiBuildRequest();
        request.setBudget(new BigDecimal("25000000"));
        request.setPurpose("gaming");

        AiBuildResponse responseDto = AiBuildResponse.builder()
                .buildId(1)
                .buildName("Bạch Hổ Gaming")
                .totalPrice(new BigDecimal("24500000"))
                .isCompatible(true)
                .build();

        when(aiPcBuilderService.processAiBuild(any(), any())).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.buildId").value(1))
                .andExpect(jsonPath("$.data.buildName").value("Bạch Hổ Gaming"));
    }

    @Test
    @DisplayName("POST /api/v1/pc-build/ai-suggest: ngân sách 0 -> HTTP 400 (ngưỡng 8 triệu kiểm ở RequestValidator, trả BUDGET_BELOW_FLOOR)")
    void shouldReturn400WhenBudgetIsLessThanMinimum() throws Exception {
        AiBuildRequest invalidRequest = new AiBuildRequest();
        invalidRequest.setBudget(BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/pc-build/compatible-replacements (bản cũ) đã bị xóa -> không xử lý, không gọi service")
    void shouldNotExposeLegacyGetReplacements() throws Exception {
        mockMvc.perform(get("/api/v1/pc-build/compatible-replacements").param("role", "cpu"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(400));
        org.mockito.Mockito.verifyNoInteractions(pcCustomService);
    }

    private static RequestPostProcessor fromIp(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    @Test
    @DisplayName("429: /ai-suggest lần thứ 7 trong một phút từ cùng IP -> RATE_LIMITED + Retry-After; X-Forwarded-For giả không lách được; IP khác vẫn qua")
    void shouldRateLimitAiSuggestPerIp() throws Exception {
        when(aiPcBuilderService.processAiBuild(any(), any())).thenReturn(AiBuildResponse.builder().buildName("B").build());
        String body = "{\"budget\":30000000,\"purpose\":\"Chơi game\"}";

        for (int attempt = 0; attempt < 6; attempt++) {
            mockMvc.perform(post("/api/v1/pc-build/ai-suggest").with(fromIp("10.0.0.7"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").with(fromIp("10.0.0.7"))
                        .header("X-Forwarded-For", "1.2.3.4")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").isNumber());
        verify(aiPcBuilderService, org.mockito.Mockito.times(6)).processAiBuild(any(), any());

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").with(fromIp("10.0.0.8"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("429: /validate cho 30 lần mỗi phút; user đăng nhập được đếm theo tài khoản, không theo IP")
    void shouldRateLimitValidateAt30PerMinute() throws Exception {
        when(pcCustomService.validateBuild(any())).thenReturn(ValidateBuildResponse.builder().isCompatible(true).build());

        for (int attempt = 0; attempt < 30; attempt++) {
            mockMvc.perform(post("/api/v1/pc-build/validate").with(fromIp("10.0.0.9"))
                            .contentType(MediaType.APPLICATION_JSON).content(buildItemsJson(1, 1)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/pc-build/validate").with(fromIp("10.0.0.9"))
                        .contentType(MediaType.APPLICATION_JSON).content(buildItemsJson(1, 1)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        // Cùng IP nhưng đã đăng nhập: bộ đếm riêng của tài khoản
        mockMvc.perform(post("/api/v1/pc-build/validate").with(fromIp("10.0.0.9"))
                        .principal(() -> "limit@test.com")
                        .contentType(MediaType.APPLICATION_JSON).content(buildItemsJson(1, 1)))
                .andExpect(status().isOk());
        // Endpoint khác có bộ đếm riêng
        when(pcCustomService.getCompatibleReplacements(any(ReplacementQueryRequest.class))).thenReturn(List.of());
        mockMvc.perform(post("/api/v1/pc-build/compatible-replacements").with(fromIp("10.0.0.9"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"psu\",\"items\":[{\"role\":\"psu\",\"variantId\":93,\"quantity\":1}]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("B16: POST /api/v1/pc-build/{buildId}/save đã bị xóa -> không xử lý, không gọi service")
    void shouldNotExposeLegacySaveByIdEndpoint() throws Exception {
        when(identityApi.getUserByEmail("user@test.com")).thenReturn(Optional.of(UserDto.builder().id(1).build()));

        mockMvc.perform(post("/api/v1/pc-build/10/save").principal(() -> "user@test.com"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(400));
        org.mockito.Mockito.verifyNoInteractions(pcCustomService);
    }

    @Test
    @DisplayName("B17: POST /api/v1/pc-build/save không gửi totalPrice/unitPrice -> vẫn hợp lệ, HTTP 200")
    void shouldSaveCustomBuildWithoutClientPrices() throws Exception {
        when(identityApi.getUserByEmail("user@test.com")).thenReturn(Optional.of(UserDto.builder().id(1).build()));
        when(pcCustomService.saveNewBuild(any(), any())).thenReturn(77);

        mockMvc.perform(post("/api/v1/pc-build/save")
                        .principal(() -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildName\":\"Test\",\"items\":[{\"variantId\":101,\"role\":\"cpu\",\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(77));
    }

    @Test
    @DisplayName("POST /api/v1/pc-build/save: quantity = 0 -> HTTP 400")
    void shouldRejectSaveWithZeroQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/pc-build/save")
                        .principal(() -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":101,\"role\":\"cpu\",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/pc-build/save: Chưa đăng nhập -> HTTP 401")
    void shouldReturn401WhenSaveCustomBuildNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/v1/pc-build/save")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"buildName\":\"Test\",\"totalPrice\":10000000,\"items\":[{\"variantId\":101,\"role\":\"cpu\",\"quantity\":1,\"unitPrice\":10000000}]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập để lưu cấu hình"));
    }

    @Test
    @DisplayName("GET /api/v1/pc-build/my-builds: Chưa đăng nhập -> HTTP 401")
    void shouldReturn401WhenGetMySavedBuildsNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/v1/pc-build/my-builds"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập để xem danh sách cấu hình đã lưu"));
    }

    @Test
    @DisplayName("GET /api/v1/pc-build/my-builds: Đã đăng nhập -> HTTP 200 và danh sách cấu hình")
    void shouldReturn200AndListWhenGetMySavedBuildsLoggedIn() throws Exception {
        when(identityApi.getUserByEmail("test@example.com"))
                .thenReturn(Optional.of(UserDto.builder().id(10).email("test@example.com").build()));

        SavedPcBuildSummaryResponse summary = SavedPcBuildSummaryResponse.builder()
                .buildId(1)
                .buildName("Bộ PC Intel i5-13400F & RTX 4060")
                .totalPrice(new BigDecimal("18000000"))
                .cpuName("Intel i5-13400F")
                .gpuName("RTX 4060")
                .itemCount(8)
                .build();

        when(pcCustomService.getMySavedBuilds(10)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/pc-build/my-builds")
                        .principal(() -> "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].buildId").value(1))
                .andExpect(jsonPath("$.data[0].buildName").value("Bộ PC Intel i5-13400F & RTX 4060"));
    }

    @Test
    @DisplayName("GET /api/v1/pc-build/{buildId}: Chưa đăng nhập -> HTTP 401")
    void shouldReturn401WhenGetSavedBuildDetailNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/v1/pc-build/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập để xem chi tiết cấu hình"));
    }

    @Test
    @DisplayName("GET /api/v1/pc-build/{buildId}: Đã đăng nhập -> HTTP 200 và chi tiết cấu hình")
    void shouldReturn200AndDetailWhenGetSavedBuildDetailLoggedIn() throws Exception {
        when(identityApi.getUserByEmail("test@example.com"))
                .thenReturn(Optional.of(UserDto.builder().id(10).email("test@example.com").build()));

        SavedPcBuildDetailResponse detail = SavedPcBuildDetailResponse.builder()
                .buildId(1)
                .buildName("Bộ PC Intel i5-13400F & RTX 4060")
                .totalPrice(new BigDecimal("18000000"))
                .items(List.of())
                .build();

        when(pcCustomService.getSavedBuildDetail(1, 10)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/pc-build/1")
                        .principal(() -> "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.buildId").value(1))
                .andExpect(jsonPath("$.data.buildName").value("Bộ PC Intel i5-13400F & RTX 4060"));
    }

    @Test
    @DisplayName("DELETE /api/v1/pc-build/{buildId}: Chưa đăng nhập -> HTTP 401")
    void shouldReturn401WhenDeleteSavedBuildNotLoggedIn() throws Exception {
        mockMvc.perform(delete("/api/v1/pc-build/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập để thực hiện thao tác này"));
    }

    @Test
    @DisplayName("DELETE /api/v1/pc-build/{buildId}: Đã đăng nhập -> HTTP 200 và xóa thành công")
    void shouldReturn200WhenDeleteSavedBuildLoggedIn() throws Exception {
        when(identityApi.getUserByEmail("test@example.com"))
                .thenReturn(Optional.of(UserDto.builder().id(10).email("test@example.com").build()));

        mockMvc.perform(delete("/api/v1/pc-build/1")
                        .principal(() -> "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Đã xóa cấu hình PC thành công"));

        verify(pcCustomService).deleteSavedBuild(1, 10);
    }

    private static String buildItemsJson(int count, int quantity) {
        StringBuilder items = new StringBuilder();
        for (int index = 0; index < count; index++) {
            if (index > 0) items.append(',');
            items.append("{\"role\":\"ssd\",\"variantId\":").append(80 + index).append(",\"quantity\":").append(quantity).append('}');
        }
        return "{\"budget\":30000000,\"items\":[" + items + "]}";
    }

    @Test
    @DisplayName("POST /api/v1/pc-build/validate: build hợp lệ -> HTTP 200 kèm isCompatible, budgetFit, checks")
    void shouldValidateBuild() throws Exception {
        ValidateBuildResponse response = ValidateBuildResponse.builder()
                .isCompatible(true).hasWarnings(true).hasUnknown(false)
                .totalPrice(new BigDecimal("29808000"))
                .budgetFit(BudgetFitResponse.builder().status("IN_WINDOW").percent(new BigDecimal("99.4")).build())
                .checks(List.of(CompatibilityCheckResponse.of("R1", "CPU ↔ Bo mạch chủ (Socket)", "pass", "Tương thích (AM5 = AM5)")))
                .requiredPsuW(477)
                .build();
        when(pcCustomService.validateBuild(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/pc-build/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildItemsJson(1, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCompatible").value(true))
                .andExpect(jsonPath("$.data.hasWarnings").value(true))
                .andExpect(jsonPath("$.data.budgetFit.status").value("IN_WINDOW"))
                .andExpect(jsonPath("$.data.checks[0].key").value("R1"))
                .andExpect(jsonPath("$.data.checks[0].valid").value(true))
                .andExpect(jsonPath("$.data.requiredPsuW").value(477));
    }

    @Test
    @DisplayName("POST /api/v1/pc-build/validate: quantity 9, 17 dòng hoặc items rỗng -> HTTP 400")
    void shouldRejectInvalidValidateRequests() throws Exception {
        for (String body : List.of(buildItemsJson(1, 9), buildItemsJson(17, 1), buildItemsJson(0, 1))) {
            mockMvc.perform(post("/api/v1/pc-build/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        org.mockito.Mockito.verifyNoInteractions(pcCustomService);
    }

    @Test
    @DisplayName("POST /api/v1/pc-build/compatible-replacements: gửi toàn bộ build -> HTTP 200 với status, failedChecks")
    void shouldQueryReplacementsWithFullBuild() throws Exception {
        CompatibleReplacementResponse dto = CompatibleReplacementResponse.builder()
                .variantId(94).productName("MSI MAG A650BN").price(new BigDecimal("1190000"))
                .isCompatible(false).compatibilityNote("Vượt ngân sách").status("fail")
                .failedChecks(List.of("J8a")).warningChecks(List.of())
                .newTotal(new BigDecimal("30008000")).build();
        when(pcCustomService.getCompatibleReplacements(any(ReplacementQueryRequest.class))).thenReturn(List.of(dto));

        mockMvc.perform(post("/api/v1/pc-build/compatible-replacements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"psu\",\"budget\":30000000,\"items\":[{\"role\":\"psu\",\"variantId\":93,\"quantity\":1}],\"limit\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].isCompatible").value(false))
                .andExpect(jsonPath("$.data[0].status").value("fail"))
                .andExpect(jsonPath("$.data[0].failedChecks[0]").value("J8a"));

        org.mockito.ArgumentCaptor<ReplacementQueryRequest> captor = org.mockito.ArgumentCaptor.forClass(ReplacementQueryRequest.class);
        verify(pcCustomService).getCompatibleReplacements(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getRole()).isEqualTo("psu");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getItems()).hasSize(1);
    }

    @Test
    @DisplayName("POST /api/v1/pc-build/save: build không tương thích -> HTTP 422 với code BUILD_INCOMPATIBLE và data.checks")
    void shouldReturn422WhenSavingIncompatibleBuild() throws Exception {
        when(identityApi.getUserByEmail("user@test.com")).thenReturn(Optional.of(UserDto.builder().id(1).build()));
        when(pcCustomService.saveNewBuild(any(), any())).thenThrow(new BuildRuleException("BUILD_INCOMPATIBLE",
                org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Cấu hình có linh kiện không tương thích",
                java.util.Map.of("checks", List.of(CompatibilityCheckResponse.of("R1", "Socket", "fail", "Lệch socket")))));

        mockMvc.perform(post("/api/v1/pc-build/save")
                        .principal(() -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":101,\"role\":\"cpu\",\"quantity\":1}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUILD_INCOMPATIBLE"))
                .andExpect(jsonPath("$.data.checks[0].key").value("R1"));
    }

    @Test
    @DisplayName("GET /api/v1/pc-build/options -> HTTP 200 với minBudget, floorPrice, examples")
    void shouldReturnBuildOptions() throws Exception {
        when(aiPcBuilderService.options()).thenReturn(com.project.cybershop.pcbuild.api.dto.response.BuildOptionsResponse.builder()
                .minBudget(new BigDecimal("8000000")).floorPrice(new BigDecimal("6778000")).maxBudget(new BigDecimal("9999999999"))
                .examples(List.of("Chơi game 2K")).build());

        mockMvc.perform(get("/api/v1/pc-build/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minBudget").value(8000000))
                .andExpect(jsonPath("$.data.floorPrice").value(6778000))
                .andExpect(jsonPath("$.data.examples[0]").value("Chơi game 2K"));
    }

    @Test
    @DisplayName("POST /ai-suggest: AI lỗi -> HTTP 503 AI_UNAVAILABLE với header Retry-After")
    void shouldReturn503WithRetryAfterWhenAiUnavailable() throws Exception {
        when(aiPcBuilderService.processAiBuild(any(), any())).thenThrow(new com.project.cybershop.pcbuild.internal.support.AiUnavailableException(
                com.project.cybershop.pcbuild.internal.support.AiUnavailableException.Detail.CIRCUIT_OPEN, 17));

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":30000000,\"purpose\":\"Chơi game\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "17"))
                .andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.detail").value("CIRCUIT_OPEN"));
    }

    @Test
    @DisplayName("POST /ai-suggest: nhu cầu mơ hồ -> HTTP 422 NEEDS_UNCLEAR có clarifyingQuestion")
    void shouldReturn422WhenNeedsUnclear() throws Exception {
        when(aiPcBuilderService.processAiBuild(any(), any())).thenThrow(new BuildRuleException("NEEDS_UNCLEAR",
                org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, "Nhu cầu chưa đủ rõ",
                java.util.Map.of("clarifyingQuestion", "Bạn dùng máy để làm gì?")));

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":30000000,\"purpose\":\"Máy tốt\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NEEDS_UNCLEAR"))
                .andExpect(jsonPath("$.data.clarifyingQuestion").value("Bạn dùng máy để làm gì?"));
    }
}
