package com.project.cybershop.aftersales.internal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "request_code", nullable = false, length = 30)
    private String requestCode;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "warranty_card_id", nullable = false)
    private Integer warrantyCardId;

    /** Lần tiếp nhận chứa phiếu này (ServiceIntake). */
    @Column(name = "intake_id", nullable = false)
    private Integer intakeId;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "issue_description", nullable = false, columnDefinition = "TEXT")
    private String issueDescription;

    @Column(name = "condition_note", columnDefinition = "TEXT")
    private String conditionNote;

    @Column(name = "accessories_note", columnDefinition = "TEXT")
    private String accessoriesNote;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Convert(converter = ServiceRequestStatus.JpaConverter.class)
    @Column(name = "status", nullable = false, length = 50)
    private ServiceRequestStatus status;

    @Convert(converter = Resolution.JpaConverter.class)
    @Column(name = "resolution", length = 30)
    private Resolution resolution;

    @Column(name = "assigned_technician_id")
    private Integer assignedTechnicianId;

    @Column(name = "technician_note", columnDefinition = "TEXT")
    private String technicianNote;

    @Column(name = "public_note", columnDefinition = "TEXT")
    private String publicNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
