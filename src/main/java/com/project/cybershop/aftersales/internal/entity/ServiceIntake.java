package com.project.cybershop.aftersales.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một lần khách mang máy tới quầy, gắn với một đơn hàng. Mỗi linh kiện của lần này là một phiếu con
 * (ServiceRequest có intakeId trỏ về đây); thông tin tiếp nhận nằm ở từng phiếu con.
 */
@Entity
@Table(name = "service_intakes")
@Getter
@Setter
@NoArgsConstructor
public class ServiceIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "intake_code", nullable = false, length = 50, unique = true)
    private String intakeCode;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
