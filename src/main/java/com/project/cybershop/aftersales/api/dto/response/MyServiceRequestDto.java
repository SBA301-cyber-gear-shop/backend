package com.project.cybershop.aftersales.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Phiếu bảo hành dưới góc nhìn của khách: không có ghi chú nội bộ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyServiceRequestDto {
    private Integer id;
    private String requestCode;
    private String intakeCode;
    private String productName;
    private Integer pcCustomId;
    private String serialNumber;
    private boolean underWarrantyAtIntake;
    private LocalDateTime warrantyStartDate;
    private LocalDateTime warrantyEndDate;
    private Long warrantyRemainingDays;
    private LocalDate warrantyRemainingAsOf;
    private String replacementSerialNumber;
    private LocalDateTime replacementWarrantyEndDate;
    private Long replacementWarrantyRemainingDays;
    private String status;
    private String resolution;
    private String issueDescription;
    private String publicNote;
    private LocalDate expectedReturnDate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<MyTimelineEntryDto> timeline;
}
