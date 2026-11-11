package com.project.cybershop.support.internal.service;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.ReconstructedBuildContext;

import java.util.List;

public interface SupportPcBuildAnalyzer {

    ReconstructedBuildContext reconstructAdvisedBuildFromHistory(
            List<AiChatRequest.HistoryItem> history,
            String fallbackMessage);

    boolean isPcBuildDeepDiveAnalysisQuery(
            String message,
            List<AiChatRequest.HistoryItem> history);

    AiChatResponse handlePcBuildDeepDiveAdvisory(
            String message,
            List<AiChatRequest.HistoryItem> history,
            String conversationId);

    AiChatResponse handlePcUpgradeOrSurplusAdvisory(
            String message,
            List<AiChatRequest.HistoryItem> history,
            String conversationId);
}
