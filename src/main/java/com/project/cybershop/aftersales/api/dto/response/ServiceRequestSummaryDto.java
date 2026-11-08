package com.project.cybershop.aftersales.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestSummaryDto {
    private Integer id;
    private Long version;
    private String requestCode;
    private Integer intakeId;
    private String intakeCode;
    private String status;
    private String resolution;
    private String issueDescription;
    private String contactName;
    private String contactPhone;
    private LocalDate expectedReturnDate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Integer warrantyCardId;
    private Integer pcCustomId;
    private boolean underWarrantyAtIntake;
    private String serialNumber;
    private LocalDateTime warrantyStartDate;
    private LocalDateTime warrantyEndDate;
    private Long warrantyRemainingDays;
    private LocalDate warrantyRemainingAsOf;
    private String replacementSerialNumber;
    private LocalDateTime replacementWarrantyEndDate;
    private Long replacementWarrantyRemainingDays;
    private String productName;
    private String orderCode;
}
