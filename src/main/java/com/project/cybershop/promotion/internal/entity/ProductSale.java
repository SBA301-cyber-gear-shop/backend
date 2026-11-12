package com.project.cybershop.promotion.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "product_sales") @Getter @Setter @NoArgsConstructor
public class ProductSale {
    @Id @Column(name = "product_id") private Integer productId;
    @Column(precision = 5, scale = 2) private BigDecimal percentage;
    @Column(name = "discount_type", nullable = false, length = 30) private String discountType = "percentage";
    @Column(name = "discount_value", precision = 12, scale = 2) private BigDecimal discountValue;
    @Column(name = "start_date", nullable = false) private LocalDateTime start;
    @Column(name = "end_date", nullable = false) private LocalDateTime end;
    @Column(nullable = false) private boolean enabled;
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "variant_ids") private Integer[] variantIds;
}
