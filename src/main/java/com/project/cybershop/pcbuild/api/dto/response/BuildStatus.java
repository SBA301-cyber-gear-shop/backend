package com.project.cybershop.pcbuild.api.dto.response;

/** Trạng thái bộ PC sau bước 6: xung đột phần cứng và vượt tiền luôn tách riêng. */
public enum BuildStatus {
    VALID,
    INCOMPATIBLE,
    OVER_BUDGET
}
