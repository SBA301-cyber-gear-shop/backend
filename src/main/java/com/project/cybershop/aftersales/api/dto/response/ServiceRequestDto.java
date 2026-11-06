package com.project.cybershop.aftersales.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDto {
    private Integer id;
    private String requestCode;
    /** Lần tiếp nhận chứa phiếu này; các phiếu cùng lần là các linh kiện khách mang tới cùng lúc. */
    private Integer intakeId;
    private String intakeCode;
    private String status;
    private String resolution;
    private String issueDescription;
    private String conditionNote;
    private String accessoriesNote;
    private String contactName;
    private String contactPhone;
    private LocalDate expectedReturnDate;
    private String technicianNote;
    private String publicNote;
    private Integer assignedTechnicianId;
    private String assignedTechnicianName;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long version;
    private WarrantyCardDto card;
    private boolean underWarrantyAtIntake;
    private List<ServiceRequestEventDto> events;
    private List<AllowedTransitionDto> allowedTransitions;
    private boolean canAddNote;
    private boolean canRelinkCard;
    private boolean canReplaceDevice;
    /** Còn hàng cùng phiên bản trong kho để lấy làm thiết bị thay thế. */
    private boolean replacementInStock;
}
