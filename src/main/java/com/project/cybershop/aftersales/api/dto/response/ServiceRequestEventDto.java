package com.project.cybershop.aftersales.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestEventDto {
    private Integer id;
    private String fromStatus;
    private String toStatus;
    private Integer actorId;
    private String actorName;
    private String internalNote;
    private String publicNote;
    private LocalDateTime createdAt;
}
