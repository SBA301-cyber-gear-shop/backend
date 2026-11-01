package com.project.cybershop.pcbuild;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phân quyền endpoint PC Build qua filter bảo mật thật (DD 9.7). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("SecurityConfig - phân quyền endpoint PC Build (DD 9.7)")
class PcBuildSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final ResultMatcher NOT_UNAUTHORIZED =
            result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401);

    @Test
    @DisplayName("Chưa đăng nhập: /save, /my-builds, GET và DELETE /{id} -> 401")
    void savedBuildEndpointsRequireLogin() throws Exception {
        mockMvc.perform(post("/api/v1/pc-build/save").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":1,\"role\":\"cpu\",\"quantity\":1}]}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/pc-build/my-builds")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/pc-build/5")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/pc-build/5")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Khách vãng lai vẫn dùng được /options, /validate, /compatible-replacements, /ai-suggest")
    void suggestionEndpointsStayPublic() throws Exception {
        String items = "\"items\":[{\"role\":\"cpu\",\"variantId\":1,\"quantity\":1}]";
        mockMvc.perform(get("/api/v1/pc-build/options")).andExpect(NOT_UNAUTHORIZED);
        mockMvc.perform(post("/api/v1/pc-build/validate").contentType(MediaType.APPLICATION_JSON)
                .content("{" + items + "}")).andExpect(NOT_UNAUTHORIZED);
        mockMvc.perform(post("/api/v1/pc-build/compatible-replacements").contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"cpu\"," + items + "}")).andExpect(NOT_UNAUTHORIZED);
        // Ngân sách dưới sàn: bị từ chối ở tầng nghiệp vụ (400), không phải vì thiếu đăng nhập, và không gọi AI
        mockMvc.perform(post("/api/v1/pc-build/ai-suggest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"budget\":1000,\"purpose\":\"Chơi game\"}")).andExpect(NOT_UNAUTHORIZED);
    }
}
