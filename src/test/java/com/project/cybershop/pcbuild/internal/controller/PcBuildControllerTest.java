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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PcBuildController.class)
@AutoConfigureMockMvc(addFilters = false) // Tắt filter bảo mật để tập trung test logic endpoint Controller
@DisplayName("PcBuildController - Kiểm thử 100% các API Endpoints và Validation")
class PcBuildControllerTest {

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
    @DisplayName("POST /api/v1/pc-build/ai-suggest: Thất bại khi ngân sách dưới 5.000.000 -> HTTP 400 Bad Request")
    void shouldReturn400WhenBudgetIsLessThanMinimum() throws Exception {
        AiBuildRequest invalidRequest = new AiBuildRequest();
        invalidRequest.setBudget(new BigDecimal("3000000")); // Dưới mức tối thiểu 5 triệu

        mockMvc.perform(post("/api/v1/pc-build/ai-suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/pc-build/compatible-replacements: Lấy danh sách linh kiện thay thế -> HTTP 200")
    void shouldReturnReplacementsList() throws Exception {
        CompatibleReplacementResponse dto = CompatibleReplacementResponse.builder()
                .variantId(101)
                .productName("Intel i5 13400F")
                .price(new BigDecimal("4690000"))
                .isCompatible(true)
                .build();

        when(pcCustomService.getCompatibleReplacements("cpu", null, null)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/pc-build/compatible-replacements")
                        .param("role", "cpu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].productName").value("Intel i5 13400F"));
    }

    @Test
    @DisplayName("POST /api/v1/pc-build/{buildId}/save: Chưa đăng nhập (User null) -> HTTP 401")
    void shouldReturn401WhenUserNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/v1/pc-build/10/save"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập để lưu cấu hình"));
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
}
