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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private SpecificationOption option;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    public SpecificationValue(ProductVariant productVariant, Specification specification, String value) {
        this(productVariant, specification, null, value);
    }

    public SpecificationValue(ProductVariant productVariant, Specification specification, SpecificationOption option, String value) {
        this.productVariant = productVariant;
        this.specification = specification;
        this.option = option;
        this.value = value;
    }
}
