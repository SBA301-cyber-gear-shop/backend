package com.project.cybershop.pcbuild.internal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.api.dto.request.RequirementAnalyzeRequest;
import com.project.cybershop.pcbuild.api.dto.response.RequirementAnalyzeResponse;
import com.project.cybershop.pcbuild.api.dto.response.RequirementStatus;
import com.project.cybershop.pcbuild.api.dto.response.RequirementSummary;
import com.project.cybershop.pcbuild.internal.service.RequirementService;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import com.project.cybershop.shared.config.JwtUtil;
import com.project.cybershop.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PcRequirementControllerTest {

    private MockMvc mockMvc;
    private RequirementService requirementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        requirementService = mock(RequirementService.class);
        PcRequirementController controller = new PcRequirementController(requirementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Request hợp lệ -> 200 OK và bọc trong ApiResponseDTO")
    void validRequest_returns200() throws Exception {
        RequirementAnalyzeResponse mockResponse = RequirementAnalyzeResponse.builder()
                .status(RequirementStatus.READY)
                .message("Shop đã hiểu yêu cầu")
                .requirement(RequirementSummary.builder().budget(30_000_000L).purpose("GAMING").build())
                .minBudget(16_000_000L)
                .maxBuildPrice(100_000_000L)
                .build();

        when(requirementService.analyze(any(), anyString())).thenReturn(mockResponse);

        RequirementAnalyzeRequest request = new RequirementAnalyzeRequest(30_000_000L, "Build PC chơi game");

        mockMvc.perform(post("/api/pc-build/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.requirement.purpose").value("GAMING"));
    }

    @Test
    @DisplayName("Budget thiếu hoặc <= 0 -> 400 Bad Request")
    void invalidBudget_returns400() throws Exception {
        String invalidJson = "{\"budget\": -100, \"message\": \"chơi game\"}";

        mockMvc.perform(post("/api/pc-build/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.budget").exists());
    }

    @Test
    @DisplayName("Message rỗng -> 400 Bad Request")
    void blankMessage_returns400() throws Exception {
        String invalidJson = "{\"budget\": 20000000, \"message\": \"   \"}";

        mockMvc.perform(post("/api/pc-build/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.message").exists());
    }

    @Test
    @DisplayName("Vượt quá rate limit -> 429 Too Many Requests kèm Retry-After header")
    void rateLimited_returns429WithHeader() throws Exception {
        when(requirementService.analyze(any(), anyString()))
                .thenThrow(new PcBuildException("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS,
                        "Bạn thao tác quá nhanh. Vui lòng thử lại sau 15 giây.",
                        Map.of("retryAfterSeconds", 15L), 15L));

        RequirementAnalyzeRequest request = new RequirementAnalyzeRequest(20_000_000L, "chơi game");

        mockMvc.perform(post("/api/pc-build/requirements/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "15"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}

