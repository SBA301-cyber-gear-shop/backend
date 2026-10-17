package com.project.cybershop.pcbuild.internal.service;

import lombok.Data;

import java.util.List;

public interface LlmClientService {

    @Data
    class AiDraftProposal {
        private String buildName;
        private String aiReasoning;
        private String performanceHighlight;
        private List<PartSelection> selectedParts;

        @Data
        public static class PartSelection {
            private String role;
            private Integer variantId;
            private Integer quantity = 1;
            private String reason;
        }
    }

    AiDraftProposal generateBuildProposal(String systemPrompt, String userContextJson);
}
