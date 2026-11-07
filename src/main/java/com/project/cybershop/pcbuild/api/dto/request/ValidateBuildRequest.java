package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Kiểm tra lại bộ PC sau khi khách tự đổi món (bước 9 gọi lại bước 6). */
public record ValidateBuildRequest(
        @NotNull(message = "Ngân sách không được để trống")
        @Positive(message = "Ngân sách phải lớn hơn 0")
        @Max(value = 9_999_999_999L, message = "Ngân sách tối đa là 9.999.999.999đ")
        Long budget,

        @NotEmpty(message = "Bộ PC phải có ít nhất một món")
        @Size(max = 20, message = "Bộ PC tối đa 20 dòng")
        List<@Valid @NotNull BuildItemRequest> items
) {
}
