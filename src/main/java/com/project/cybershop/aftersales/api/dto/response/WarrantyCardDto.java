package com.project.cybershop.aftersales.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyCardDto {
    private Integer id;
    private String serialNumber;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    /** Thẻ còn hiệu lực hôm nay: đang active và chưa quá ngày kết thúc. */
    private boolean valid;
    private String productName;
    private String variantName;
    private String sku;
    private String orderCode;
    private Integer orderId;
    /** Cấu hình PC (pc_customs) mà linh kiện này thuộc về; trống là mua lẻ. */
    private Integer pcCustomId;
    /** Thẻ khớp trực tiếp với từ khóa tra cứu; các thẻ khác cùng đơn được trả kèm để thấy cả bộ máy. */
    private boolean matched;
    private String customerName;
    private String customerPhone;
    private String pickupName;
    private String pickupPhone;
    /** Thẻ cũ mà thẻ này thay thế, nếu có. */
    private Integer replacesCardId;
    /** Thẻ mới đã thay thế thẻ này, nếu thiết bị đã được đổi. */
    private Integer replacedByCardId;
    private String replacedBySerialNumber;
    private long repairedCount;
    private ServiceRequestSummaryDto openRequest;
    /** Mọi phiếu của thẻ, mới nhất trước. */
    private List<ServiceRequestSummaryDto> requests;
}
