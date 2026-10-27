package com.project.cybershop.aftersales.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Một lần gửi bảo hành dưới góc nhìn của khách: trạng thái chung của cả bộ máy và kết quả từng linh kiện,
 * không có ghi chú nội bộ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyServiceIntakeDto {

    private String intakeCode;
    private String orderCode;
    private String status;
    private String issueDescription;
    private LocalDate expectedReturnDate;
    private LocalDateTime createdAt;
    private LocalDateTime returnedAt;
    private List<MyServiceRequestDto> components;
}
