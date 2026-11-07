package com.project.cybershop.pcbuild.internal.controller;

import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.internal.service.PcBuildService;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import com.project.cybershop.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PcBuildControllerTest {

    private static final String AUTO_URL = "/api/pc-build/builds/auto";
    private static final String VALIDATE_URL = "/api/pc-build/builds/validate";

    private MockMvc mockMvc;
    private PcBuildService pcBuildService;

    @BeforeEach
    void setUp() {
        pcBuildService = mock(PcBuildService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PcBuildController(pcBuildService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("auto hợp lệ → 200, kết quả bọc trong ApiResponseDTO")
    void autoBuild_returns200() throws Exception {
        when(pcBuildService.autoBuild(any())).thenReturn(BuildResultResponse.builder()
                .status(BuildStatus.VALID).budget(20_000_000L).purpose("GAMING").total(19_900_000L)
                .difference(-100_000L).build());

        mockMvc.perform(post(AUTO_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 20000000, \"purpose\": \"GAMING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("VALID"))
                .andExpect(jsonPath("$.data.difference").value(-100000));
    }

    @Test
    @DisplayName("auto với budget âm và purpose rỗng → 400 kèm validationErrors")
    void autoBuild_invalidBody_returns400() throws Exception {
        mockMvc.perform(post(AUTO_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": -1, \"purpose\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.budget").exists())
                .andExpect(jsonPath("$.validationErrors.purpose").exists());
    }

    @Test
    @DisplayName("BUILD_NOT_FOUND → 422 kèm code và data.slot")
    void autoBuild_buildNotFound_returns422() throws Exception {
        when(pcBuildService.autoBuild(any())).thenThrow(new PcBuildException("BUILD_NOT_FOUND",
                HttpStatus.UNPROCESSABLE_CONTENT, "Shop chưa ráp được bộ PC", Map.of("slot", "PSU"), null));

        mockMvc.perform(post(AUTO_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 20000000, \"purpose\": \"GAMING\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("BUILD_NOT_FOUND"))
                .andExpect(jsonPath("$.data.slot").value("PSU"));
    }

    @Test
    @DisplayName("validate với items rỗng hoặc qty = 0 → 400")
    void validate_invalidBody_returns400() throws Exception {
        mockMvc.perform(post(VALIDATE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 20000000, \"items\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.items").exists());

        mockMvc.perform(post(VALIDATE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 20000000, \"items\": [{\"variantId\": 12, \"qty\": 0}]}"))
                .andExpect(status().isBadRequest());
    }
}
