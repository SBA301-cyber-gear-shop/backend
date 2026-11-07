package com.project.cybershop.support.internal.service;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.SupportMessageDto;
import com.project.cybershop.support.api.dto.response.SupportSessionDto;

import java.util.List;

public interface StaffLiveSupportService {
    List<SupportSessionDto> getSessions(String statusFilter);
    SupportSessionDto getSession(String conversationId);
    List<SupportMessageDto> getMessages(String conversationId, Long afterId);
    SupportSessionDto assignStaff(String conversationId, String staffEmail);
    SupportMessageDto sendStaffMessage(String conversationId, String staffEmail, String content);
    SupportSessionDto closeSession(String conversationId, String staffEmail);
    void recordCustomerMessage(String conversationId, String customerEmail, String customerName, String content);
    void createOrOpenSession(String conversationId, String customerEmail, String customerName, String phone, String initialMessage);
    void createOrOpenSession(String conversationId, String customerEmail, String customerName, String phone, String initialMessage, List<AiChatRequest.HistoryItem> history);
}
