package com.project.cybershop.pcbuild.internal.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public interface LlmClientService {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class AiDraftProposal {
        @JsonAlias({"build_name", "title", "name"})
        private String buildName;

        @JsonAlias({"ai_reasoning", "reasoning", "description"})
        private String aiReasoning;

        @JsonAlias({"performance_highlight", "highlight"})
        private String performanceHighlight;

        @JsonAlias({"selected_parts", "parts", "items", "components"})
        private List<PartSelection> selectedParts = new ArrayList<>();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PartSelection {
            private String role;

            @JsonAlias({"variant_id", "id", "variantId"})
            private Integer variantId;

            private Integer quantity = 1;
            private String reason;
        }
    }

    AiDraftProposal generateBuildProposal(String systemPrompt, String userContextJson);
}
