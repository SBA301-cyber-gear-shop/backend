package com.project.cybershop.catalog.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"Category_Specifications\"", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cat_spec", columnNames = {"category_id", "specification_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class CategorySpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specification_id", nullable = false)
    private Specification specification;

    @Column(name = "is_filterable")
    private Boolean isFilterable = false;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "show_in_menu")
    private Boolean showInMenu = false;

    public CategorySpecification(Category category, Specification specification, Boolean isFilterable, Boolean isPrimary, Boolean showInMenu) {
        this.category = category;
        this.specification = specification;
        this.isFilterable = isFilterable != null ? isFilterable : false;
        this.isPrimary = isPrimary != null ? isPrimary : false;
        this.showInMenu = showInMenu != null ? showInMenu : false;
    }
}
