package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Khách đổi linh kiện của một bộ đã lưu ngay trong popup "Cấu hình đã lưu". pc_customs không lưu ngân sách nên
 * không có budget và không có bước xác nhận vượt ngân sách; giá đọc lại từ DB lúc lưu.
 */
public record UpdateSavedBuildRequest(
        @NotEmpty(message = "Bộ PC phải có ít nhất một món")
        @Size(max = 20, message = "Bộ PC tối đa 20 dòng")
        List<@Valid @NotNull BuildItemRequest> items
) {
}
