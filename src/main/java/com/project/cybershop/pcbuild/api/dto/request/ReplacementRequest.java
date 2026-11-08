package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Bước 9: lấy danh sách món tương thích cho dropdown của một loại linh kiện.
 *
 * @param category mã slot cần đổi: MONITOR, KEYBOARD, MOUSE, CPU, MAINBOARD, RAM, SSD, GPU, COOLER, CASE, PSU
 * @param items    bộ PC khách đang có (có thể chưa có món nào của category, ví dụ thêm GPU cho bộ văn phòng)
 */
public record ReplacementRequest(
        @NotBlank(message = "Loại linh kiện không được để trống")
        @Size(max = 30, message = "Loại linh kiện tối đa 30 ký tự")
        String category,

        @NotNull(message = "Danh sách món không được để trống")
        @Size(max = 20, message = "Bộ PC tối đa 20 dòng")
        List<@Valid @NotNull BuildItemRequest> items
) {
}
