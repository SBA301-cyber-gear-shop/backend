package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Bước 9: khách bấm Lưu. Giá không lấy từ request mà đọc lại từ DB lúc lưu.
 *
 * @param confirmOverBudget true khi khách đã bấm "Tiếp tục" ở popup vượt ngân sách (bước 7); null coi như false
 */
public record SaveBuildRequest(
        @NotNull(message = "Ngân sách không được để trống")
        @Positive(message = "Ngân sách phải lớn hơn 0")
        @Max(value = 9_999_999_999L, message = "Ngân sách tối đa là 9.999.999.999đ")
        Long budget,

        @NotEmpty(message = "Bộ PC phải có ít nhất một món")
        @Size(max = 20, message = "Bộ PC tối đa 20 dòng")
        List<@Valid @NotNull BuildItemRequest> items,

        Boolean confirmOverBudget
) {
}
