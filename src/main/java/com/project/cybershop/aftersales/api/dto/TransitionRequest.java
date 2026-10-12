package com.project.cybershop.aftersales.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionRequest {

    @NotBlank(message = "Chưa chọn trạng thái mới")
    private String toStatus;

    /** Bắt buộc khi chuyển sang ready_for_return, để trống ở các bước khác. */
    private String resolution;

    @NotBlank(message = "Ghi chú nội bộ không được để trống")
    private String internalNote;

    private String publicNote;

    private LocalDate expectedReturnDate;

    /** Số phiên bản phiếu mà người dùng đang xem, để phát hiện phiếu đã bị người khác sửa. */
    private Long version;
}
