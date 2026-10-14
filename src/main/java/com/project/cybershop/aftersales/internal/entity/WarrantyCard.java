package com.project.cybershop.aftersales.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "warranty_cards")
@Getter
@Setter
@NoArgsConstructor
public class WarrantyCard {

    public static final String ACTIVE = "active";
    public static final String VOID = "void";
    public static final String REPLACED = "replaced";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_item_id", nullable = false)
    private Integer orderItemId;

    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    // Hết hạn không lưu ở đây mà luôn tính từ endDate.
    @Column(name = "status", length = 50)
    private String status = ACTIVE;

    @Column(name = "replaces_card_id")
    private Integer replacesCardId;
}
