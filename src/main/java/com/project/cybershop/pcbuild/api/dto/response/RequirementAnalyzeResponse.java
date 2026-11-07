package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementAnalyzeResponse {
    private RequirementStatus status;
    private String message;
    private RequirementSummary requirement;
    private Long minBudget;
    private Long maxBuildPrice;
    @Builder.Default
    private List<PurposeOptionResponse> suggestedPurposes = new ArrayList<>();
    private String explanation;
    private Long suggestedBudget;
    @Builder.Default
    private List<String> notes = new ArrayList<>();
}

