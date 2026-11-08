package com.project.cybershop.support.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessageDto {
    private Long id;
    private String conversationId;
    private String senderType; // "CUSTOMER", "STAFF", "BOT"
    private String senderName;
    private String content;
    private String createdAt;
}
