package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/** Thông tin vận hành của một lần /ai-suggest (DD 9.1). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiBuildMetadataResponse {
    private String requestId;
    private OffsetDateTime generatedAt;
    private String extractStatus;               // OK | CACHED | USER_PROFILE
    private String aiStatus;                    // OK | PARTIAL (bước giải thích)
    private String provider;
    private String model;
    private Map<String, String> promptVersions; // profile → PROFILE_V1, explain → EXPLAIN_V3
    private String dictionaryVersion;
    private Map<String, Long> latencyMs;        // profile, solver, explain, total
    private Integer candidateCount;
    private Integer solverIterations;
}
