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

/** Một món của bộ PC đã lưu; unit_price là giá tại thời điểm lưu (docx 3.5), sau này gắn serial để làm bảo hành. */
@Entity
@Table(name = "pc_custom_items")
@Getter
@Setter
@NoArgsConstructor
public class PcCustomItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pc_custom_id", nullable = false)
    private PcCustom pcCustom;

    /** product_variants.id; bảng của module catalog nên chỉ lưu id. */
    @Column(name = "product_variant_id", nullable = false)
    private Integer productVariantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Mã slot của khuôn: MONITOR, CPU, RAM... */
    @Column(name = "component_role", nullable = false, length = 50)
    private String componentRole;
}
