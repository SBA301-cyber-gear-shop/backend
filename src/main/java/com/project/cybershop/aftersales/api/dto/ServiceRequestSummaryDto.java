package com.project.cybershop.aftersales.api.dto;

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
    private String requestCode;
    private String status;
    private String resolution;
    private String issueDescription;
    private String contactName;
    private String contactPhone;
    private LocalDate expectedReturnDate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Integer warrantyCardId;
    private String serialNumber;
    private String productName;
}
