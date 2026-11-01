package com.project.cybershop.review.api.dto.request;

import jakarta.validation.constraints.*;
public record ReviewRequest(
        @NotNull(message = "Vui lòng chọn số sao")
        @Min(value = 1, message = "Số sao phải từ 1 đến 5")
        @Max(value = 5, message = "Số sao phải từ 1 đến 5") Integer rating,
        @NotBlank(message = "Vui lòng nhập nội dung đánh giá")
        @Size(max=5000, message = "Nội dung đánh giá tối đa 5000 ký tự") String comment) {}
