package com.project.cybershop.support.internal.service;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.PcBuildProfile;

import java.math.BigDecimal;
import java.util.List;

public interface SupportPcBuildSolver {

    PcBuildProfile parsePcBuildProfile(String message, List<AiChatRequest.HistoryItem> history, long budgetVal);

    List<AiChatResponse.ProductSummary> buildRealPcComponentsForBudget(long budgetVal);

    List<AiChatResponse.ProductSummary> buildRealPcComponentsForBudget(
            long budgetVal,
            String userMessage,
            List<AiChatRequest.HistoryItem> history);

    AiChatResponse.ProductSummary findBestStepUpComponent(
            String slotKey,
            AiChatResponse.ProductSummary currentItem,
            double remainingBudget,
            String requiredSocket,
            String requiredRamType,
            PcBuildProfile profile,
            long totalBudgetVal);

    AiChatResponse formatSmartPcBuildAdvisory(String message, BigDecimal budget, String conversationId);

    AiChatResponse formatSmartPcBuildAdvisory(
            String message,
            BigDecimal budget,
            List<AiChatRequest.HistoryItem> history,
            String conversationId);
}
