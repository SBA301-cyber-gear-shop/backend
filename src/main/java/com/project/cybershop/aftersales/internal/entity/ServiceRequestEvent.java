package com.project.cybershop.aftersales.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một dòng lịch sử của phiếu bảo hành. Chỉ thêm, không sửa, không xóa.
 */
@Entity
@Table(name = "\"Service_request_events\"")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRequestEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "service_request_id", nullable = false)
    private Integer serviceRequestId;

    @Column(name = "actor_id", nullable = false)
    private Integer actorId;

    @Convert(converter = ServiceRequestStatus.JpaConverter.class)
    @Column(name = "from_status", length = 50)
    private ServiceRequestStatus fromStatus;

    @Convert(converter = ServiceRequestStatus.JpaConverter.class)
    @Column(name = "to_status", nullable = false, length = 50)
    private ServiceRequestStatus toStatus;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "public_note", columnDefinition = "TEXT")
    private String publicNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
