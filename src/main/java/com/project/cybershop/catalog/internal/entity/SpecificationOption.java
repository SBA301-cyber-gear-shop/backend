package com.project.cybershop.catalog.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "specification_options", uniqueConstraints = {
    @UniqueConstraint(name = "uk_spec_opt_code", columnNames = {"specification_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
public class SpecificationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specification_id", nullable = false)
    private Specification specification;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "display_value", nullable = false, length = 100)
    private String displayValue;

    public SpecificationOption(Specification specification, String code, String displayValue) {
        this.specification = specification;
        this.code = code;
        this.displayValue = displayValue;
    }
}
