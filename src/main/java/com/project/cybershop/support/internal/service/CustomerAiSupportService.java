package com.project.cybershop.support.internal.service;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;

public interface CustomerAiSupportService {
    AiChatResponse generateResponse(String userEmailOrNull, AiChatRequest request);
}
