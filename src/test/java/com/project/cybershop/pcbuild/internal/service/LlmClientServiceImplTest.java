package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LlmClientServiceImpl - Kiểm thử tích hợp AI Client & Xử lý lỗi không dùng Mockdata")
class LlmClientServiceImplTest {

    private LlmClientServiceImpl llmClientService;

    @BeforeEach
    void setUp() {
        llmClientService = new LlmClientServiceImpl(new ObjectMapper());
    }

    @Test
    @DisplayName("Khi không cấu hình API Key (null hoặc blank) -> Ném BusinessException thông báo rõ ràng")
    void shouldThrowBusinessExceptionWhenApiKeyIsMissing() {
        ReflectionTestUtils.setField(llmClientService, "apiKey", "");

        assertThatThrownBy(() -> llmClientService.generateBuildProposal("System prompt", "{}"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chưa cấu hình API Key");
    }

    @Test
    @DisplayName("Khi API Key là null -> Ném BusinessException yêu cầu cấu hình")
    void shouldThrowBusinessExceptionWhenApiKeyIsNull() {
        ReflectionTestUtils.setField(llmClientService, "apiKey", null);

        assertThatThrownBy(() -> llmClientService.generateBuildProposal("System prompt", "{}"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chưa cấu hình API Key");
    }
}
