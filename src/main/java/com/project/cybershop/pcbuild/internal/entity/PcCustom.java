package com.project.cybershop.pcbuild.internal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "pc_customs")
public class PcCustom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "order_id")
    private Integer orderId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ColumnDefault("0")
    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Builder.Default
    @OneToMany(mappedBy = "pcCustom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PcCustomItem> items = new ArrayList<>();
}
