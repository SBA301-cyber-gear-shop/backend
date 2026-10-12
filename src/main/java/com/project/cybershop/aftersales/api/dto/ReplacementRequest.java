package com.project.cybershop.aftersales.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplacementRequest {

    @NotBlank(message = "Serial thiết bị thay thế không được để trống")
    @Size(max = 100, message = "Serial tối đa 100 ký tự")
    private String newSerialNumber;

    /** stock: lấy từ hàng đang bán. supplier: hãng gửi về. */
    @NotBlank(message = "Chưa chọn nguồn thiết bị thay thế")
    @Pattern(regexp = "stock|supplier", message = "Nguồn thiết bị phải là stock hoặc supplier")
    private String source;

    @NotBlank(message = "Ghi chú nội bộ không được để trống")
    private String internalNote;

    @NotBlank(message = "Nội dung thông báo cho khách không được để trống")
    private String publicNote;
}
