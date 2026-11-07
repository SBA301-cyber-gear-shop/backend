package com.project.cybershop.pcbuild;

import com.project.cybershop.shared.config.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chạy SecurityConfig thật: chỉ /builds/save cần đăng nhập, /builds/replacements vẫn công khai.
 * Body cố ý sai để request dừng ở bước validate (400), chứng tỏ đã qua lớp bảo mật mà không cần dữ liệu kho.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PcBuildSecurityIntegrationTest {

    private static final String INVALID_SAVE_BODY = "{\"budget\": 20000000, \"items\": []}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /builds/save không có token → 401 với câu báo đăng nhập")
    void save_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/pc-build/builds/save").contentType(MediaType.APPLICATION_JSON).content(INVALID_SAVE_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bạn cần đăng nhập để tiếp tục."));
    }

    @Test
    @DisplayName("POST /builds/save có token → qua lớp bảo mật, bị chặn ở validate body (400)")
    void save_withToken_passesSecurity() throws Exception {
        String token = "Bearer " + jwtUtil.generateToken("customer@techzone.demo", "CUSTOMER");

        mockMvc.perform(post("/api/pc-build/builds/save").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(INVALID_SAVE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.items").exists());
    }

    @Test
    @DisplayName("POST /builds/replacements không cần token (công khai như /auto, /validate)")
    void replacements_isPublic() throws Exception {
        mockMvc.perform(post("/api/pc-build/builds/replacements").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \" \", \"items\": []}"))
                .andExpect(status().isBadRequest());
    }
}
