package com.project.cybershop.shipping.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Integer orderId;

    @Column(name = "carrier", nullable = false, length = 50)
    private String carrier = "GHN";

    @Column(name = "tracking_code", nullable = false, unique = true, length = 100)
    private String trackingCode;

    @Column(name = "service_id")
    private Integer serviceId = 53320;

    @Column(name = "cod_amount", precision = 12, scale = 2)
    private BigDecimal codAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "weight_gram")
    private Integer weightGram = 1000;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ready_to_pick";

    @Column(name = "status_text", length = 255)
    private String statusText = "Chờ lấy hàng";

    @Column(name = "expected_delivery_time")
    private LocalDateTime expectedDeliveryTime;

    @Column(name = "print_token", length = 255)
    private String printToken;

    @Column(name = "tracking_logs", columnDefinition = "text")
    private String trackingLogs;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
