package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một luật bị vi phạm, ví dụ {"rule": "RULE_7", "message": "..."}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildViolationResponse {
    private String rule;
    private String message;
}
