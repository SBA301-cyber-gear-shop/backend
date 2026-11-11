package com.project.cybershop.order.api.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record PlaceOrderRequest(
        @NotBlank(message = "Vui lòng nhập tên người nhận")
        @Size(max=100, message = "Tên người nhận tối đa 100 ký tự") String pickupName,
        @NotBlank(message = "Vui lòng nhập số điện thoại người nhận")
        @Size(max=20, message = "Số điện thoại người nhận tối đa 20 ký tự") String pickupPhone,
        @Size(max=2000, message = "Ghi chú tối đa 2000 ký tự") String note,
        @Size(max=50, message = "Mã khuyến mãi tối đa 50 ký tự") String promotionCode,
        @NotBlank(message = "Vui lòng chọn phương thức thanh toán") String method,
        Integer paymentMethodId,
        @NotNull(message = "Thiếu tổng tiền dự kiến")
        @DecimalMin(value = "0", message = "Tổng tiền dự kiến không được âm") BigDecimal expectedTotal,
        @NotNull(message = "Thiếu số tiền giảm dự kiến")
        @DecimalMin(value = "0", message = "Số tiền giảm dự kiến không được âm") BigDecimal expectedDiscount,
        List<Integer> cartItemIds,
        List<Integer> itemVariantIds,
        String deliveryType,
        BigDecimal shippingFee,
        String shippingAddress,
        Integer toProvinceId,
        Integer toDistrictId,
        String toWardCode,
        List<PcGroup> pcGroups
) {
    public record PcGroup(List<PcPart> parts) {}
    public record PcPart(Integer variantId, Integer quantity, String role) {}
    public PlaceOrderRequest(String pickupName, String pickupPhone, String note, String promotionCode,
                            String method, Integer paymentMethodId, BigDecimal expectedTotal, BigDecimal expectedDiscount) {
        this(pickupName, pickupPhone, note, promotionCode, method, paymentMethodId, expectedTotal, expectedDiscount, null, null, "store", BigDecimal.ZERO, null, null, null, null, null);
    }

    public PlaceOrderRequest(String pickupName, String pickupPhone, String note, String promotionCode,
                            String method, Integer paymentMethodId, BigDecimal expectedTotal, BigDecimal expectedDiscount,
                            List<Integer> itemVariantIds) {
        this(pickupName, pickupPhone, note, promotionCode, method, paymentMethodId, expectedTotal, expectedDiscount, null, itemVariantIds, "store", BigDecimal.ZERO, null, null, null, null, null);
    }

    public PlaceOrderRequest(String pickupName, String pickupPhone, String note, String promotionCode,
                            String method, Integer paymentMethodId, BigDecimal expectedTotal, BigDecimal expectedDiscount,
                            List<Integer> cartItemIds, List<Integer> itemVariantIds) {
        this(pickupName, pickupPhone, note, promotionCode, method, paymentMethodId, expectedTotal, expectedDiscount, cartItemIds, itemVariantIds, "store", BigDecimal.ZERO, null, null, null, null, null);
    }
}
