package com.project.cybershop.pcbuild.api.dto.response;

/** Kết quả bước 3 (code kiểm tra yêu cầu). */
public enum RequirementStatus {
    /** Đạt mức tối thiểu của mục đích: đi tiếp bước 4. */
    READY,
    /** Ngân sách thấp hơn min_budget của mục đích: hiện popup gợi ý mục đích vừa tiền. */
    BELOW_MIN_BUDGET,
    /** Ngân sách thấp hơn mọi min_budget: báo shop chưa có bộ PC nào ở mức này. */
    NO_AFFORDABLE_PURPOSE,
    /** Ngân sách vượt giá bộ PC cao cấp nhất ráp được từ kho. */
    BUDGET_TOO_HIGH
}
