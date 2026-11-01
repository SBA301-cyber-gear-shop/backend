package com.project.cybershop.order.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record HandoverRequest(
        @NotBlank(message = "Vui lòng nhập số điện thoại người nhận")
        @Size(max=20, message = "Số điện thoại người nhận tối đa 20 ký tự") String recipientPhone,
        @AssertTrue(message = "Vui lòng xác nhận đã đối chiếu người nhận và thiết bị") boolean recipientVerified,
        @NotNull(message = "Thiếu danh sách thiết bị bàn giao")
        @Size(max=10000, message = "Danh sách thiết bị bàn giao quá dài")
        List<@NotNull(message = "Thông tin thiết bị không được để trống") @Valid Device> devices
) {
    public record Device(
            @NotNull(message = "Thiếu mã dòng đơn hàng của thiết bị") Integer orderItemId,
            @NotBlank(message = "Vui lòng nhập serial cho từng thiết bị")
            @Size(max=100, message = "Serial tối đa 100 ký tự") String serialNumber) {}
}
