package com.project.cybershop.catalog.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "serial_code", nullable = false, unique = true, length = 100)
    private String serialCode; // SKU của phiên bản

    @Column(name = "variant_name", length = 150)
    private String variantName;

    @Column(name = "slug", unique = true, length = 150)
    private String slug;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @org.hibernate.annotations.BatchSize(size = 100)
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpecificationValue> attributeValues = new ArrayList<>();

    public ProductVariant(Product product, String serialCode, String variantName, String slug,
                          BigDecimal price, BigDecimal originalPrice, Integer stockQuantity, String imageUrl) {
        this.product = product;
        this.serialCode = serialCode;
        this.variantName = variantName;
        this.slug = slug;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
        this.imageUrl = imageUrl;
    }

    public void addAttributeValue(SpecificationValue value) {
        attributeValues.add(value);
        value.setProductVariant(this);
    }
}
