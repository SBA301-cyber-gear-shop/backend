package com.project.cybershop.catalog.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "specification_values", uniqueConstraints = {
    @UniqueConstraint(name = "uk_variant_spec", columnNames = {"product_variant_id", "specification_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class SpecificationValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specification_id", nullable = false)
    private Specification specification;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    public SpecificationValue(ProductVariant productVariant, Specification specification, String value) {
        this.productVariant = productVariant;
        this.specification = specification;
        this.value = value;
    }
}
