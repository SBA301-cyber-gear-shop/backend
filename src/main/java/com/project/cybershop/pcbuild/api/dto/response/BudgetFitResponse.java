package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Độ sát ngân sách của bộ máy (DD 9.1, 9.2). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetFitResponse {
    private BigDecimal ratio;      // total / budget, 4 chữ số thập phân
    private BigDecimal percent;    // ratio × 100, 1 chữ số thập phân
    private String status;         // IN_WINDOW, UNDER_WINDOW (/validate), OVER (/validate); các mã UNDER_* của solver ở giai đoạn 2b
    private BigDecimal windowMin;  // 0,90 × budget
    private BigDecimal windowMax;  // = budget
    private BigDecimal leftover;   // budget - total (âm khi OVER)
}
