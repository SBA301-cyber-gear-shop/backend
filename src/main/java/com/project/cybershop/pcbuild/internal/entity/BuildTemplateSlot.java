package com.project.cybershop.pcbuild.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Khuôn bộ PC (metadata): mỗi dòng là một loại linh kiện, thứ tự chọn và số lượng cho phép. */
@Entity
@Table(name = "build_template_slots")
@Getter
@Setter
@NoArgsConstructor
public class BuildTemplateSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "is_required", nullable = false)
    private Boolean required;

    @Column(name = "min_qty", nullable = false)
    private Integer minQty;

    @Column(name = "max_qty", nullable = false)
    private Integer maxQty;

    @Column(name = "required_if_category_id")
    private Integer requiredIfCategoryId;

    @Column(name = "required_if_spec_id")
    private Integer requiredIfSpecId;

    @Column(name = "required_if_value", length = 100)
    private String requiredIfValue;
}
