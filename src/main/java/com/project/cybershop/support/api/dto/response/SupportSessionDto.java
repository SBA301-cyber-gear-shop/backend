package com.project.cybershop.support.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportSessionDto {
    private Long id;
    private String conversationId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String status; // "WAITING", "ACTIVE", "CLOSED"
    private Integer assignedStaffId;
    private String assignedStaffName;
    private String lastMessage;
    private String createdAt;
    private String updatedAt;
    private int unreadCount;
}
