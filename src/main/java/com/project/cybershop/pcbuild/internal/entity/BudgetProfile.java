package com.project.cybershop.pcbuild.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Cấu hình ngân sách theo mục đích: phần trăm ngân sách của một slot và ngân sách tối thiểu của mục đích.
 * min_budget là thuộc tính của purpose nên lặp lại giống nhau ở mọi dòng cùng purpose.
 */
@Entity
@Table(name = "budget_profiles")
@Getter
@Setter
@NoArgsConstructor
public class BudgetProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private BuildTemplateSlot slot;

    @Column(name = "share_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal sharePct;

    @Column(name = "min_budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal minBudget;
}
