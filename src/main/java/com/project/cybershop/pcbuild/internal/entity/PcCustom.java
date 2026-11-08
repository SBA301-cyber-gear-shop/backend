package com.project.cybershop.pcbuild.internal.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Bộ PC khách đã lưu (docx 3.5). user_id và order_id chỉ là số: users và orders thuộc module khác nên không map entity.
 * Ngân sách, mục đích, lời giải thích chưa có cột (schema đã chốt) nên không lưu.
 */
@Entity
@Table(name = "pc_customs")
@Getter
@Setter
@NoArgsConstructor
public class PcCustom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "pcCustom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PcCustomItem> items = new ArrayList<>();

    public void addItem(PcCustomItem item) {
        item.setPcCustom(this);
        items.add(item);
    }
}
