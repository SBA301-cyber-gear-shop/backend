package com.project.cybershop.promotion.internal.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "product_sale_prices") @Getter @Setter @NoArgsConstructor
public class ProductSalePrice {
    @Id @Column(name = "product_variant_id") private Integer variantId;
    @Column(name = "product_id", nullable = false) private Integer productId;
    @Column(name = "regular_price", nullable = false, precision = 12, scale = 2) private BigDecimal regularPrice;
    @Column(name = "original_price", precision = 12, scale = 2) private BigDecimal originalPrice;
    @Column(name = "applied_price", nullable = false, precision = 12, scale = 2) private BigDecimal appliedPrice;
    @Column(name = "applied_original_price", precision = 12, scale = 2) private BigDecimal appliedOriginalPrice;
}
