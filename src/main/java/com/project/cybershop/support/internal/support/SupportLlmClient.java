package com.project.cybershop.support.internal.support;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.aiconfig.api.AiConfigPublicApi;
import com.project.cybershop.aiconfig.api.dto.AiConfigDto;
import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.CustomerContext;
import com.project.cybershop.support.internal.model.CustomerContext.OrderSummary;
import com.project.cybershop.support.internal.model.CustomerContext.ServiceRequestSummary;
import com.project.cybershop.support.internal.model.CustomerContext.WarrantySummary;
import com.project.cybershop.support.internal.model.StorePromotionContext;
import com.project.cybershop.support.internal.model.StorePromotionContext.FlashSaleSummary;
import com.project.cybershop.support.internal.model.StorePromotionContext.PromotionSummary;
import com.project.cybershop.support.internal.model.SupportClassifiedIntent;
import com.project.cybershop.support.internal.repository.SupportCustomerContextRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SupportLlmClient {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    private static final List<String> VALID_INTENTS = List.of(
            "PC_BUILD",
            "PC_UPGRADE",
            "PC_DEEP_DIVE",
            "PRODUCT_CONSULT",
            "PRODUCT_COMPARE",
            "COMPATIBILITY_CHECK",
            "CART_ACTION",
            "ORDER_LOOKUP",
            "WARRANTY_LOOKUP",
            "TROUBLESHOOTING",
            "POLICY_FAQ",
            "STAFF_HANDOVER",
            "OFF_TOPIC",
            "GENERAL_QA"
    );

    private static final Map<String, Object> INTENT_RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "intent", Map.of("type", "STRING", "enum", VALID_INTENTS),
                    "budgetVnd", Map.of("type", "INTEGER", "nullable", true),
                    "isAddingExtraBudget", Map.of("type", "BOOLEAN"),
                    "targetCategories", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                    "mentionedProducts", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                    "pcProfile", Map.of("type", "STRING", "nullable", true, "enum", List.of("GAMING", "WORKSTATION_3D", "OFFICE")),
                    "preferWhite", Map.of("type", "BOOLEAN"),
                    "preferAesthetic", Map.of("type", "BOOLEAN")
            ),
            "required", List.of("intent", "isAddingExtraBudget", "targetCategories", "mentionedProducts", "preferWhite", "preferAesthetic")
    );

    private static final Map<String, Object> COMMENTARY_RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "commentary", Map.of("type", "STRING")
            ),
            "required", List.of("commentary")
    );

    private static final String INTENT_ROUTER_SYSTEM_PROMPT = """
            BẠN LÀ BỘ PHÂN LOẠI Ý ĐỊNH & TRÍCH XUẤT THỰC THỂ (NLU ROUTER) CỦA TRỢ LÝ AI CYBERBOT TẠI CỬA HÀNG MÁY TÍNH TECHZONE.
            Nhiệm vụ: Đọc tin nhắn hiện tại của khách hàng và lịch sử hội thoại gần nhất, sau đó phân loại chính xác vào JSON Schema.

            QUY TẮC TRÍCH XUẤT BẮT BUỘC:
            1. Trường "intent":
               - "PC_BUILD": Khách muốn tư vấn/lên cấu hình/ráp trọn bộ PC theo ngân sách hoặc nhu cầu (kể cả viết tắt, sai chính tả hoặc ghi số bằng chữ như "bốn mươi triệu", "4 chục củ", "tầm 35").
               - "PC_UPGRADE": Đã có bộ PC vừa tư vấn trong lịch sử và khách muốn nâng cấp, dùng tiền dư để nâng món, bù thêm tiền nâng linh kiện hoặc đổi món trong bộ đó.
               - "PC_DEEP_DIVE": Đã có bộ PC vừa tư vấn trong lịch sử và khách hỏi sâu về hiệu năng, FPS, nhiệt độ, hoặc hỏi chi tiết về CPU/RAM/VGA/Nguồn/Mainboard của bộ đó.
               - "PRODUCT_CONSULT": Hỏi giá, tồn kho, thông số hoặc tư vấn 1 linh kiện/sản phẩm lẻ.
               - "PRODUCT_COMPARE": So sánh đối chiếu giữa 2 linh kiện/sản phẩm.
               - "COMPATIBILITY_CHECK": Hỏi 2 linh kiện có lắp chung/tương thích với nhau không (VD: i5 14400F lắp B650 được không, RAM DDR4 cắm main B760 DDR5 được không).
               - "CART_ACTION": Muốn thêm bộ PC hoặc linh kiện vừa tư vấn vào giỏ hàng, hoặc xin danh sách linh kiện vừa nói.
               - "ORDER_LOOKUP": Tra cứu đơn hàng, vận đơn GHN.
               - "WARRANTY_LOOKUP": Tra cứu bảo hành, phiếu sửa chữa RMA.
               - "TROUBLESHOOTING": Hỏi cách xử lý lỗi phần cứng (không lên hình, màn hình xanh BSOD, lỏng RAM, quá nhiệt).
               - "POLICY_FAQ": Hỏi chính sách bảo hành, đổi trả, trả góp, giao hàng, mã giảm giá/khuyến mãi.
               - "STAFF_HANDOVER": Yêu cầu gặp nhân viên tư vấn, xuất hóa đơn đỏ VAT công ty, mua sỉ B2B, khiếu nại.
               - "OFF_TOPIC": Câu hỏi ngoài lề hoàn toàn không liên quan đến máy tính, linh kiện hay mua sắm tại TechZone (nấu ăn, thời tiết, bóng đá, giải toán, lịch sử...).
               - "GENERAL_QA": Chào hỏi hoặc câu hỏi chung khác về cửa hàng.
            2. Trường "budgetVnd":
               - Đổi mọi cách viết tiền tệ tiếng Việt sang số nguyên VNĐ chính xác (VD: "40tr", "bốn mươi triệu", "4 chục củ", "tầm 40" -> 40000000; "25 củ rưỡi", "hai mươi lăm triệu năm trăm" -> 25500000).
               - Nếu không nhắc đến số tiền/ngân sách thì để null.
            3. Trường "targetCategories":
               - Chỉ lấy từ tập chuẩn: ["CPU", "MAINBOARD", "RAM", "VGA", "SSD", "NGUỒN", "CASE", "TẢN NHIỆT"].
            4. Trường "pcProfile":
               - "WORKSTATION_3D" nếu làm đồ họa 3D, Blender, Render, dựng phim, lập trình, chạy Docker/máy ảo/AI.
               - "OFFICE" nếu làm văn phòng, kế toán, học tập nhẹ nhàng.
               - "GAMING" nếu chơi game, eSports, AAA.
            """;

    private static final String GROUNDED_COMMENTARY_TECHNICAL_CONTRACT = """
            QUY ĐỊNH KỸ THUẬT HỆ THỐNG BẮT BUỘC (GROUNDED COMMENTARY CONTRACT):
            1. Bạn đóng vai Kỹ sư phần cứng trưởng của TechZone, viết đoạn nhận xét chuyên môn ngắn gọn (2–3 câu, khoảng 120–420 ký tự) giải thích trực tiếp theo đúng câu hỏi/nhu cầu riêng của khách hàng ("userMessage") dựa trên dữ liệu phần cứng đã khóa cứng ("lockedFacts").
            2. RÀNG BUỘC SỐ LIỆU TUYỆT ĐỐI:
               - CHỈ ĐƯỢC NHẮC ĐẾN đúng tên linh kiện, thông số và mức giá nằm trong "lockedFacts".
               - TUYỆT ĐỐI KHÔNG tự bịa thêm tên sản phẩm khác, không tự đổi giá tiền, không ghi "DDR4 / DDR5" nước đôi.
               - TUYỆT ĐỐI KHÔNG bảo khách sang trang /build-pc.
            3. Văn phong tiếng Việt chuyên nghiệp, súc tích, dùng in đậm Markdown (**...**) cho các điểm nhấn kỹ thuật (như tên chip, GPU, dung lượng RAM, công nghệ DLSS/3D V-Cache/CUDA, luồng gió Airflow).
            """;

    private final ObjectMapper objectMapper;
    private final ObjectMapper tolerantMapper;
    private final SupportCustomerContextRepository customerContextRepository;
    private final SupportSecurityGuard securityGuard;
    private final AiConfigPublicApi aiConfigApi;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Map<String, SupportClassifiedIntent> intentCache = new ConcurrentHashMap<>();
    private final Map<String, String> commentaryCache = new ConcurrentHashMap<>();
    private final Map<String, AiConfigDto> configCache = new ConcurrentHashMap<>();
    private volatile long configCacheTimestamp = 0L;
    private static final long CONFIG_CACHE_TTL_MS = 30_000L;
    private static final AiConfigDto EMPTY_CONFIG = new AiConfigDto();

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.chatbox-model:gemini-2.5-flash}")
    private String modelName;

    public SupportLlmClient(
            ObjectMapper objectMapper,
            SupportCustomerContextRepository customerContextRepository,
            SupportSecurityGuard securityGuard,
            AiConfigPublicApi aiConfigApi) {
        this.objectMapper = objectMapper;
        this.customerContextRepository = customerContextRepository;
        this.securityGuard = securityGuard;
        this.aiConfigApi = aiConfigApi;
        this.tolerantMapper = objectMapper.copy()
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
    }

    private AiConfigDto getCachedActiveConfig(String configKey) {
        if (aiConfigApi == null) return null;
        long now = System.currentTimeMillis();
        if (now - configCacheTimestamp > CONFIG_CACHE_TTL_MS) {
            configCache.clear();
            configCacheTimestamp = now;
        }
        AiConfigDto cached = configCache.get(configKey);
        if (cached != null) {
            return cached == EMPTY_CONFIG ? null : cached;
        }
        try {
            AiConfigDto cfg = aiConfigApi.getActiveConfig(configKey);
            configCache.put(configKey, cfg != null ? cfg : EMPTY_CONFIG);
            return cfg;
        } catch (Exception ignored) {
            configCache.put(configKey, EMPTY_CONFIG);
            return null;
        }
    }

    public String resolveEffectiveApiKey() {
        if (aiConfigApi != null) {
            for (String configKey : List.of("CHAT_BOX", "PC_BUILD_ANA", "PC_BUILD_EXPLAIN", "EMBEDDING_CONFIG")) {
                AiConfigDto cfg = getCachedActiveConfig(configKey);
                if (cfg != null && cfg.getApiKey() != null && !cfg.getApiKey().isBlank()
                        && !cfg.getApiKey().startsWith("AI_API_KEY_PLACEHOLDER")) {
                    return cfg.getApiKey().trim();
                }
            }
        }
        return (apiKey != null && !apiKey.startsWith("AI_API_KEY_PLACEHOLDER")) ? apiKey.trim() : "";
    }

    public String resolveEffectiveModel() {
        if (aiConfigApi != null) {
            AiConfigDto cfg = getCachedActiveConfig("CHAT_BOX");
            if (cfg != null && cfg.getModelName() != null && !cfg.getModelName().isBlank()) {
                String m = cfg.getModelName().trim();
                if (!m.contains("gemini-3.5-flash-lite")) {
                    return m;
                }
            }
        }
        if (modelName != null && !modelName.isBlank() && !modelName.contains("gemini-3.5-flash-lite")) {
            return modelName.trim();
        }
        return "gemini-2.5-flash";
    }

    private String resolveAdminCustomPrompt() {
        if (aiConfigApi != null) {
            AiConfigDto cfg = getCachedActiveConfig("CHAT_BOX");
            if (cfg != null && cfg.getSystemPrompt() != null && !cfg.getSystemPrompt().isBlank()) {
                return cfg.getSystemPrompt().trim();
            }
        }
        return "";
    }

    public boolean isConfigured() {
        String key = resolveEffectiveApiKey();
        return key != null && !key.isBlank();
    }

    /**
     * Tầng 2 (Hybrid LLM Structured Router): Phân loại ý định & bóc tách ngân sách/linh kiện
     * bằng Gemini Structured Outputs (responseSchema) với cơ chế cache và fallback an toàn.
     */
    public SupportClassifiedIntent classifyIntentWithLlm(
            String message,
            List<AiChatRequest.HistoryItem> history) {
        if (message == null || message.isBlank() || !isConfigured()) {
            return SupportClassifiedIntent.empty();
        }

        String recentBotSummary = "";
        if (history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= Math.max(0, history.size() - 4); i--) {
                AiChatRequest.HistoryItem h = history.get(i);
                if ("bot".equalsIgnoreCase(h.getSender()) && h.getText() != null) {
                    String bt = h.getText();
                    if (bt.contains("🔹") || bt.contains("Cấu hình đề xuất")) {
                        recentBotSummary = "HAS_RECENT_PC_BUILD";
                        break;
                    }
                }
            }
        }

        String cacheKey = message.trim().toLowerCase() + "|" + recentBotSummary;
        SupportClassifiedIntent cached = intentCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            Map<String, Object> inputPayload = new LinkedHashMap<>();
            inputPayload.put("userMessage", message.trim());
            inputPayload.put("hasRecentPcBuildInHistory", !recentBotSummary.isEmpty());
            if (history != null && !history.isEmpty()) {
                List<String> recentTurns = new ArrayList<>();
                for (int i = Math.max(0, history.size() - 4); i < history.size(); i++) {
                    AiChatRequest.HistoryItem item = history.get(i);
                    if (item.getText() != null && !item.getText().isBlank()) {
                        String snippet = item.getText().length() > 180 ? item.getText().substring(0, 180) + "..." : item.getText();
                        recentTurns.add(item.getSender() + ": " + snippet);
                    }
                }
                inputPayload.put("recentConversation", recentTurns);
            }

            String userJson = objectMapper.writeValueAsString(inputPayload);
            String rawJson = postStructuredGemini(
                    resolveEffectiveModel(),
                    resolveEffectiveApiKey(),
                    INTENT_ROUTER_SYSTEM_PROMPT,
                    userJson,
                    INTENT_RESPONSE_SCHEMA,
                    Duration.ofMillis(4200)
            );

            if (rawJson != null && !rawJson.isBlank()) {
                SupportClassifiedIntent parsed = tolerantMapper.readValue(stripCodeFence(rawJson), SupportClassifiedIntent.class);
                if (parsed != null && parsed.isValidIntent()) {
                    if (intentCache.size() > 256) {
                        intentCache.clear();
                    }
                    intentCache.put(cacheKey, parsed);
                    log.info("[HYBRID_INTENT_ROUTER] message='{}' -> intent={}, budget={}, categories={}, profile={}",
                            message, parsed.intent(), parsed.budgetVnd(), parsed.targetCategories(), parsed.pcProfile());
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.debug("[HYBRID_INTENT_ROUTER] Fallback về bộ quy tắc định sẵn ({}): {}", e.getClass().getSimpleName(), e.getMessage());
        }
        return SupportClassifiedIntent.empty();
    }

    /**
     * Tầng Kết hợp (Hybrid Grounded Commentary):
     * Java khóa chặt dữ liệu phần cứng/giá/tồn kho trong lockedFacts,
     * Gemini viết đoạn giải thích chuyên sâu bám sát đúng câu hỏi của khách hàng.
     * Nếu Gemini timeout hoặc lỗi -> trả về ngay fallbackCommentary trong 0ms.
     */
    public String generateGroundedCommentary(
            String scenario,
            String userMessage,
            String lockedFacts,
            String fallbackCommentary) {
        if (!isConfigured() || userMessage == null || userMessage.isBlank() || lockedFacts == null || lockedFacts.isBlank()) {
            return fallbackCommentary;
        }

        String cacheKey = scenario + "|" + userMessage.trim().toLowerCase() + "|" + lockedFacts.hashCode();
        String cached = commentaryCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String adminPrompt = resolveAdminCustomPrompt();
            String systemPrompt = (adminPrompt.isEmpty() ? "" : adminPrompt + "\n\n") + GROUNDED_COMMENTARY_TECHNICAL_CONTRACT;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("scenario", scenario);
            payload.put("userMessage", userMessage.trim());
            payload.put("lockedFacts", lockedFacts);

            String userJson = objectMapper.writeValueAsString(payload);
            String rawJson = postStructuredGemini(
                    resolveEffectiveModel(),
                    resolveEffectiveApiKey(),
                    systemPrompt,
                    userJson,
                    COMMENTARY_RESPONSE_SCHEMA,
                    Duration.ofMillis(4500)
            );

            if (rawJson != null && !rawJson.isBlank()) {
                JsonNode node = tolerantMapper.readTree(stripCodeFence(rawJson));
                String commentary = node.path("commentary").asText("").trim();
                commentary = securityGuard.sanitizeBotOutput(commentary);
                if (isValidGroundedCommentary(commentary)) {
                    if (commentaryCache.size() > 256) {
                        commentaryCache.clear();
                    }
                    commentaryCache.put(cacheKey, commentary);
                    return commentary;
                }
            }
        } catch (Exception e) {
            log.debug("[GROUNDED_COMMENTARY] Scenario={} dùng văn mẫu kỹ sư dự phòng ({})", scenario, e.getMessage());
        }
        return fallbackCommentary;
    }

    private boolean isValidGroundedCommentary(String commentary) {
        if (commentary == null || commentary.length() < 40 || commentary.length() > 750) {
            return false;
        }
        String lower = commentary.toLowerCase();
        if (lower.contains("/build-pc") || lower.contains("ddr4 / ddr5") || lower.contains("ddr4/ddr5")) {
            return false;
        }
        return true;
    }

    private String postStructuredGemini(
            String targetModel,
            String key,
            String systemPrompt,
            String userJson,
            Map<String, Object> responseSchema,
            Duration timeout) throws Exception {
        String cleanModel = targetModel.startsWith("models/") ? targetModel.substring(7) : targetModel;
        String url = GEMINI_BASE_URL + cleanModel + ":generateContent";

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema);
        generationConfig.put("temperature", 0.25);

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userJson)))),
                "generationConfig", generationConfig
        );

        String jsonPayload = objectMapper.writeValueAsString(body);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", key)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (httpResponse.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + httpResponse.statusCode());
        }

        JsonNode root = tolerantMapper.readTree(httpResponse.body());
        for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
            if (part.hasNonNull("text") && !part.path("thought").asBoolean(false)) {
                return part.path("text").asText().trim();
            }
        }
        return "";
    }

    private static String stripCodeFence(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }
        return trimmed;
    }

    public AiChatResponse callGeminiWithFallback(
            String message,
            String resolvedQuery,
            List<AiChatRequest.HistoryItem> history,
            CustomerContext ctx,
            String knowledgeContext,
            String catalogContext,
            String conversationId) throws Exception {
        String effectiveModel = resolveEffectiveModel();
        try {
            return executeGeminiCall(effectiveModel, message, resolvedQuery, history, ctx, knowledgeContext, catalogContext, conversationId);
        } catch (Exception e) {
            log.warn("Gọi Gemini model '{}' thất bại ({}). Đang tự động thử lại với model dự phòng 'gemini-2.5-flash'...", effectiveModel, e.getMessage());
            if (!"gemini-2.5-flash".equalsIgnoreCase(effectiveModel)) {
                try {
                    return executeGeminiCall("gemini-2.5-flash", message, resolvedQuery, history, ctx, knowledgeContext, catalogContext, conversationId);
                } catch (Exception fallbackEx) {
                    log.warn("Model dự phòng 'gemini-2.5-flash' cũng thất bại: {}", fallbackEx.getMessage());
                    throw fallbackEx;
                }
            }
            throw e;
        }
    }

    private AiChatResponse executeGeminiCall(
            String targetModel,
            String message,
            String resolvedQuery,
            List<AiChatRequest.HistoryItem> history,
            CustomerContext ctx,
            String knowledgeContext,
            String catalogContext,
            String conversationId) throws Exception {
        String effectiveKey = resolveEffectiveApiKey();
        if (effectiveKey == null || effectiveKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình API Key cho CyberBot");
        }

        String systemInstruction = buildSystemPrompt(ctx, knowledgeContext, catalogContext);

        String jsonSchemaPrompt = """
            
            BẠN PHẢI TRẢ LỜI ĐÚNG ĐỊNH DẠNG JSON SAU ĐÂY (không bọc code fence markdown):
            {
              "message": "Nội dung câu trả lời súc tích, chuyên nghiệp chuẩn TechZone (hỗ trợ định dạng Markdown rõ ràng, không lạm dụng emoji)",
              "messageType": "info" HOẶC "proposal" HOẶC "navigate" HOẶC "cart_action",
              "messageRoute": "/don-hang/{orderCode}" HOẶC "/bao-hanh" HOẶC "/gio-hang" HOẶC "/san-pham/{productId}" HOẶC null,
              "quickActions": [
                {"label": "🛒 Thêm tất cả vào giỏ hàng", "action": "add_all_to_cart", "route": "add_all_to_cart"},
                {"label": "💳 Xem giỏ hàng", "action": "navigate", "route": "/gio-hang"}
              ]
            }
            """;

        String cleanModel = targetModel.startsWith("models/") ? targetModel.substring(7) : targetModel;
        String url = GEMINI_BASE_URL + cleanModel + ":generateContent";

        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null && !history.isEmpty()) {
            for (AiChatRequest.HistoryItem h : history) {
                if (h.getText() == null || h.getText().isBlank()) continue;
                String role = "user".equalsIgnoreCase(h.getSender()) ? "user" : "model";
                contents.add(Map.of("role", role, "parts", List.of(Map.of("text", h.getText()))));
            }
        }
        String effectiveUserMessage = (resolvedQuery != null && !resolvedQuery.isBlank() && !resolvedQuery.equalsIgnoreCase(message.trim()))
                ? message + "\n[Ngữ cảnh sản phẩm/linh kiện đang xét: " + resolvedQuery + "]"
                : message;
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", effectiveUserMessage))));

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction + jsonSchemaPrompt))),
                "contents", contents,
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.35
                )
        );

        String jsonPayload = objectMapper.writeValueAsString(body);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", effectiveKey)
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String rawResponse = httpResponse.body();

        if (httpResponse.statusCode() != 200) {
            throw new IllegalStateException("Google Gemini API trả về HTTP " + httpResponse.statusCode() + ": " + rawResponse);
        }

        JsonNode root = tolerantMapper.readTree(rawResponse);
        String text = stripCodeFence(root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText().trim());

        String msg;
        String msgType = "info";
        String msgRoute = null;
        List<AiChatResponse.QuickAction> quickActions = new ArrayList<>();

        try {
            JsonNode resJson = tolerantMapper.readTree(text);
            msg = resJson.path("message").asText(text);
            msgType = resJson.path("messageType").asText("info");
            msgRoute = resJson.hasNonNull("messageRoute") && !resJson.path("messageRoute").asText().isBlank()
                    ? resJson.path("messageRoute").asText() : null;

            JsonNode qaNode = resJson.path("quickActions");
            if (qaNode.isArray()) {
                for (JsonNode actionItem : qaNode) {
                    if (actionItem.isObject()) {
                        String label = actionItem.path("label").asText();
                        String action = actionItem.path("action").asText("navigate");
                        String route = actionItem.hasNonNull("route") ? actionItem.path("route").asText() : null;
                        quickActions.add(new AiChatResponse.QuickAction(label, action, route));
                    } else if (actionItem.isTextual()) {
                        String label = actionItem.asText();
                        quickActions.add(new AiChatResponse.QuickAction(label, "ask", null));
                    }
                }
            }
        } catch (Exception parseEx) {
            log.info("Gemini không trả về JSON cấu trúc chuẩn, dùng text thô: {}", parseEx.getMessage());
            msg = text;
        }

        if (msg == null || msg.isBlank()) {
            throw new IllegalStateException("Phản hồi rỗng từ Gemini");
        }

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(msg)
                .messageType(msgType)
                .messageRoute(msgRoute)
                .quickActions(quickActions)
                .build();
    }

    public String buildSystemPrompt(CustomerContext ctx, String knowledgeContext, String catalogContext) {
        StringBuilder sb = new StringBuilder();
        String adminCustomPrompt = resolveAdminCustomPrompt();
        if (!adminCustomPrompt.isEmpty()) {
            sb.append(adminCustomPrompt).append("\n\n");
        }
        sb.append("""
            BẠN LÀ CYBERBOT - Trợ lý AI thông minh, chuyên nghiệp và am hiểu kỹ thuật của cửa hàng công nghệ TechZone.
            NHIỆM VỤ CỐT LÕI:
            1. Trả lời bằng tiếng Việt chuyên nghiệp, súc tích, chuẩn mực kỹ sư phần cứng TechZone. Hạn chế tối đa sử dụng emoji hoạt hình để đảm bảo giao diện lịch sự, đáng tin cậy.
            2. Trả lời đúng trọng tâm câu hỏi của khách hàng:
               - Khách hỏi về linh kiện / RAM / CPU / VGA / màn hình -> Dựa vào KHO LINH KIỆN THỰC TẾ của TechZone để trả lời chính xác thông số, tồn kho, giá bán và đề xuất nút xem sản phẩm (/san-pham/{id}).
               - Khách hỏi các sản phẩm ngoài danh mục (Laptop, Macbook, Điện thoại...) -> Lịch sự giải thích TechZone chuyên sâu vào linh kiện Máy tính để bàn PC và Máy trạm Workstation, không kinh doanh laptop/điện thoại; gợi ý cấu hình PC để bàn tương đương về ngân sách nhưng hiệu năng vượt trội.
               - Khách hỏi xử lý sự cố (Troubleshooting: Không lên hình, lỏng RAM, màn hình xanh BSOD, quá nhiệt) -> Chẩn đoán nguyên nhân kỹ thuật và hướng dẫn xử lý từng bước (cắm dây vào cổng VGA rời, lau chân RAM, kiểm tra đèn Debug LED, miễn phí vệ sinh tra keo tại shop).
               - Khách hỏi tư vấn build PC / cấu hình máy tính / lên dàn máy theo ngân sách hoặc nhu cầu (ví dụ: 'build PC 25 triệu', 'tư vấn cấu hình đồ họa 3D', 'lên dàn máy gaming'...):
                 BẠN PHẢI TƯ VẤN CỤ THỂ ĐẦY ĐỦ TRỌN BỘ 8 LINH KIỆN MÁY TÍNH (CPU, Mainboard, RAM, Card đồ họa VGA, Ổ cứng SSD, Nguồn PSU, Vỏ Case, Tản nhiệt) kèm tên sản phẩm thực tế từ KHO LINH KIỆN THỰC TẾ của TechZone, giá bán, thông số nổi bật và tổng ngân sách.
                 Phân tích ngắn gọn lý do chọn cấu hình và nêu rõ cam kết TechZone: Miễn phí 100% công lắp ráp, stress-test kiểm tra nhiệt độ và kích hoạt bảo hành điện tử chính hãng theo Serial.
                 - KHI TƯ VẤN CẤU HÌNH PC HOẶC LINH KIỆN CÓ TỔNG GIÁ TRỊ, BẠN BẮT BUỘC PHẢI ĐỐI CHIẾU DANH SÁCH MÃ VOUCHER TRONG MỤC 'CHƯƠNG TRÌNH KHUYẾN MÃI' VÀ TÍNH TOÁN SỐ TIỀN KHÁCH ĐƯỢC GIẢM (VÍ DỤ: 'Nhập mã NEWBIE tại bước thanh toán để giảm ngay 500,000₫, tổng tiền chỉ còn X...'). KHÔNG ĐƯỢC BỎ QUÊN TƯ VẤN MÃ GIẢM GIÁ CHO KHÁCH.
                 TUYỆT ĐỐI KHÔNG chỉ nói chung chung hoặc chỉ bảo khách sang trang /build-pc mà không liệt kê danh sách linh kiện trong khung chat!
               - Khách hỏi so sánh hoặc tính tương thích -> Phân tích rõ ràng sự khác biệt về socket (LGA1700 vs AM5), chuẩn RAM (DDR4 vs DDR5), công suất nguồn (PSU) và khuyến nghị mẫu phù hợp.
               - Khách hỏi về chính sách bảo hành, GHN, thanh toán -> Dựa vào TRI THỨC TỪ VECTOR DATABASE để giải đáp.
               - Khách hỏi về đơn hàng hoặc bảo hành cá nhân -> Dựa vào THÔNG TIN KHÁCH HÀNG (nếu đã đăng nhập).
            3. HỖ TRỢ ĐIỀU HƯỚNG THÔNG MINH (Smart Navigation):
               - Khi khách hỏi xem danh sách linh kiện trên shop (ví dụ: 'gửi tôi list linh kiện này', 'list những linh kiện bạn nói để tôi xem trên shop'...), bạn PHẢI trả lời chi tiết danh sách linh kiện trong khung chat, TUYỆT ĐỐI KHÔNG tự ý chuyển hướng sang '/build-pc'!
               - Khi khách hỏi tư vấn cấu hình PC hoặc hỏi linh kiện, bạn PHẢI luôn đề xuất nút:
                 1. {"label": "🛒 Thêm tất cả vào giỏ hàng", "action": "add_all_to_cart", "route": "add_all_to_cart"}
                 2. {"label": "💳 Xem giỏ hàng", "action": "navigate", "route": "/gio-hang"}
                 3. {"label": "💬 Nhờ chuyên viên hỗ trợ", "action": "connect_staff", "route": null}
                 TUYỆT ĐỐI KHÔNG hiện nút "Tự mở công cụ Build PC" hay nút chuyển hướng sang '/build-pc' khi đang tư vấn linh kiện hoặc cấu hình PC!
               - Khi khách hỏi về đơn hàng cá nhân, trả lời chi tiết và CHỦ ĐỘNG HỎI: 'Bạn có muốn mình đưa bạn tới trang chi tiết đơn hàng để theo dõi tiến trình không?' kèm nút hành động.
               - Khi khách hỏi bảo hành, đề xuất nút chuyển đến '/bao-hanh'.
               - Khi tư vấn được sản phẩm ưng ý, đề xuất nút xem chi tiết '/san-pham/{id}'.
               - TƯƠNG THÍCH PHẦN CỨNG BẮT BUỘC (DDR4 / DDR5): Khi tư vấn cấu hình PC theo ngân sách, bạn PHẢI chọn dứt khoát 1 chuẩn bộ nhớ duy nhất (ví dụ đã chọn Mainboard DDR4 thì RAM phải là DDR4; đã chọn Mainboard DDR5 thì RAM phải là DDR5). TUYỆT ĐỐI KHÔNG ghi nước đôi 'DDR4 / DDR5' trên cùng một dòng khiến khách hàng bối rối và linh kiện bị xung đột!
            4. QUY TẮC BẢO TOÀN NGỮ CẢNH (Contextual Follow-up):
               - Nếu khách hàng đồng ý với đề xuất gần nhất của bạn (nói 'có', 'ok', 'được', 'chuyển đi'...), hãy chuyển messageType='navigate' và messageRoute tương ứng.
               - Nếu khách hàng đổi chủ đề hoặc hỏi một câu hỏi mới (ví dụ hỏi linh kiện khác như RAM, VGA, hỏi giá, hỏi thông số...), bạn PHẢI trả lời câu hỏi mới đó, TUYỆT ĐỐI KHÔNG tiếp tục điều hướng theo đề xuất cũ!
            5. ĐỀ XUẤT KẾT NỐI NHÂN VIÊN (Handover to Staff):
               - Khi khách hỏi các vấn đề ngoài phạm vi giải quyết của AI (hóa đơn đỏ VAT công ty, mua sỉ/dự án B2B, khiếu nại nhân viên, chuyển khoản nhầm, hoặc yêu cầu gặp người thật/tổng đài):
               - Hãy trả lời lịch sự và ĐỀ XUẤT: 'Bạn có muốn mình kết nối bạn với Nhân viên tư vấn (Staff) đang trực tuyến không ạ?'
               - Đặt messageType = 'handover_proposal' và quickActions = [{'label': 'Kết nối nhân viên ngay', 'action': 'connect_staff', 'route': null}].
            
            """);

        if (ctx.isMember()) {
            sb.append("\n=== THÔNG TIN KHÁCH HÀNG ĐANG CHAT (ĐÃ ĐĂNG NHẬP) ===\n");
            sb.append("Khách hàng: ").append(ctx.fullName()).append(" (SĐT: ").append(securityGuard.maskPhone(ctx.phone())).append(")\n");
            sb.append("DANH SÁCH ĐƠN HÀNG GẦN ĐÂY CỦA KHÁCH:\n");
            for (OrderSummary o : ctx.recentOrders()) {
                sb.append("- Đơn: ").append(o.code())
                        .append(" | Trạng thái: ").append(customerContextRepository.mapStatusVietnamese(o.status()))
                        .append(" | Vận chuyển: ").append(o.deliveryType())
                        .append(o.trackingCode() != null ? " (Mã GHN: " + o.trackingCode() + ")" : "")
                        .append(" | Tổng tiền: ").append(MONEY_FMT.format(o.finalAmount()))
                        .append(" | Sản phẩm: ").append(o.items())
                        .append(" | Đặt lúc: ").append(o.createdAt()).append("\n");
            }

            sb.append("\nDANH SÁCH LINH KIỆN ĐANG BẢO HÀNH CỦA KHÁCH:\n");
            for (WarrantySummary w : ctx.activeWarranties()) {
                sb.append("- ").append(w.productName())
                        .append(" | Serial: ").append(w.serialNumber())
                        .append(" | Hết hạn: ").append(w.endDate())
                        .append(" (Còn ").append(w.daysRemaining()).append(" ngày)\n");
            }

            if (ctx.activeServiceRequests() != null && !ctx.activeServiceRequests().isEmpty()) {
                sb.append("\nTIẾN ĐỘ PHIẾU BẢO HÀNH & SỬA CHỮA CỦA KHÁCH (KỸ THUẬT VIÊN ĐANG XỬ LÝ):\n");
                for (ServiceRequestSummary sr : ctx.activeServiceRequests()) {
                    sb.append("- Phiếu: `" + sr.requestCode() + "` | Thiết bị: " + sr.productName() + " (Serial: `" + sr.serialNumber() + "`) | Trạng thái Roadmap: **" + sr.statusVietnamese() + "** | Lỗi: " + sr.issueDescription() + " | Dự kiến hoàn tất/trả máy: " + sr.expectedReturnDate() + "\n");
                }
            }
        }

        StorePromotionContext promoCtx = customerContextRepository.fetchActivePromotionsAndFlashSales();
        if (!promoCtx.activeVouchers().isEmpty() || !promoCtx.activeFlashSales().isEmpty()) {
            sb.append("\n=== CHƯƠNG TRÌNH KHUYẾN MÃI, MÃ GIẢM GIÁ & FLASH SALE ĐANG DIỄN RA ===\n");
            if (!promoCtx.activeVouchers().isEmpty()) {
                sb.append("MÃ VOUCHER GIẢM GIÁ ÁP DỤNG KHI THANH TOÁN:\n");
                for (PromotionSummary p : promoCtx.activeVouchers()) {
                    String discount = "percentage".equalsIgnoreCase(p.discountType()) ? (p.discountValue() != null ? p.discountValue().intValue() + "%" : "Ưu đãi") : (p.discountValue() != null ? MONEY_FMT.format(p.discountValue().doubleValue()) : "Ưu đãi");
                    sb.append("- Mã: `" + p.code() + "` - " + p.name() + " (Mức giảm: " + discount + " | HSD: " + p.endDate() + ")\n");
                }
            }
            if (!promoCtx.activeFlashSales().isEmpty()) {
                sb.append("SẢN PHẨM FLASH SALE GIÁ SỐC HÔM NAY:\n");
                for (FlashSaleSummary fs : promoCtx.activeFlashSales()) {
                    sb.append("- " + fs.productName() + " (" + fs.variantName() + "): Giá sale: " + MONEY_FMT.format(fs.livePrice()) + " (Giá gốc: " + MONEY_FMT.format(fs.originalPrice()) + " - " + fs.discountDesc() + ")\n");
                }
            }
        } else {
            sb.append("\n=== KHÁCH VÃNG LAI (CHƯA ĐĂNG NHẬP) ===\n");
            sb.append("Khách chưa đăng nhập. Nhắc khách đăng nhập nếu muốn tra cứu đơn hàng hoặc bảo hành cá nhân.\n");
        }

        if (catalogContext != null && !catalogContext.isBlank()) {
            sb.append("\n=== KHO LINH KIỆN THỰC TẾ CỦA TECHZONE (THỜI GIAN THỰC) ===\n");
            sb.append(catalogContext).append("\n");
            sb.append("LƯU Ý TƯ VẤN: Nếu khách hỏi mẫu linh kiện KHÔNG có trong danh sách trên hoặc có sản phẩm ghi Tồn kho = 0, bạn hãy thông báo rõ ràng tình trạng kho và tư vấn gợi ý các mẫu tương đương ở trên kèm nút bấm xem sản phẩm (/san-pham/{id})!\n");
        }

        sb.append("\n=== TRI THỨC TỪ VECTOR DATABASE (CHÍNH SÁCH, LẮP RÁP & TROUBLESHOOTING) ===\n");
        sb.append(knowledgeContext).append("\n");

        return sb.toString();
    }
}
