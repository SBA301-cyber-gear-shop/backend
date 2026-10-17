package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LlmClientServiceImpl - Kiểm thử tích hợp AI Client & Mock Fallback")
class LlmClientServiceImplTest {

    private LlmClientServiceImpl llmClientService;

    @BeforeEach
    void setUp() {
        llmClientService = new LlmClientServiceImpl(new ObjectMapper());
    }

    @Test
    @DisplayName("Khi không cấu hình API Key (null hoặc blank) -> Tự động gọi Mock Proposal an toàn")
    void shouldReturnMockProposalWhenApiKeyIsMissing() {
        ReflectionTestUtils.setField(llmClientService, "apiKey", "");

        var proposal = llmClientService.generateBuildProposal("System prompt", "{}");

        assertThat(proposal).isNotNull();
        assertThat(proposal.getBuildName()).contains("Bạch Hổ");
        assertThat(proposal.getSelectedParts()).hasSize(8);
        assertThat(proposal.getPerformanceHighlight()).isNotEmpty();
    }

    @Test
    @DisplayName("Kiểm tra tính toàn vẹn của Mock Proposal: Chứa đủ 8 vai trò linh kiện chính")
    void shouldContainAllEightRolesInMockProposal() {
        ReflectionTestUtils.setField(llmClientService, "apiKey", null);

        var proposal = llmClientService.generateBuildProposal("System prompt", "{}");

        List<String> roles = proposal.getSelectedParts().stream()
                .map(LlmClientService.AiDraftProposal.PartSelection::getRole)
                .toList();

        assertThat(roles).containsExactlyInAnyOrder(
                "cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler"
        );
    }
}
