package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityCheckResponse {
    private String key;
    private String label;
    private String status; // pass, fail, warning
    private String detail;
    private Boolean valid;

    public CompatibilityCheckResponse(String label, String status, String detail) {
        this.label = label;
        this.status = status;
        this.detail = detail;
        this.valid = "pass".equalsIgnoreCase(status);
    }

    public CompatibilityCheckResponse(String key, String label, boolean valid, String detail) {
        this.key = key;
        this.label = label;
        this.valid = valid;
        this.status = valid ? "pass" : "fail";
        this.detail = detail;
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(this.valid) || "pass".equalsIgnoreCase(this.status);
    }
}
