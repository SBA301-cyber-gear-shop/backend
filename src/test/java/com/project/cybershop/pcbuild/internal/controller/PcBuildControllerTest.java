package com.project.cybershop.pcbuild.internal.controller;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.identity.api.dto.response.UserDto;
import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildStatus;
import com.project.cybershop.pcbuild.api.dto.response.ReplacementOptionResponse;
import com.project.cybershop.pcbuild.api.dto.response.SaveBuildResponse;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PcBuildControllerTest {

    private static final String AUTO_URL = "/api/pc-build/builds/auto";
    private static final String VALIDATE_URL = "/api/pc-build/builds/validate";
    private static final String REPLACEMENTS_URL = "/api/pc-build/builds/replacements";
    private static final String SAVE_URL = "/api/pc-build/builds/save";

    private MockMvc mockMvc;
    private PcBuildService pcBuildService;
    private IdentityApi identityApi;

    @BeforeEach
    void setUp() {
        pcBuildService = mock(PcBuildService.class);
        identityApi = mock(IdentityApi.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PcBuildController(pcBuildService, identityApi))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("auto hợp lệ → 200, kết quả bọc trong ApiResponseDTO, có message bước 8")
    void autoBuild_returns200() throws Exception {
        when(pcBuildService.autoBuild(any(), anyString())).thenReturn(BuildResultResponse.builder()
                .status(BuildStatus.VALID).budget(20_000_000L).purpose("GAMING").total(19_900_000L)
                .difference(-100_000L).message("Dạ, Shop tư vấn cho bạn...").build());

        mockMvc.perform(post(AUTO_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 20000000, \"purpose\": \"GAMING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("VALID"))
                .andExpect(jsonPath("$.data.difference").value(-100000))
                .andExpect(jsonPath("$.data.message").value("Dạ, Shop tư vấn cho bạn..."));
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
        when(pcBuildService.autoBuild(any(), anyString())).thenThrow(new PcBuildException("BUILD_NOT_FOUND",
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

    @Test
    @DisplayName("replacements với category rỗng → 400")
    void replacements_blankCategory_returns400() throws Exception {
        mockMvc.perform(post(REPLACEMENTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \" \", \"items\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.category").exists());
    }

    @Test
    @DisplayName("replacements hợp lệ → 200, danh sách lựa chọn trong data")
    void replacements_returns200() throws Exception {
        when(pcBuildService.replacements(any())).thenReturn(List.of(ReplacementOptionResponse.builder()
                .id(61).name("GPU-A").price(5_600_000L).qty(1).maxQty(1).current(true).build()));

        mockMvc.perform(post(REPLACEMENTS_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"GPU\", \"items\": [{\"variantId\": 61, \"qty\": 1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(61))
                .andExpect(jsonPath("$.data[0].current").value(true));
    }

    @Test
    @DisplayName("save không có người đăng nhập → 401, không gọi service")
    void save_withoutPrincipal_returns401() throws Exception {
        mockMvc.perform(post(SAVE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 20000000, \"items\": [{\"variantId\": 12, \"qty\": 1}]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(pcBuildService);
    }

    @Test
    @DisplayName("save: lấy userId từ email trong token rồi gọi service")
    void save_usesLoggedInUser() throws Exception {
        when(identityApi.requireActiveUser("khach@cybershop.vn")).thenReturn(UserDto.builder().id(7).build());
        when(pcBuildService.save(any(), eq(7))).thenReturn(SaveBuildResponse.builder()
                .buildId(88).status(BuildStatus.VALID).total(19_900_000L).difference(-100_000L).build());

        mockMvc.perform(post(SAVE_URL).principal(() -> "khach@cybershop.vn").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 20000000, \"items\": [{\"variantId\": 12, \"qty\": 1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buildId").value(88));
        verify(pcBuildService).save(any(), eq(7));
    }

    @Test
    @DisplayName("save OVER_BUDGET chưa xác nhận → 409 kèm code để frontend hiện popup")
    void save_overBudget_returns409() throws Exception {
        when(identityApi.requireActiveUser("khach@cybershop.vn")).thenReturn(UserDto.builder().id(7).build());
        when(pcBuildService.save(any(), eq(7))).thenThrow(new PcBuildException("OVER_BUDGET_NOT_CONFIRMED",
                HttpStatus.CONFLICT, "Bộ PC hiện tại đã vượt quá ngân sách 300.000đ", Map.of("difference", 300_000L), null));

        mockMvc.perform(post(SAVE_URL).principal(() -> "khach@cybershop.vn").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\": 20000000, \"items\": [{\"variantId\": 12, \"qty\": 1}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OVER_BUDGET_NOT_CONFIRMED"))
                .andExpect(jsonPath("$.data.difference").value(300000));
    }
}
