package com.project.cybershop.support.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportPollResponse {
    private String status; // "WAITING", "ACTIVE", "CLOSED"
    private String assignedStaffName;
    private List<SupportMessageDto> messages;
}
