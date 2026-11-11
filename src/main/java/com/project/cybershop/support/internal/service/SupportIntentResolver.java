package com.project.cybershop.support.internal.service;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.CustomerContext;
import com.project.cybershop.support.internal.model.SupportClassifiedIntent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface SupportIntentResolver {

    SupportClassifiedIntent classifyHybridIntent(String message, List<AiChatRequest.HistoryItem> history);

    Set<String> extractTargetCategories(String text);

    List<String> extractComponentLines(String text);

    boolean isProductQuery(String message);

    boolean isReferralComponentQuery(String message);

    boolean isFollowUpPronounQuery(String message);

    String resolveContextualSearchQuery(String message, List<AiChatRequest.HistoryItem> history, String currentPath);

    boolean hasRecentPcBuildInHistory(List<AiChatRequest.HistoryItem> history);

    BigDecimal extractBudgetFromMessage(String msg);

    BigDecimal extractBudgetFromMessage(String msg, List<AiChatRequest.HistoryItem> history);

    BigDecimal extractExplicitBudget(String msg);

    boolean isPcUpgradeOrSurplusQuery(String message, List<AiChatRequest.HistoryItem> history);

    boolean isPcBuilderRequest(String message);

    boolean isPcBuilderRequest(String message, List<AiChatRequest.HistoryItem> history);

    AiChatResponse checkQuickNavigationIntent(
            String message,
            List<AiChatRequest.HistoryItem> history,
            CustomerContext ctx,
            String conversationId);

    AiChatResponse buildStaffWaitingResponse(String conversationId);

    AiChatResponse buildStaffConnectedResponse(String conversationId);

    AiChatResponse buildStaffEndedResponse(String conversationId);

    AiChatResponse handleStaffSessionMessage(String message, CustomerContext ctx, String conversationId);
}
