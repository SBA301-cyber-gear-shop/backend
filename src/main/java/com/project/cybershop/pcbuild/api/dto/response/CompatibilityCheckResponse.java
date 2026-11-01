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
    private String key;    // mã check ổn định: R1..R12, J1..J11, CORE, ROLE, DUP, STOCK
    private String label;
    private String status; // pass, warning, unknown, fail
    private String detail;
    private Boolean valid;

    public CompatibilityCheckResponse(String label, String status, String detail) {
        this.label = label;
        this.status = status;
        this.detail = detail;
        this.valid = isValidStatus(status);
    }

    public CompatibilityCheckResponse(String key, String label, boolean valid, String detail) {
        this.key = key;
        this.label = label;
        this.valid = valid;
        this.status = valid ? "pass" : "fail";
        this.detail = detail;
    }

    public static CompatibilityCheckResponse of(String key, String label, String status, String detail) {
        return new CompatibilityCheckResponse(key, label, status, detail, isValidStatus(status));
    }

    // DD 7.2: chỉ fail mới là không hợp lệ; warning và unknown vẫn lắp được
    public boolean isValid() {
        return status != null ? isValidStatus(status) : Boolean.TRUE.equals(valid);
    }

    private static boolean isValidStatus(String status) {
        return !"fail".equalsIgnoreCase(status);
    }
}
