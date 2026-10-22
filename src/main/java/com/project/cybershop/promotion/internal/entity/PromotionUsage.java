package com.project.cybershop.promotion.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "promotion_usages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "promotion_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PromotionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "order_id", nullable = false, unique = true)
    private Integer orderId;

    @Column(name = "status")
    private String status;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;
}
