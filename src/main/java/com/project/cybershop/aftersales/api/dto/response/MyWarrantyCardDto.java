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
public class MyWarrantyCardDto {
    private Integer id;
    private String serialNumber;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private boolean valid;
    private String productName;
    private String variantName;
    private String orderCode;
    private String replacedBySerialNumber;
    private List<MyServiceRequestDto> requests;
}
