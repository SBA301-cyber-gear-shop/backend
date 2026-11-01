package com.project.cybershop.promotion.api.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
        @NotBlank(message = "Vui lòng nhập mã khuyến mãi")
        @Size(max=50, message = "Mã khuyến mãi tối đa 50 ký tự") String code,
        @NotBlank(message = "Vui lòng nhập tên chương trình")
        @Size(max=150, message = "Tên chương trình tối đa 150 ký tự") String name,
        @NotBlank(message = "Vui lòng chọn loại giảm giá") String type,
        @NotNull(message = "Vui lòng nhập giá trị giảm")
        @DecimalMin(value="0",inclusive=false, message = "Giá trị giảm phải lớn hơn 0")
        @Digits(integer=10,fraction=2, message = "Giá trị giảm tối đa 10 chữ số và 2 số lẻ") BigDecimal value,
        @NotNull(message = "Vui lòng nhập số lượt dùng")
        @Min(value = 1, message = "Số lượt dùng phải từ 1 trở lên") Integer limit,
        @NotNull(message = "Vui lòng chọn ngày bắt đầu") LocalDateTime start,
        @NotNull(message = "Vui lòng chọn ngày kết thúc") LocalDateTime end,
        boolean active,
        @Size(max=150, message = "Tiêu đề banner tối đa 150 ký tự") String bannerTitle,
        @Size(max=255, message = "Đường dẫn ảnh banner tối đa 255 ký tự") String bannerImage,
        @Size(max=255, message = "Liên kết banner tối đa 255 ký tự") String bannerLink,
        @Min(value = 0, message = "Thứ tự banner không được âm") int bannerOrder, boolean bannerActive
) {}
