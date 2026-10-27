package com.project.cybershop.aftersales.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntakeCreateRequest {

    /** Các linh kiện còn bảo hành, mỗi linh kiện thành một phiếu con. Tất cả phải cùng một đơn hàng. */
    @NotEmpty(message = "Chưa chọn linh kiện nào để tiếp nhận")
    private List<Integer> warrantyCardIds;

    /** Linh kiện hết hạn khách vẫn để lại cùng bộ máy: ghi vào phụ kiện nhận giữ, không tạo phiếu. */
    private List<Integer> outOfWarrantyCardIds;

    @NotBlank(message = "Mô tả lỗi không được để trống")
    private String issueDescription;

    @NotBlank(message = "Tình trạng ngoại quan không được để trống")
    private String conditionNote;

    @NotBlank(message = "Phụ kiện nhận giữ không được để trống")
    private String accessoriesNote;

    @NotBlank(message = "Tên người mang máy không được để trống")
    @Size(max = 100, message = "Tên người mang máy tối đa 100 ký tự")
    private String contactName;

    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải có 10 chữ số bắt đầu bằng 0")
    private String contactPhone;

    @NotNull(message = "Ngày hẹn trả không được để trống")
    private LocalDate expectedReturnDate;
}
