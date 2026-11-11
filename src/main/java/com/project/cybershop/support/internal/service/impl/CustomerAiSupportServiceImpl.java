package com.project.cybershop.support.internal.service.impl;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.api.dto.response.SupportSessionDto;
import com.project.cybershop.support.internal.model.CatalogSearchResult;
import com.project.cybershop.support.internal.model.CustomerContext;
import com.project.cybershop.support.internal.model.SupportClassifiedIntent;
import com.project.cybershop.support.internal.repository.SupportCatalogLookupRepository;
import com.project.cybershop.support.internal.repository.SupportCustomerContextRepository;
import com.project.cybershop.support.internal.service.CatalogVectorSyncService;
import com.project.cybershop.support.internal.service.CustomerAiSupportService;
import com.project.cybershop.support.internal.service.StaffLiveSupportService;
import com.project.cybershop.support.internal.service.SupportIntentResolver;
import com.project.cybershop.support.internal.service.SupportOrderAndPolicyAdvisor;
import com.project.cybershop.support.internal.service.SupportPcBuildAnalyzer;
import com.project.cybershop.support.internal.service.SupportPcBuildSolver;
import com.project.cybershop.support.internal.service.SupportProductAdvisor;
import com.project.cybershop.support.internal.support.SupportLlmClient;
import com.project.cybershop.support.internal.support.SupportSecurityGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrator điều phối luồng xử lý AI Chatbox & Chuyển giao Nhân viên (Staff Handover).
 * Tuân thủ kiến trúc phân tầng của Spring Modulith (tương tự PcBuildServiceImpl).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAiSupportServiceImpl implements CustomerAiSupportService {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    private final SupportCatalogLookupRepository catalogRepository;
    private final SupportCustomerContextRepository customerContextRepository;
    private final CatalogVectorSyncService catalogVectorSyncService;
    private final StaffLiveSupportService liveSupportService;
    private final SupportSecurityGuard securityGuard;
    private final SupportIntentResolver intentResolver;
    private final SupportPcBuildSolver pcBuildSolver;
    private final SupportPcBuildAnalyzer pcBuildAnalyzer;
    private final SupportProductAdvisor productAdvisor;
    private final SupportOrderAndPolicyAdvisor orderAndPolicyAdvisor;
    private final SupportLlmClient llmClient;

    @Override
    public AiChatResponse generateResponse(String userEmailOrNull, AiChatRequest request) {
        String message = request.getMessage().trim();
        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        CustomerContext customerCtx = customerContextRepository.fetchCustomerContext(userEmailOrNull);

        // 0.1. Chặn tấn công Prompt Injection, Jailbreak & Dò hỏi cấu trúc hệ thống (SEC-02)
        if (securityGuard.isSecurityViolationOrPromptInjection(message)) {
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message("Dạ, vì lý do bảo mật thông tin và an toàn hệ thống của TechZone, CyberBot không thể thực thi các câu lệnh can thiệp cấu hình, trích xuất mã nguồn hay dữ liệu nội bộ. Bạn có cần hỗ trợ tư vấn linh kiện PC hoặc tra cứu đơn hàng không ạ?")
                    .messageType("info")
                    .quickActions(List.of(
                            new AiChatResponse.QuickAction("Tư vấn cấu hình PC", "navigate", "/build-pc"),
                            new AiChatResponse.QuickAction("Xem linh kiện tại Shop", "navigate", "/san-pham"),
                            new AiChatResponse.QuickAction("Nhờ chuyên viên hỗ trợ", "connect_staff", null)
                    ))
                    .build();
        }

        // 0.2. Nếu đang trong phiên Live Chat với nhân viên hỗ trợ (Staff Mode hoặc phiên ACTIVE trên DB)
        SupportSessionDto currentSession = (liveSupportService != null) ? liveSupportService.getSession(conversationId) : null;
        boolean isDbActiveSession = currentSession != null && "ACTIVE".equalsIgnoreCase(currentSession.getStatus());
        boolean isDbWaitingSession = currentSession != null && "WAITING".equalsIgnoreCase(currentSession.getStatus());

        if (("staff".equalsIgnoreCase(request.getMode()) || isDbActiveSession) && !"connect_staff".equalsIgnoreCase(message)) {
            if (liveSupportService != null && !"end_staff_session".equalsIgnoreCase(message)) {
                liveSupportService.recordCustomerMessage(
                        conversationId,
                        userEmailOrNull,
                        customerCtx.isMember() ? customerCtx.fullName() : "Khách hàng",
                        message
                );
            }
            return intentResolver.handleStaffSessionMessage(message, customerCtx, conversationId);
        }

        // 1. Nhận lệnh kết nối trực tiếp hoặc kết thúc phiên hỗ trợ
        if ("connect_staff".equalsIgnoreCase(message)) {
            if (liveSupportService != null) {
                liveSupportService.createOrOpenSession(
                        conversationId,
                        userEmailOrNull,
                        customerCtx.isMember() ? customerCtx.fullName() : "Khách hàng",
                        customerCtx.phone(),
                        "Khách hàng bấm kết nối chuyên viên tư vấn",
                        request.getHistory()
                );
            }
            return intentResolver.buildStaffWaitingResponse(conversationId);
        }
        if ("end_staff_session".equalsIgnoreCase(message)) {
            if (liveSupportService != null) {
                liveSupportService.closeSession(conversationId, null);
            }
            return intentResolver.buildStaffEndedResponse(conversationId);
        }

        // 1.5. Nếu khách đang trong hàng chờ (WAITING) và nhắn thêm nội dung bổ sung
        if (isDbWaitingSession && liveSupportService != null) {
            liveSupportService.recordCustomerMessage(
                    conversationId,
                    userEmailOrNull,
                    customerCtx.isMember() ? customerCtx.fullName() : "Khách hàng",
                    message
            );
        }

        // 2. Kiểm tra yêu cầu gặp nhân viên trực tiếp
        if (securityGuard.isStaffRequest(message)) {
            if (liveSupportService != null) {
                liveSupportService.createOrOpenSession(
                        conversationId,
                        userEmailOrNull,
                        customerCtx.isMember() ? customerCtx.fullName() : "Khách hàng",
                        customerCtx.phone(),
                        message,
                        request.getHistory()
                );
            }
            return intentResolver.buildStaffWaitingResponse(conversationId);
        }

        // 2.1. Câu hỏi ngoài phạm vi nghiệp vụ tự động (VAT, mua sỉ B2B, khiếu nại...) -> ĐỀ XUẤT kết nối chuyên viên
        if (securityGuard.isOutOfScope(message)) {
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message("Dạ, yêu cầu này của bạn cần sự hỗ trợ và thẩm định trực tiếp từ chuyên viên TechZone. **Bạn có muốn mình kết nối bạn với Nhân viên tư vấn (Staff) đang trực tuyến không ạ?**")
                    .messageType("handover_proposal")
                    .quickActions(List.of(
                            new AiChatResponse.QuickAction("Kết nối nhân viên ngay", "connect_staff", null),
                            new AiChatResponse.QuickAction("Tiếp tục hỏi CyberBot", "ask", "Tiếp tục hỗ trợ")
                    ))
                    .build();
        }

        // 2.5. Chặn sớm câu hỏi ngoài ngữ cảnh cửa hàng máy tính (Off-Topic Guardrail)
        boolean hasStoreDomainIntent = intentResolver.isProductQuery(message)
                || intentResolver.isPcUpgradeOrSurplusQuery(message, request.getHistory())
                || pcBuildAnalyzer.isPcBuildDeepDiveAnalysisQuery(message, request.getHistory())
                || intentResolver.isPcBuilderRequest(message, request.getHistory())
                || !intentResolver.extractTargetCategories(message).isEmpty()
                || intentResolver.extractExplicitBudget(message) != null
                || intentResolver.isReferralComponentQuery(message)
                || intentResolver.isFollowUpPronounQuery(message);

        if (securityGuard.isOffTopicDomainQuery(message, hasStoreDomainIntent)) {
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message("Dạ, mình là **CyberBot** — Trợ lý chuyên trách về **Linh kiện Máy tính, Build PC, Đơn hàng & Bảo hành** tại **TechZone**. Câu hỏi này nằm ngoài phạm vi chuyên môn của mình rồi ạ!\n\nBạn có cần mình tư vấn cấu hình PC, tra cứu linh kiện trong kho hay kiểm tra đơn hàng / bảo hành gì không nhé?")
                    .messageType("info")
                    .quickActions(List.of(
                            new AiChatResponse.QuickAction("Tư vấn cấu hình PC", "navigate", "/build-pc"),
                            new AiChatResponse.QuickAction("Xem linh kiện tại Shop", "navigate", "/san-pham"),
                            new AiChatResponse.QuickAction("Kiểm tra bảo hành", "navigate", "/bao-hanh")
                    ))
                    .build();
        }

        // 2.6. Xử lý sớm ngữ cảnh hội thoại Build PC (Nâng cấp tiền dư / Phân tích sâu hiệu năng & linh kiện trong dàn / Đổi cấu hình)
        if (!intentResolver.isReferralComponentQuery(message)) {
            if (intentResolver.isPcUpgradeOrSurplusQuery(message, request.getHistory())) {
                return pcBuildAnalyzer.handlePcUpgradeOrSurplusAdvisory(message, request.getHistory(), conversationId);
            }
            if (pcBuildAnalyzer.isPcBuildDeepDiveAnalysisQuery(message, request.getHistory())) {
                return pcBuildAnalyzer.handlePcBuildDeepDiveAdvisory(message, request.getHistory(), conversationId);
            }
            if (intentResolver.isPcBuilderRequest(message, request.getHistory())) {
                BigDecimal budget = intentResolver.extractBudgetFromMessage(message, request.getHistory());
                return pcBuildSolver.formatSmartPcBuildAdvisory(message, budget, request.getHistory(), conversationId);
            }
        }

        // 3. Giải đại từ tham chiếu đơn lẻ (Follow-up Pronoun) & Mã đơn hàng động & Vector RAG
        String resolvedQuery = intentResolver.resolveContextualSearchQuery(message, request.getHistory(), request.getCurrentPath());
        String dynamicOrderContext = customerContextRepository.resolveDynamicOrderContext(message);
        String knowledgeContext = catalogRepository.searchVectorKnowledge(resolvedQuery);

        // 4. Xử lý ngữ cảnh nối tiếp (Referral Query / Add to Cart) hoặc Tìm kiếm sản phẩm
        boolean isReferral = intentResolver.isReferralComponentQuery(message);
        if (isReferral) {
            return handleReferralAndCartAction(message, request.getHistory(), conversationId);
        }

        List<AiChatResponse.ProductSummary> products = resolveCatalogProductsForQuery(resolvedQuery);
        CatalogSearchResult catalogResult = catalogRepository.searchCatalogProducts(resolvedQuery);
        String catalogTextContext = !products.isEmpty()
                ? catalogRepository.buildCatalogContextFromSummaries(products)
                : catalogResult.textContext();

        // 5. Kiểm tra điều hướng nhanh CHỈ KHI là câu xác nhận ngắn gọn
        AiChatResponse fastNavResponse = intentResolver.checkQuickNavigationIntent(message, request.getHistory(), customerCtx, conversationId);
        if (fastNavResponse != null) {
            if (fastNavResponse.getProducts().isEmpty() && !products.isEmpty() && intentResolver.isProductQuery(resolvedQuery)) {
                fastNavResponse.setProducts(products);
            }
            return fastNavResponse;
        }

        // 6. Ưu tiên xử lý qua Hệ thống Chuyên gia Nghiệp vụ Định sẵn (Tier 1: Fast-Path Rule Engine)
        AiChatResponse finalResponse = orderAndPolicyAdvisor.generateRuleBasedResponse(
                message, resolvedQuery, products, customerCtx, knowledgeContext, dynamicOrderContext, conversationId);

        // 7. Tầng 2 (Hybrid LLM Structured Router): Khi câu hỏi có cách diễn đạt tự nhiên/phức tạp nằm ngoài Fast-Path
        boolean isGreeting = message.toLowerCase().trim().matches("^(xin chào|chào|chào shop|chào bot|hi|hello|alo|ơi|shop ơi|bot ơi|bạn là ai|giúp mình|hỗ trợ mình).*");
        boolean fellThroughDeterministic = finalResponse != null
                && finalResponse.getMessage() != null
                && finalResponse.getMessage().startsWith("Xin chào! Mình là **CyberBot**");

        if (!isGreeting && fellThroughDeterministic && llmClient.isConfigured()) {
            SupportClassifiedIntent hybridIntent = intentResolver.classifyHybridIntent(message, request.getHistory());
            if (hybridIntent != null && hybridIntent.isValidIntent()) {
                switch (hybridIntent.intent()) {
                    case "PC_BUILD" -> {
                        BigDecimal budget = (hybridIntent.budgetVnd() != null && hybridIntent.budgetVnd() >= 3_000_000L)
                                ? BigDecimal.valueOf(hybridIntent.budgetVnd())
                                : intentResolver.extractBudgetFromMessage(message, request.getHistory());
                        return pcBuildSolver.formatSmartPcBuildAdvisory(message, budget, request.getHistory(), conversationId);
                    }
                    case "PC_UPGRADE" -> {
                        if (intentResolver.hasRecentPcBuildInHistory(request.getHistory())) {
                            return pcBuildAnalyzer.handlePcUpgradeOrSurplusAdvisory(message, request.getHistory(), conversationId);
                        }
                    }
                    case "PC_DEEP_DIVE" -> {
                        if (intentResolver.hasRecentPcBuildInHistory(request.getHistory())) {
                            return pcBuildAnalyzer.handlePcBuildDeepDiveAdvisory(message, request.getHistory(), conversationId);
                        }
                    }
                    case "CART_ACTION" -> {
                        if (intentResolver.hasRecentPcBuildInHistory(request.getHistory())) {
                            return handleReferralAndCartAction(message, request.getHistory(), conversationId);
                        }
                    }
                    case "COMPATIBILITY_CHECK" -> {
                        return productAdvisor.buildCompatibilityAdvisory(message, conversationId);
                    }
                    case "PRODUCT_COMPARE" -> {
                        return productAdvisor.buildComparisonAdvisory(message, products, conversationId);
                    }
                    case "PRODUCT_CONSULT" -> {
                        if (products.isEmpty() && hybridIntent.mentionedProducts() != null && !hybridIntent.mentionedProducts().isEmpty()) {
                            String prodQuery = String.join(" ", hybridIntent.mentionedProducts());
                            products = resolveCatalogProductsForQuery(prodQuery);
                        }
                        if (!products.isEmpty()) {
                            return productAdvisor.buildSmartProductAdvisory(message, resolvedQuery, products, conversationId);
                        }
                    }
                    case "STAFF_HANDOVER" -> {
                        return AiChatResponse.builder()
                                .conversationId(conversationId)
                                .message("Dạ, yêu cầu này của bạn cần sự hỗ trợ và thẩm định trực tiếp từ chuyên viên TechZone. **Bạn có muốn mình kết nối bạn với Nhân viên tư vấn (Staff) đang trực tuyến không ạ?**")
                                .messageType("handover_proposal")
                                .quickActions(List.of(
                                        new AiChatResponse.QuickAction("Kết nối nhân viên ngay", "connect_staff", null),
                                        new AiChatResponse.QuickAction("Tiếp tục hỏi CyberBot", "ask", "Tiếp tục hỗ trợ")
                                ))
                                .build();
                    }
                    case "OFF_TOPIC" -> {
                        return AiChatResponse.builder()
                                .conversationId(conversationId)
                                .message("Dạ, mình là **CyberBot** — Trợ lý chuyên trách về **Linh kiện Máy tính, Build PC, Đơn hàng & Bảo hành** tại **TechZone**. Câu hỏi này nằm ngoài phạm vi chuyên môn của mình rồi ạ!\n\nBạn có cần mình tư vấn cấu hình PC, tra cứu linh kiện trong kho hay kiểm tra đơn hàng / bảo hành gì không nhé?")
                                .messageType("info")
                                .quickActions(List.of(
                                        new AiChatResponse.QuickAction("Tư vấn cấu hình PC", "navigate", "/build-pc"),
                                        new AiChatResponse.QuickAction("Xem linh kiện tại Shop", "navigate", "/san-pham"),
                                        new AiChatResponse.QuickAction("Kiểm tra bảo hành", "navigate", "/bao-hanh")
                                ))
                                .build();
                    }
                    default -> {
                        // Tiếp tục xuống Gemini RAG tổng quát
                    }
                }
            }

            try {
                AiChatResponse llmResp = llmClient.callGeminiWithFallback(
                        message, resolvedQuery, request.getHistory(), customerCtx, knowledgeContext, catalogTextContext, conversationId);
                if (llmResp != null) {
                    finalResponse = llmResp;
                }
            } catch (Exception e) {
                log.warn("Lỗi khi gọi Gemini LLM cho câu hỏi mở, giữ phản hồi mặc định: {}", e.getMessage());
            }
        }

        // 8. Gắn danh sách Product Cards & Hậu kiểm bảo mật đầu ra
        if (finalResponse != null && finalResponse.getProducts().isEmpty() && !products.isEmpty() && intentResolver.isProductQuery(resolvedQuery)) {
            finalResponse.setProducts(products);
        }
        if (finalResponse != null && finalResponse.getMessage() != null) {
            finalResponse.setMessage(securityGuard.sanitizeBotOutput(finalResponse.getMessage()));
        }
        return finalResponse;
    }

    private List<AiChatResponse.ProductSummary> resolveCatalogProductsForQuery(String resolvedQuery) {
        List<AiChatResponse.ProductSummary> products = new ArrayList<>();
        if (!intentResolver.isProductQuery(resolvedQuery)) {
            return products;
        }

        CatalogSearchResult catalogResult = catalogRepository.searchCatalogProducts(resolvedQuery);
        Set<Integer> seenProdIds = new HashSet<>();
        if (!catalogResult.products().isEmpty()) {
            for (AiChatResponse.ProductSummary cp : catalogResult.products()) {
                if (cp.getId() != null && seenProdIds.add(cp.getId())) {
                    products.add(cp);
                }
            }
        }
        if (products.size() < 4 && catalogVectorSyncService != null) {
            List<AiChatResponse.ProductSummary> vectorProds = catalogVectorSyncService.searchProductsByVector(resolvedQuery, 6);
            if (vectorProds != null) {
                for (AiChatResponse.ProductSummary vp : vectorProds) {
                    if (vp.getId() != null && seenProdIds.add(vp.getId())) {
                        products.add(vp);
                        if (products.size() >= 6) break;
                    }
                }
            }
        }

        if (!products.isEmpty() && (products.get(0).getStock() == null || products.get(0).getStock() <= 0)) {
            String catName = products.get(0).getCategory();
            if (catName != null && !catName.isBlank() && catalogVectorSyncService != null) {
                List<AiChatResponse.ProductSummary> alternatives = catalogVectorSyncService.searchProductsByVector(catName + " chính hãng", 4);
                if (alternatives != null) {
                    for (AiChatResponse.ProductSummary alt : alternatives) {
                        if (!alt.getId().equals(products.get(0).getId()) && alt.getStock() != null && alt.getStock() > 0) {
                            if (products.stream().noneMatch(existing -> existing.getId().equals(alt.getId()))) {
                                products.add(alt);
                            }
                        }
                    }
                }
            }
        }
        return products;
    }

    private AiChatResponse handleReferralAndCartAction(
            String message,
            List<AiChatRequest.HistoryItem> history,
            String conversationId) {
        String searchContext = message;
        if (history != null && !history.isEmpty()) {
            String bestBotContext = null;
            int maxComponents = 0;

            for (int i = history.size() - 1; i >= 0; i--) {
                AiChatRequest.HistoryItem h = history.get(i);
                if ("bot".equalsIgnoreCase(h.getSender()) && h.getText() != null && !h.getText().isBlank()) {
                    String botText = h.getText();
                    if (botText.contains("🔹") || (botText.contains("• **") && botText.contains("—"))) {
                        bestBotContext = botText;
                        break;
                    }
                    List<String> comps = intentResolver.extractComponentLines(botText);
                    if (comps.size() > maxComponents) {
                        maxComponents = comps.size();
                        bestBotContext = botText;
                    }
                }
            }

            if (bestBotContext != null) {
                searchContext = bestBotContext;
            } else {
                for (int i = history.size() - 1; i >= 0; i--) {
                    AiChatRequest.HistoryItem h = history.get(i);
                    if ("bot".equalsIgnoreCase(h.getSender()) && h.getText() != null && !h.getText().isBlank()) {
                        searchContext = h.getText();
                        break;
                    }
                }
            }
        }

        List<AiChatResponse.ProductSummary> products = new ArrayList<>();
        List<String> componentLines = intentResolver.extractComponentLines(searchContext);
        Set<Integer> addedProductIds = new HashSet<>();
        Set<String> seenCategories = new HashSet<>();
        List<AiChatResponse.ProductSummary> candidates = new ArrayList<>();

        for (String line : componentLines) {
            AiChatResponse.ProductSummary exactProd = catalogRepository.findExactAdvisedProduct(line);
            if (exactProd != null) {
                candidates.add(exactProd);
            } else if (catalogVectorSyncService != null) {
                List<AiChatResponse.ProductSummary> found = catalogVectorSyncService.searchProductsByVector(line, 2);
                if (found == null || found.isEmpty()) {
                    CatalogSearchResult catRes = catalogRepository.searchCatalogProducts(line);
                    if (!catRes.products().isEmpty()) {
                        found = catRes.products();
                    }
                }
                if (found != null && !found.isEmpty()) {
                    candidates.addAll(found);
                }
            }
        }

        if (candidates.isEmpty() && catalogVectorSyncService != null && searchContext != null && !searchContext.isBlank()) {
            List<AiChatResponse.ProductSummary> vectorProds = catalogVectorSyncService.searchProductsByVector(searchContext, 6);
            if (vectorProds != null && !vectorProds.isEmpty()) {
                candidates.addAll(vectorProds);
            }
        }

        if (candidates.isEmpty()) {
            candidates.addAll(pcBuildSolver.buildRealPcComponentsForBudget(25_000_000L));
        }

        boolean isDdr5Main = false;
        boolean isDdr4Main = false;
        for (AiChatResponse.ProductSummary p : candidates) {
            if ("MAINBOARD".equals(securityGuard.normalizeCategoryGroup(p.getCategory()))) {
                String mainText = (p.getName() + " " + (p.getVariantName() != null ? p.getVariantName() : "")).toLowerCase();
                if (mainText.contains("ddr5")) {
                    isDdr5Main = true;
                    break;
                } else if (mainText.contains("ddr4")) {
                    isDdr4Main = true;
                    break;
                }
            }
        }

        for (AiChatResponse.ProductSummary p : candidates) {
            String catGroup = securityGuard.normalizeCategoryGroup(p.getCategory());
            if (seenCategories.contains(catGroup)) {
                continue;
            }
            if ("RAM".equals(catGroup)) {
                String ramText = (p.getName() + " " + (p.getVariantName() != null ? p.getVariantName() : "")).toLowerCase();
                if (isDdr5Main && ramText.contains("ddr4")) continue;
                if (isDdr4Main && ramText.contains("ddr5")) continue;
            }
            if (addedProductIds.add(p.getId())) {
                seenCategories.add(catGroup);
                products.add(p);
            }
        }

        if (!seenCategories.contains("RAM") && catalogVectorSyncService != null) {
            String ramQuery = isDdr5Main ? "RAM DDR5" : "RAM DDR4";
            List<AiChatResponse.ProductSummary> rams = catalogVectorSyncService.searchProductsByVector(ramQuery, 2);
            if (rams != null) {
                for (AiChatResponse.ProductSummary r : rams) {
                    if ("RAM".equals(securityGuard.normalizeCategoryGroup(r.getCategory())) && addedProductIds.add(r.getId())) {
                        seenCategories.add("RAM");
                        products.add(r);
                        break;
                    }
                }
            }
        }

        if (products.isEmpty()) {
            products.addAll(pcBuildSolver.buildRealPcComponentsForBudget(25_000_000L));
        }

        Set<String> targetCats = intentResolver.extractTargetCategories(message);
        boolean wantsAll = message.toLowerCase().matches(".*(tất cả|hết|cả bộ|toàn bộ|trọn bộ|bộ này|dàn này|dàn pc|add_all_to_cart).*");
        boolean isCartIntent = message.toLowerCase().matches(".*(thêm|cho|bỏ|đưa|chốt|lấy|mua|add_all_to_cart).*giỏ.*")
                || message.toLowerCase().matches(".*vào giỏ.*")
                || message.contains("add_all_to_cart")
                || (!targetCats.isEmpty() && message.toLowerCase().matches(".*(thêm|cho|lấy|mua|chốt|đưa|bỏ|chọn|order).*"));

        if (isCartIntent && !wantsAll && !targetCats.isEmpty()) {
            products = products.stream()
                    .filter(p -> targetCats.contains(securityGuard.normalizeCategoryGroup(p.getCategory())))
                    .collect(Collectors.toList());
        }

        if (!targetCats.isEmpty()) {
            Set<String> existingCats = products.stream()
                    .map(p -> securityGuard.normalizeCategoryGroup(p.getCategory()))
                    .collect(Collectors.toSet());
            for (String reqCat : targetCats) {
                if (!existingCats.contains(reqCat)) {
                    for (AiChatResponse.ProductSummary def : pcBuildSolver.buildRealPcComponentsForBudget(25_000_000L)) {
                        if (reqCat.equals(securityGuard.normalizeCategoryGroup(def.getCategory()))) {
                            products.add(def);
                            existingCats.add(reqCat);
                            break;
                        }
                    }
                }
            }
        } else if (wantsAll || isCartIntent) {
            Set<String> existingCats = products.stream()
                    .map(p -> securityGuard.normalizeCategoryGroup(p.getCategory()))
                    .collect(Collectors.toSet());
            String[] allCoreCats = {"CPU", "MAINBOARD", "RAM", "VGA", "SSD", "NGUỒN", "CASE", "TẢN NHIỆT"};
            for (String coreCat : allCoreCats) {
                if (!existingCats.contains(coreCat)) {
                    for (AiChatResponse.ProductSummary def : pcBuildSolver.buildRealPcComponentsForBudget(25_000_000L)) {
                        if (coreCat.equals(securityGuard.normalizeCategoryGroup(def.getCategory()))) {
                            products.add(def);
                            existingCats.add(coreCat);
                            break;
                        }
                    }
                }
            }
        }
        if (products.isEmpty()) {
            products.addAll(pcBuildSolver.buildRealPcComponentsForBudget(25_000_000L));
        }

        if (isCartIntent) {
            products = products.stream()
                    .filter(p -> p.getStock() != null && p.getStock() > 0)
                    .collect(Collectors.toList());
        }

        StringBuilder reply = new StringBuilder();
        double totalEst = securityGuard.sumBuildPrice(products);

        if (isCartIntent) {
            if (wantsAll || targetCats.isEmpty()) {
                reply.append("Dạ, CyberBot đã thêm trọn bộ **").append(products.size()).append(" linh kiện PC** vào giỏ hàng của bạn thành công! 🎉\n\n");
                int idx = 1;
                for (AiChatResponse.ProductSummary p : products) {
                    reply.append(idx++).append(". **").append(securityGuard.getCategoryDisplayName(p.getCategory())).append("**: ")
                            .append(p.getName()).append(" — **")
                            .append(MONEY_FMT.format(p.getPrice())).append("**\n");
                }
                reply.append("\n**Tổng giá trị trọn bộ**: **").append(MONEY_FMT.format(totalEst)).append("**\n");
                String promoAdvisory = customerContextRepository.buildDynamicPromotionAdvisory(totalEst);
                if (!promoAdvisory.isBlank()) {
                    reply.append(promoAdvisory).append("\n");
                }
                reply.append("🎁 **Cam kết từ TechZone**:\n");
                reply.append("• Miễn phí 100% công lắp ráp, đi dây chuyên nghiệp và stress-test kiểm tra độ ổn định.\n");
                reply.append("• Kích hoạt bảo hành điện tử chính hãng theo Serial linh kiện khi nhận máy.\n\n");
                reply.append("Linh kiện đã được thêm vào giỏ hàng. Bạn có thể kiểm tra giỏ hoặc thanh toán ngay nhé!");
            } else {
                reply.append("Dạ, CyberBot đã thêm **").append(products.size()).append(" linh kiện** được chọn vào giỏ hàng của bạn thành công! 🎉\n\n");
                for (AiChatResponse.ProductSummary p : products) {
                    reply.append("• **").append(securityGuard.getCategoryDisplayName(p.getCategory())).append("**: ")
                            .append(p.getName()).append(" — **")
                            .append(MONEY_FMT.format(p.getPrice())).append("**\n");
                }
                reply.append("\n**Tổng giá trị linh kiện đã chọn**: **").append(MONEY_FMT.format(totalEst)).append("**\n\n");
                reply.append("Linh kiện đã được thêm vào giỏ hàng. Bạn có thể kiểm tra giỏ hoặc thanh toán ngay nhé!");
            }
        } else {
            reply.append("Dạ, CyberBot đã tra cứu kho hàng và gửi bạn danh sách linh kiện thực tế có sẵn tại shop:\n\n");
            for (AiChatResponse.ProductSummary p : products) {
                reply.append("• **").append(securityGuard.getCategoryDisplayName(p.getCategory())).append("**: ")
                        .append(p.getName()).append(" — **")
                        .append(MONEY_FMT.format(p.getPrice())).append("**\n");
            }
            if (totalEst > 0) {
                reply.append("\n**Tổng giá trị dự tính**: **").append(MONEY_FMT.format(totalEst)).append("**\n");
                String promoAdvisory = customerContextRepository.buildDynamicPromotionAdvisory(totalEst);
                if (!promoAdvisory.isBlank()) {
                    reply.append(promoAdvisory).append("\n");
                }
            }
            reply.append("Bạn có thể bấm vào từng thẻ sản phẩm bên dưới để xem chi tiết hoặc bấm nút **Thêm tất cả vào giỏ hàng** để mua ngay nhé!");
        }

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType(isCartIntent ? "cart_action" : "info")
                .products(products)
                .quickActions(isCartIntent ? List.of(
                        new AiChatResponse.QuickAction("🛒 Xem giỏ hàng", "navigate", "/gio-hang"),
                        new AiChatResponse.QuickAction("💳 Tiến hành thanh toán", "navigate", "/thanh-toan"),
                        new AiChatResponse.QuickAction("💬 Nhờ chuyên viên hỗ trợ", "connect_staff", null)
                ) : List.of(
                        new AiChatResponse.QuickAction("🛒 Thêm tất cả vào giỏ hàng", "add_all_to_cart", "add_all_to_cart"),
                        new AiChatResponse.QuickAction("💳 Xem giỏ hàng", "navigate", "/gio-hang"),
                        new AiChatResponse.QuickAction("💬 Nhờ chuyên viên hỗ trợ", "connect_staff", null)
                ))
                .build();
    }
}
