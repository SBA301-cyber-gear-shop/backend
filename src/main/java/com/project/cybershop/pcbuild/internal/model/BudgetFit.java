package com.project.cybershop.pcbuild.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Kết quả AI đánh giá mức độ phù hợp giữa ngân sách và kỳ vọng hiệu năng ở lần gọi thứ hai. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetFit {
    private boolean budgetFit;
    private String explanation;
    private Long suggestedBudget;
    private String achievableExpectation;
}

