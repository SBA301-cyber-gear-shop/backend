package com.project.cybershop.aftersales.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Một lần tiếp nhận (cả bộ máy) dưới góc nhìn nhân viên: thông tin chung lấy từ phiếu con đầu tiên,
 * trạng thái chung theo linh kiện đi chậm nhất, kèm các thao tác người gọi được làm ở cấp bộ máy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceIntakeDto {

    private Integer id;
    private String intakeCode;
    private Integer orderId;
    private String orderCode;
    private String customerName;
    private String status;
    private String contactName;
    private String contactPhone;
    private String issueDescription;
    private String conditionNote;
    private String accessoriesNote;
    private LocalDate expectedReturnDate;
    private LocalDateTime createdAt;
    private LocalDateTime returnedAt;
    private Long version;
    /** Các phiếu con, mỗi linh kiện một phiếu, theo thứ tự tiếp nhận. */
    private List<ServiceRequestSummaryDto> requests;
    private boolean canReturn;
    private boolean canCancel;
    private boolean canAddComponent;
}
