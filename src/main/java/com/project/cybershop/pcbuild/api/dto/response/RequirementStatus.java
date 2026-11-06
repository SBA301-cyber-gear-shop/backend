package com.project.cybershop.pcbuild.api.dto.response;

/** Kết quả bước 3 (code kiểm tra yêu cầu). */
public enum RequirementStatus {
    /** Đủ thông tin và đạt mức tối thiểu của mục đích: đi tiếp bước 4. */
    READY,
    /** Thiếu thông tin bắt buộc (hiện chỉ có budget): hỏi đúng trường còn thiếu. */
    NEED_MORE_INFO,
    /** Đã hỏi lại đủ số lần, hoặc AI không phản hồi: chuyển khách sang form nhập ngân sách và hãng. */
    FORM_REQUIRED,
    /** Ngân sách thấp hơn min_budget của mục đích: hiện popup gợi ý mục đích vừa tiền. */
    BELOW_MIN_BUDGET,
    /** Ngân sách thấp hơn mọi min_budget: báo shop chưa có bộ PC nào ở mức này. */
    NO_AFFORDABLE_PURPOSE,
    /** Ngân sách vượt giá bộ PC cao cấp nhất ráp được từ kho. */
    BUDGET_TOO_HIGH
}
