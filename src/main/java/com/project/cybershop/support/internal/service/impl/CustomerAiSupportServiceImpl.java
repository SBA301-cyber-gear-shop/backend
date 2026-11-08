package com.project.cybershop.support.internal.service.impl;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.service.CatalogVectorSyncService;
import com.project.cybershop.support.internal.service.CustomerAiSupportService;
import com.project.cybershop.support.internal.service.GeminiEmbeddingService;
import com.project.cybershop.support.internal.service.StaffLiveSupportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class CustomerAiSupportServiceImpl implements CustomerAiSupportService {

    private final JdbcTemplate jdbcTemplate;
    private final GeminiEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final ObjectMapper tolerantMapper;
    private final StaffLiveSupportService liveSupportService;
    private final CatalogVectorSyncService catalogVectorSyncService;

    // Singleton HttpClient dùng lại cho toàn bộ request (BUG-06)
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.chatbox-model:gemini-3.6-flash}")
    private String modelName;

    @Autowired
    public CustomerAiSupportServiceImpl(JdbcTemplate jdbcTemplate, GeminiEmbeddingService embeddingService, ObjectMapper objectMapper, StaffLiveSupportService liveSupportService, CatalogVectorSyncService catalogVectorSyncService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
        this.liveSupportService = liveSupportService;
        this.catalogVectorSyncService = catalogVectorSyncService;
        this.tolerantMapper = objectMapper.copy()
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    @Override
    public AiChatResponse generateResponse(String userEmailOrNull, AiChatRequest request) {
        String message = request.getMessage().trim();
        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        CustomerContext customerCtx = fetchCustomerContext(userEmailOrNull);

        // 0. Nếu đang trong phiên Live Chat với nhân viên hỗ trợ (Staff Mode)
        if ("staff".equalsIgnoreCase(request.getMode())) {
            if (liveSupportService != null) {
                liveSupportService.recordCustomerMessage(
                        conversationId,
                        userEmailOrNull,
                        customerCtx.isMember() ? customerCtx.fullName() : "Khách hàng",
                        message
                );
            }
            return handleStaffSessionMessage(message, customerCtx, conversationId);
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
            return buildStaffConnectedResponse(conversationId);
        }
        if ("end_staff_session".equalsIgnoreCase(message)) {
            if (liveSupportService != null) {
                liveSupportService.closeSession(conversationId, null);
            }
            return buildStaffEndedResponse(conversationId);
        }

        // 2. Kiểm tra yêu cầu gặp nhân viên hoặc câu hỏi ngoài phạm vi nghiệp vụ tự động -> ĐỀ XUẤT kết nối
        if (isStaffRequest(message) || isOutOfScope(message)) {
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

        // 0. Giải đại từ tham chiếu đơn lẻ (Follow-up Pronoun) từ History hoặc URL hiện tại (Ví dụ: "tư vấn sản phẩm đó", "con này tốn điện không")
        String resolvedQuery = resolveContextualSearchQuery(message, request.getHistory(), request.getCurrentPath());

        // 1. Nhận diện mã đơn hàng động gõ trực tiếp trong câu chat (ví dụ "TZ-8219", "ORD-1234")
        String dynamicOrderContext = resolveDynamicOrderContext(message);

        // 2. Tìm kiếm trong Vector Database (RAG similarity search cho chính sách & hướng dẫn)
        String knowledgeContext = searchVectorKnowledge(resolvedQuery);

        // 3. Xử lý ngữ cảnh nối tiếp (Referral Query) ví dụ: "gửi tôi list linh kiện này trên shop"
        boolean isReferral = isReferralComponentQuery(message);
        String searchContext = resolvedQuery;
        if (isReferral && request.getHistory() != null && !request.getHistory().isEmpty()) {
            String bestBotContext = null;
            int maxComponents = 0;

            for (int i = request.getHistory().size() - 1; i >= 0; i--) {
                AiChatRequest.HistoryItem h = request.getHistory().get(i);
                if ("bot".equalsIgnoreCase(h.getSender()) && h.getText() != null && !h.getText().isBlank()) {
                    String botText = h.getText();
                    // Ưu tiên 1: Nếu tin nhắn gần nhất của bot đã có danh sách linh kiện thực tế (chứa 🔹 hoặc • **NHÓM**...—)
                    if (botText.contains("🔹") || (botText.contains("• **") && botText.contains("—"))) {
                        bestBotContext = botText;
                        break;
                    }

                    // Ưu tiên 2: Tìm tin nhắn tư vấn cấu hình có số lượng linh kiện phần cứng nhiều nhất trong lịch sử
                    List<String> comps = extractComponentLines(botText);
                    if (comps.size() > maxComponents) {
                        maxComponents = comps.size();
                        bestBotContext = botText;
                    }
                }
            }

            if (bestBotContext != null) {
                searchContext = bestBotContext;
            } else {
                for (int i = request.getHistory().size() - 1; i >= 0; i--) {
                    AiChatRequest.HistoryItem h = request.getHistory().get(i);
                    if ("bot".equalsIgnoreCase(h.getSender()) && h.getText() != null && !h.getText().isBlank()) {
                        searchContext = h.getText();
                        break;
                    }
                }
            }
        }

        // 5. Nâng cấp Referral Query: Tách từng dòng linh kiện từ tin nhắn trước của Bot để tìm Top 1 sản phẩm cho từng dòng (BUG-05 & Tinh chỉnh lọc Category + DDR4/DDR5)
        List<AiChatResponse.ProductSummary> products = new ArrayList<>();
        if (isReferral) {
            List<String> componentLines = extractComponentLines(searchContext);
            Set<Integer> addedProductIds = new HashSet<>();
            Set<String> seenCategories = new HashSet<>();
            List<AiChatResponse.ProductSummary> candidates = new ArrayList<>();

            if (catalogVectorSyncService != null) {
                for (String line : componentLines) {
                    List<AiChatResponse.ProductSummary> found = catalogVectorSyncService.searchProductsByVector(line, 3);
                    if (found == null || found.isEmpty()) {
                        CatalogSearchResult catRes = searchCatalogProducts(line);
                        if (!catRes.products().isEmpty()) {
                            found = catRes.products();
                        }
                    }
                    if (found != null && !found.isEmpty()) {
                        candidates.addAll(found);
                    }
                }
            }

            // Fallback nếu không tìm thấy gì từ component lines
            if (candidates.isEmpty() && catalogVectorSyncService != null) {
                List<AiChatResponse.ProductSummary> vectorProds = catalogVectorSyncService.searchProductsByVector(searchContext, 6);
                if (vectorProds != null && !vectorProds.isEmpty()) {
                    candidates.addAll(vectorProds);
                }
            }

            // Kiểm tra chuẩn Mainboard trong cấu hình là DDR4 hay DDR5 để đồng bộ RAM (Tránh lỗi cắm lệch chuẩn)
            boolean isDdr5Main = false;
            boolean isDdr4Main = false;
            for (AiChatResponse.ProductSummary p : candidates) {
                if ("MAINBOARD".equals(normalizeCategoryGroup(p.getCategory()))) {
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

            // Khử trùng lặp: Mỗi danh mục (VGA, CPU, MAINBOARD, RAM, STORAGE, PSU, COOLER, CASE) chỉ lấy tối đa 1 sản phẩm tốt nhất
            for (AiChatResponse.ProductSummary p : candidates) {
                String catGroup = normalizeCategoryGroup(p.getCategory());
                if (seenCategories.contains(catGroup)) {
                    continue;
                }

                // Nếu là RAM, ép chuẩn tương thích với Mainboard
                if ("RAM".equals(catGroup)) {
                    String ramText = (p.getName() + " " + (p.getVariantName() != null ? p.getVariantName() : "")).toLowerCase();
                    if (isDdr5Main && ramText.contains("ddr4")) {
                        continue; // Main DDR5 nhưng RAM là DDR4 -> bỏ qua để tìm RAM DDR5
                    }
                    if (isDdr4Main && ramText.contains("ddr5")) {
                        continue; // Main DDR4 nhưng RAM là DDR5 -> bỏ qua để tìm RAM DDR4
                    }
                }

                if (addedProductIds.add(p.getId())) {
                    seenCategories.add(catGroup);
                    products.add(p);
                }
            }

            // Nếu chưa có RAM do lọc khắt khe, tự động bổ sung 1 mẫu RAM tương thích chuẩn
            if (!seenCategories.contains("RAM") && catalogVectorSyncService != null) {
                String ramQuery = isDdr5Main ? "RAM DDR5" : "RAM DDR4";
                List<AiChatResponse.ProductSummary> rams = catalogVectorSyncService.searchProductsByVector(ramQuery, 2);
                if (rams != null) {
                    for (AiChatResponse.ProductSummary r : rams) {
                        if ("RAM".equals(normalizeCategoryGroup(r.getCategory())) && addedProductIds.add(r.getId())) {
                            seenCategories.add("RAM");
                            products.add(r);
                            break;
                        }
                    }
                }
            }
        } else if (isProductQuery(resolvedQuery)) {
            // Khi không phải referral nhưng là câu hỏi linh kiện/sản phẩm (hoặc câu hỏi follow-up đã resolve)
            if (catalogVectorSyncService != null) {
                List<AiChatResponse.ProductSummary> vectorProds = catalogVectorSyncService.searchProductsByVector(resolvedQuery, 6);
                if (vectorProds != null && !vectorProds.isEmpty()) {
                    products.addAll(vectorProds);
                }
            }
            CatalogSearchResult catalogResult = searchCatalogProducts(resolvedQuery);
            if (products.isEmpty() && !catalogResult.products().isEmpty()) {
                products.addAll(catalogResult.products());
            }

            // Nếu sản phẩm khớp nhất bị hết hàng (stock == 0), tự động lấy thêm 3 mẫu cùng danh mục đang còn hàng (stock > 0)
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
        }

        CatalogSearchResult catalogResult = searchCatalogProducts(resolvedQuery);
        String catalogTextContext = !products.isEmpty()
                ? buildCatalogContextFromSummaries(products)
                : catalogResult.textContext();

        // 6. Trả lời danh sách linh kiện vừa tư vấn (định dạng gọn gàng vừa vặn khung chat, không tràn màn hình):
        if (isReferral && !products.isEmpty()) {
            StringBuilder reply = new StringBuilder();
            boolean isCartIntent = message.toLowerCase().matches(".*(thêm.*giỏ|cho.*giỏ|vào giỏ|giỏ hàng|chốt|mua bộ này|lấy bộ này).*");

            if (isCartIntent) {
                reply.append("**CyberBot đã tổng hợp danh sách linh kiện thực tế từ cấu hình của bạn:**\n\n");
            } else {
                reply.append("Dạ, CyberBot đã tra cứu kho hàng và gửi bạn danh sách linh kiện thực tế có sẵn tại shop:\n\n");
            }

            double totalEst = 0;
            for (AiChatResponse.ProductSummary p : products) {
                if (p.getPrice() != null) totalEst += p.getPrice();
                reply.append("• **").append(normalizeCategoryGroup(p.getCategory())).append("**: ")
                        .append(p.getName()).append(" — **")
                        .append(MONEY_FMT.format(p.getPrice())).append("**\n");
            }
            if (totalEst > 0) {
                reply.append("\n**Tổng giá trị dự tính**: **").append(MONEY_FMT.format(totalEst)).append("**\n");
            }

            if (isCartIntent) {
                reply.append("\nBạn có thể bấm vào từng thẻ sản phẩm bên dưới để thêm vào giỏ hàng hoặc nhấn nút **Xem giỏ hàng** để tiến hành đặt hàng nhé!");
            } else {
                reply.append("\nBạn có thể bấm trực tiếp vào từng thẻ sản phẩm bên dưới để xem chi tiết hoặc thêm vào giỏ hàng!");
            }

            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType("info")
                    .products(products)
                    .quickActions(List.of(
                            new AiChatResponse.QuickAction("Xem giỏ hàng", "navigate", "/gio-hang"),
                            new AiChatResponse.QuickAction("Mở AI Build PC", "navigate", "/build-pc"),
                            new AiChatResponse.QuickAction("Nhờ chuyên viên lên đơn", "connect_staff", null)
                    ))
                    .build();
        }

        // 7. Kiểm tra điều hướng nhanh CHỈ KHI là câu xác nhận ngắn gọn (TUYỆT ĐỐI không chặn câu hỏi)
        AiChatResponse fastNavResponse = checkQuickNavigationIntent(message, request.getHistory(), customerCtx, conversationId);
        if (fastNavResponse != null) {
            if (fastNavResponse.getProducts().isEmpty() && !products.isEmpty() && isProductQuery(resolvedQuery)) {
                fastNavResponse.setProducts(products);
            }
            return fastNavResponse;
        }

        // 8. Gọi LLM sinh phản hồi cấu trúc kèm lịch sử hội thoại (với Fallback tự động gemini-2.5-flash)
        AiChatResponse finalResponse = null;
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                finalResponse = callGeminiWithFallback(message, resolvedQuery, request.getHistory(), customerCtx, knowledgeContext, catalogTextContext, conversationId);
            } catch (Exception e) {
                log.warn("Lỗi khi gọi Gemini LLM, chuyển sang chế độ phản hồi tự động thông minh: {}", e.getMessage());
            }
        }

        // 9. Fallback thông minh 12 nhánh nghiệp vụ chuyên sâu (không dump catalog context thô)
        if (finalResponse == null) {
            finalResponse = generateRuleBasedResponse(message, resolvedQuery, products, customerCtx, knowledgeContext, dynamicOrderContext, conversationId);
        }

        // 10. Gắn danh sách Product Cards để hiển thị UI Card trực quan CHỈ KHI có ý định hỏi sản phẩm (BUG-05)
        if (finalResponse.getProducts().isEmpty() && !products.isEmpty() && (isReferral || isProductQuery(resolvedQuery))) {
            finalResponse.setProducts(products);
        }

        return finalResponse;
    }

    private String normalizeCategoryGroup(String category) {
        if (category == null) return "KHÁC";
        String c = category.toLowerCase().trim();
        if (c.contains("cpu") || c.contains("vi xử lý") || c.contains("processor")) return "CPU";
        if (c.contains("vga") || c.contains("đồ họa") || c.contains("card") || c.contains("gpu")) return "VGA";
        if (c.contains("main") || c.contains("bo mạch") || c.contains("motherboard")) return "MAINBOARD";
        if (c.contains("ram") || c.contains("bộ nhớ") || c.contains("memory")) return "RAM";
        if (c.contains("ssd") || c.contains("ổ cứng") || c.contains("storage") || c.contains("hdd")) return "SSD";
        if (c.contains("nguồn") || c.contains("psu") || c.contains("power")) return "NGUỒN";
        if (c.contains("tản") || c.contains("cooler")) return "TẢN NHIỆT";
        if (c.contains("case") || c.contains("thùng") || c.contains("chassis") || c.contains("vỏ")) return "CASE";
        if (c.contains("màn") || c.contains("monitor")) return "MÀN HÌNH";
        return category.toUpperCase();
    }

    private List<String> extractComponentLines(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> lines = new ArrayList<>();

        // 1. Nếu tin nhắn trước đã là danh sách các sản phẩm thực tế của bot (chứa ký hiệu 🔹 hoặc • **TÊN_NHÓM**)
        if (text.contains("🔹") || (text.contains("• **") && text.contains("—"))) {
            for (String rawLine : text.split("\\r?\\n")) {
                String l = rawLine.trim();
                if (l.startsWith("🔹") || (l.startsWith("• **") && l.contains(":"))) {
                    String clean = l.replace("🔹", "").replace("**", "").replace("•", "").trim();
                    if (clean.contains(":")) {
                        clean = clean.substring(clean.indexOf(":") + 1).trim();
                    }
                    if (clean.contains("—")) {
                        clean = clean.substring(0, clean.indexOf("—")).trim();
                    }
                    if (clean.contains("(")) {
                        clean = clean.substring(0, clean.indexOf("(")).trim();
                    }
                    if (clean.length() > 3) {
                        lines.add(clean);
                    }
                }
            }
            if (!lines.isEmpty()) {
                return lines;
            }
        }

        // 2. Tách từ tin nhắn tư vấn cấu hình ban đầu
        for (String rawLine : text.split("\\r?\\n")) {
            String l = rawLine.trim();
            if (l.isBlank()) continue;
            String lower = l.toLowerCase();

            // Loại bỏ hoàn toàn các dòng văn xuôi mở đầu, dòng kết luận, tổng chi phí dự kiến
            if (lower.contains("tổng chi phí") || lower.contains("chi phí dự kiến") || lower.contains("bạn có muốn")
                    || lower.contains("chúc bạn") || lower.contains("xin chào") || lower.contains("tuyệt vời")
                    || lower.contains("lưu ý") || lower.contains("chưa bao gồm") || lower.contains("tự tay tùy chỉnh")
                    || lower.contains("trong dàn pc trên") || lower.contains("những phần chưa bao gồm")) {
                continue;
            }

            // Chỉ nhận các dòng thực sự là danh sách linh kiện:
            // Bắt đầu bằng 1., 2., *, -, • và có chứa dấu hai chấm ':'
            boolean isNumberedOrBullet = l.matches("^(\\d+[\\.\\)]|[\\*\\-•🔹]).*");
            boolean hasColon = l.contains(":");
            boolean hasHardwareKeyword = lower.matches(".*(cpu|ram|vga|gpu|card|mainboard|main|ssd|nguồn|psu|tản nhiệt|vỏ case|case|màn hình|intel|amd|ryzen|geforce|rtx).*");

            if (isNumberedOrBullet && hasColon && hasHardwareKeyword) {
                // Tách phần tên/mã linh kiện sau dấu hai chấm
                String prefix = l.substring(0, l.indexOf(":")).replaceAll("[^a-zA-Z0-9À-ỹ\\s]", "").trim();
                String afterColon = l.substring(l.indexOf(":") + 1).trim();

                // Bỏ bớt markdown **, giá tiền ví dụ (~10,990,000đ), và các ghi chú giải thích trong ngoặc
                String clean = afterColon.replaceAll("\\*\\*", "")
                        .replaceAll("\\(~?[0-9.,]+\\s*(đ|vnd|triệu|k).*?\\)", "")
                        .replaceAll("\\((bắt buộc|optix|tận dụng|đọc/ghi|tray|tiêu chuẩn).*?\\)", "")
                        .trim();

                // Nếu sau dấu hai chấm chỉ có số/dung lượng như "32GB (2x16GB) DDR4" mà chưa có tên nhóm
                if (prefix.toLowerCase().contains("ram") && !clean.toLowerCase().contains("ram")) {
                    clean = "RAM " + clean;
                } else if (prefix.toLowerCase().contains("nguồn") && !clean.toLowerCase().contains("nguồn") && !clean.toLowerCase().contains("psu")) {
                    clean = "Nguồn " + clean;
                } else if (prefix.toLowerCase().contains("mainboard") && !clean.toLowerCase().contains("mainboard") && !clean.toLowerCase().contains("main") && !clean.toLowerCase().contains("bo mạch")) {
                    clean = "Mainboard " + clean;
                }

                if (clean.contains("(") && (clean.contains("nhân") || clean.contains("luồng") || clean.contains("render"))) {
                    clean = clean.substring(0, clean.indexOf("(")).trim();
                }

                if (!clean.isBlank() && clean.length() >= 3) {
                    lines.add(clean);
                }
            }
        }

        if (lines.isEmpty()) {
            for (String rawLine : text.split("\\r?\\n")) {
                String l = rawLine.trim();
                if (l.toLowerCase().matches(".*(core i|ryzen|geforce|rtx|radeon|b760|z790|b650|ddr4|ddr5|nvme).*")) {
                    lines.add(l);
                }
            }
        }
        return lines;
    }

    private boolean isProductQuery(String message) {
        if (message == null) return false;
        String m = message.toLowerCase().trim();
        return m.matches(".*(ram|cpu|vga|gpu|card|mainboard|main|nguồn|psu|ssd|ổ cứng|màn hình|chuột|bàn phím|ddr|ghz|mhz|gb|tb|rtx|gtx|intel|amd|ryzen|core|i3|i5|i7|i9|asus|msi|gigabyte|corsair|kingston|linh kiện|mua máy|cấu hình|pc).*");
    }

    private record CustomerContext(
            Integer userId,
            String fullName,
            String phone,
            List<OrderSummary> recentOrders,
            List<WarrantySummary> activeWarranties
    ) {
        boolean isMember() {
            return userId != null;
        }
    }

    private record OrderSummary(
            Integer id,
            String code,
            String status,
            String deliveryType,
            String trackingCode,
            double finalAmount,
            String createdAt,
            String items
    ) {}

    private record WarrantySummary(
            Integer id,
            String serialNumber,
            String productName,
            String endDate,
            long daysRemaining,
            String status
    ) {}

    private CustomerContext fetchCustomerContext(String email) {
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return new CustomerContext(null, null, null, List.of(), List.of());
        }

        try {
            List<Map<String, Object>> userRows = jdbcTemplate.queryForList(
                    "SELECT id, full_name, phone FROM users WHERE email = ? LIMIT 1", email);
            if (userRows.isEmpty()) {
                return new CustomerContext(null, null, null, List.of(), List.of());
            }

            Integer userId = (Integer) userRows.get(0).get("id");
            String fullName = (String) userRows.get(0).get("full_name");
            String phone = (String) userRows.get(0).get("phone");

            String orderSql = """
                SELECT o.id, o.order_code, o.status, o.delivery_type, o.tracking_code, o.final_amount, o.created_at,
                       string_agg(oi.product_name || ' (x' || oi.quantity || ')', ', ') as items
                FROM orders o
                LEFT JOIN order_items oi ON o.id = oi.order_id
                WHERE o.user_id = ?
                GROUP BY o.id, o.order_code, o.status, o.delivery_type, o.tracking_code, o.final_amount, o.created_at
                ORDER BY o.created_at DESC
                LIMIT 3
            """;
            List<OrderSummary> orders = jdbcTemplate.query(orderSql, (rs, rowNum) -> new OrderSummary(
                    rs.getInt("id"),
                    rs.getString("order_code"),
                    rs.getString("status"),
                    rs.getString("delivery_type"),
                    rs.getString("tracking_code"),
                    rs.getDouble("final_amount"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().format(DATE_FMT) : "",
                    rs.getString("items") != null ? rs.getString("items") : "Linh kiện PC"
            ), userId);

            String warrantySql = """
                SELECT wc.id, wc.serial_number, oi.product_name, wc.end_date, wc.status
                FROM warranty_cards wc
                JOIN order_items oi ON wc.order_item_id = oi.id
                JOIN orders o ON oi.order_id = o.id
                WHERE o.user_id = ?
                ORDER BY wc.end_date DESC
                LIMIT 5
            """;
            LocalDateTime now = LocalDateTime.now();
            List<WarrantySummary> warranties = jdbcTemplate.query(warrantySql, (rs, rowNum) -> {
                LocalDateTime end = rs.getTimestamp("end_date") != null ? rs.getTimestamp("end_date").toLocalDateTime() : null;
                long days = (end != null) ? Math.max(0, Duration.between(now, end).toDays()) : 0;
                return new WarrantySummary(
                        rs.getInt("id"),
                        rs.getString("serial_number"),
                        rs.getString("product_name"),
                        end != null ? end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
                        days,
                        rs.getString("status")
                );
            }, userId);

            return new CustomerContext(userId, fullName, phone, orders, warranties);
        } catch (Exception e) {
            log.warn("Lỗi khi nạp dữ liệu khách hàng cho AI: {}", e.getMessage());
            return new CustomerContext(null, null, null, List.of(), List.of());
        }
    }

    private boolean isReferralComponentQuery(String message) {
        if (message == null) return false;
        String m = message.toLowerCase().trim();
        boolean hasListOrShow = m.matches(".*(list|danh sách|những|các|toàn bộ|tất cả|cho xem|gửi|show|xem|mua|link|thêm|cho.*vào|bỏ.*vào|chốt|lấy|order|cart|vào giỏ|giỏ hàng).*");
        boolean hasComponent = m.matches(".*(linh kiện|sản phẩm|cấu hình|món|phần cứng|dàn pc|dàn máy|bộ này|dàn này|món này|món trên).*");
        boolean hasReferralWords = m.matches(".*(này|đó|kia|bạn nói|vừa nói|vừa tư vấn|vừa nêu|vừa đề xuất|vừa báo|vừa rồi|trên shop|ở shop|tại shop|ở cửa hàng|trên|vào giỏ|giỏ hàng).*");

        return (hasListOrShow && hasComponent && hasReferralWords)
                || m.matches(".*(list.*linh kiện|danh sách.*linh kiện|linh kiện.*(bạn nói|vừa nói|này|đó|trên)|cấu hình.*(này|đó|vừa nói|trên)).*")
                || m.matches(".*(thêm.*(linh kiện|món|bộ|cấu hình).*(vào giỏ|giỏ hàng|trên)|cho.*(linh kiện|món).*(vào giỏ|giỏ hàng)|mua.*(bộ này|các món này|linh kiện trên)|chốt.*(bộ này|dàn này)).*")
                || m.matches(".*(thêm vào giỏ|cho vào giỏ|bỏ vào giỏ|mua luôn|chốt đơn|lấy bộ này|chốt bộ này|cho các món.*vào giỏ).*");
    }

    private String buildCatalogContextFromSummaries(List<AiChatResponse.ProductSummary> products) {
        if (products == null || products.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH LINH KIỆN HIỆN CÓ SẴN TRONG KHO CYBERSHOP (TÌM TỪ VECTOR DATABASE & KHO THỜI GIAN THỰC):\n");
        for (AiChatResponse.ProductSummary p : products) {
            sb.append("- ").append(p.getName())
              .append(" (Phiên bản: ").append(p.getVariantName() != null ? p.getVariantName() : "Tiêu chuẩn").append(")")
              .append(" | Thương hiệu: ").append(p.getBrand() != null ? p.getBrand() : "Chính hãng")
              .append(" | Phân loại: ").append(p.getCategory() != null ? p.getCategory() : "Linh kiện")
              .append(" | Giá bán: ").append(p.getPrice() != null ? MONEY_FMT.format(p.getPrice()) : "Liên hệ")
              .append(" | Tồn kho: ").append(p.getStock() != null ? p.getStock() : 0)
              .append(" | Link: /san-pham/").append(p.getId());
            if (p.getDescription() != null && !p.getDescription().isBlank()) {
                sb.append(" | Thông số kỹ thuật: ").append(p.getDescription().trim());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String searchVectorKnowledge(String query) {
        try {
            String vecStr = embeddingService.embedText(query);
            // Sửa triệt để BUG-01 & BUG-02: Thêm ngưỡng 1 - (embedding <=> ?::vector) >= 0.58 và truyền đủ 3 tham số vecStr
            String sql = """
                SELECT doc_category, doc_title, chunk_content, 1 - (embedding <=> ?::vector) as similarity
                FROM cyberbot_knowledge_vectors
                WHERE doc_category != 'PRODUCT_CATALOG'
                  AND 1 - (embedding <=> ?::vector) >= 0.58
                ORDER BY embedding <=> ?::vector
                LIMIT 3
            """;
            List<String> chunks = jdbcTemplate.query(sql, (rs, rowNum) ->
                    "### " + rs.getString("doc_title") + "\n" + rs.getString("chunk_content"), vecStr, vecStr, vecStr);

            return String.join("\n\n", chunks);
        } catch (Exception e) {
            log.warn("Lỗi khi tìm kiếm Vector DB: {}", e.getMessage());
            return "";
        }
    }

    private record CatalogSearchResult(
            String textContext,
            List<AiChatResponse.ProductSummary> products
    ) {}

    private record PriceAndSortFilter(Double minPrice, Double maxPrice, String orderByClause, String categoryHint) {}

    private boolean isFollowUpPronounQuery(String message) {
        if (message == null) return false;
        String m = message.toLowerCase().trim();
        return m.matches(".*\\b(đó|này|kia|ấy|nó|vừa rồi|ở trên|phía trên|con đầu|mẫu đầu|con thứ 2|con thứ hai|mẫu này|sản phẩm đó|sản phẩm này|con đó|con này|mã đó|mã này|rẻ hơn|mạnh hơn|tốt hơn|thay thế|đi kèm|tương thích)\\b.*")
                || m.matches("^(tư vấn|đánh giá|chi tiết|thông số|giá|bảo hành|tồn kho|còn hàng)\\s*(sản phẩm|linh kiện|mẫu|con|mã)?\\s*(đó|này|kia|trên|ấy)?\\??$");
    }

    private String resolveContextualSearchQuery(String message, List<AiChatRequest.HistoryItem> history, String currentPath) {
        if (message == null || message.isBlank()) return "";

        // 1. Nếu khách đang đứng trực tiếp trên trang chi tiết sản phẩm (/san-pham/{id}) và hỏi "con này / sản phẩm này"
        if (currentPath != null && currentPath.matches("^/san-pham/\\d+.*") && isFollowUpPronounQuery(message)) {
            try {
                String idStr = currentPath.replaceAll("^/san-pham/(\\d+).*", "$1");
                int pid = Integer.parseInt(idStr);
                List<String> names = jdbcTemplate.queryForList("SELECT name FROM products WHERE id = ? LIMIT 1", String.class, pid);
                if (!names.isEmpty()) {
                    return names.get(0) + " - " + message;
                }
            } catch (Exception ignored) {}
        }

        if (!isFollowUpPronounQuery(message) || history == null || history.isEmpty()) {
            return message;
        }

        // Nếu câu hiện tại đã nêu đích danh một mã linh kiện mới (ví dụ "i5 13400f", "epyc 9575f", "rtx 5060") thì giữ nguyên
        if (message.toLowerCase().matches(".*\\b(i[3579][\\s\\-]\\d{4,5}|ryzen\\s*[3579]|epyc\\s*\\d+|threadripper|rtx\\s*\\d{4}|rx\\s*\\d{4}|gtx\\s*\\d{4}|b760|z790|b650|x670|x870|h610|a520)\\b.*")) {
            return message;
        }

        // 2. Quét ngược lịch sử để tìm sản phẩm vừa được nhắc đến ở lượt trước
        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatRequest.HistoryItem item = history.get(i);
            if (item.getText() == null || item.getText().isBlank()) continue;
            String text = item.getText().trim();

            // Ưu tiên lấy từ câu hỏi gần nhất của User có chứa tên sản phẩm
            if ("user".equalsIgnoreCase(item.getSender()) && !isFollowUpPronounQuery(text) && isProductQuery(text)) {
                String cleanedUserProduct = text.replaceAll("(?i)^(tư vấn|cho hỏi|kiểm tra|xem|đánh giá|mua|tìm|sản phẩm|linh kiện)\\s+", "").trim();
                return cleanedUserProduct + " - " + message;
            }

            // Hoặc lấy sản phẩm đầu tiên được liệt kê trong tin nhắn của Bot liền trước
            if ("bot".equalsIgnoreCase(item.getSender())) {
                for (String line : text.split("\\r?\\n")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("### Tư vấn chi tiết:")) {
                        String prodName = trimmed.replace("### Tư vấn chi tiết:", "").replace("**", "").trim();
                        if (!prodName.isEmpty()) return prodName + " - " + message;
                    }
                    if (trimmed.startsWith("- ") || trimmed.startsWith("• ") || trimmed.startsWith("🔹") || trimmed.startsWith("* ")) {
                        String cleaned = trimmed.replaceAll("^[\\-•🔹\\*]+\\s*", "")
                                .replaceAll("\\*\\*", "")
                                .replaceAll("\\(Phiên bản:.*?\\)", "")
                                .split("\\|")[0]
                                .split("—")[0]
                                .trim();
                        if (cleaned.length() >= 4 && !cleaned.toLowerCase().matches("^(tra cứu|kiểm tra|tư vấn|giải đáp|phân loại|giá niêm yết|tình trạng|đánh giá|điểm nổi bật|thời gian|đồng kiểm|thanh toán|1 đổi 1|linh kiện).*")) {
                            return cleaned + " - " + message;
                        }
                    }
                }
            }
        }
        return message;
    }

    private String resolveDynamicOrderContext(String message) {
        if (message == null || message.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b(TZ-[A-Za-z0-9]+|ORD-[A-Za-z0-9]+|DH[A-Za-z0-9]+)\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(message);
        if (m.find()) {
            String code = m.group(1);
            try {
                String sql = """
                    SELECT o.id, o.order_code, o.status, o.delivery_type, o.tracking_code, o.final_amount, o.created_at,
                           string_agg(oi.product_name || ' (x' || oi.quantity || ')', ', ') as items
                    FROM orders o
                    LEFT JOIN order_items oi ON o.id = oi.order_id
                    WHERE UPPER(o.order_code) = UPPER(?)
                    GROUP BY o.id, o.order_code, o.status, o.delivery_type, o.tracking_code, o.final_amount, o.created_at
                    LIMIT 1
                """;
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, code);
                if (!rows.isEmpty()) {
                    Map<String, Object> r = rows.get(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append("* **Mã đơn hàng**: `").append(r.get("order_code")).append("`\n");
                    sb.append("* **Trạng thái**: **").append(mapStatusVietnamese((String) r.get("status"))).append("**\n");
                    if (r.get("tracking_code") != null) {
                        sb.append("* **Mã vận đơn GHN**: `").append(r.get("tracking_code")).append("`\n");
                    }
                    if (r.get("final_amount") != null) {
                        sb.append("* **Tổng thanh toán**: **").append(MONEY_FMT.format(((Number) r.get("final_amount")).doubleValue())).append("**\n");
                    }
                    if (r.get("items") != null) {
                        sb.append("* **Sản phẩm trong đơn**: ").append(r.get("items")).append("\n");
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                log.warn("Lỗi tra cứu đơn theo mã tự do: {}", e.getMessage());
            }
        }
        return null;
    }

    private PriceAndSortFilter parsePriceAndSortFilter(String msgLower) {
        Double min = null;
        Double max = null;
        String orderBy = "pv.stock_quantity DESC, pv.price ASC";

        // Bắt khoảng giá: "từ X đến Y triệu" hoặc "X - Y triệu"
        java.util.regex.Matcher rangeMatcher = java.util.regex.Pattern
                .compile("(\\d+(?:[.,]\\d+)?)\\s*(?:đến|tới|\\-)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|củ)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(msgLower);
        if (rangeMatcher.find()) {
            double v1 = Double.parseDouble(rangeMatcher.group(1).replace(",", ".")) * 1_000_000;
            double v2 = Double.parseDouble(rangeMatcher.group(2).replace(",", ".")) * 1_000_000;
            min = Math.min(v1, v2);
            max = Math.max(v1, v2);
        } else {
            // Bắt "dưới X triệu" / "tối đa X triệu"
            java.util.regex.Matcher underMatcher = java.util.regex.Pattern
                    .compile("(?:dưới|tối đa|không quá|nhỏ hơn|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|củ)", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(msgLower);
            if (underMatcher.find()) {
                max = Double.parseDouble(underMatcher.group(1).replace(",", ".")) * 1_000_000;
            } else {
                // Bắt "trên X triệu"
                java.util.regex.Matcher overMatcher = java.util.regex.Pattern
                        .compile("(?:trên|từ|hơn|>)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|củ)", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(msgLower);
                if (overMatcher.find()) {
                    min = Double.parseDouble(overMatcher.group(1).replace(",", ".")) * 1_000_000;
                } else {
                    // Bắt "tầm / khoảng X triệu" (±25%)
                    java.util.regex.Matcher aroundMatcher = java.util.regex.Pattern
                            .compile("(?:tầm|khoảng|quanh|giá)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|củ)", java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(msgLower);
                    if (aroundMatcher.find()) {
                        double target = Double.parseDouble(aroundMatcher.group(1).replace(",", ".")) * 1_000_000;
                        min = target * 0.75;
                        max = target * 1.25;
                    }
                }
            }
        }

        if (msgLower.matches(".*(rẻ nhất|giá rẻ|tiết kiệm|thấp nhất|sinh viên|văn phòng giá rẻ).*")) {
            orderBy = "pv.price ASC";
        } else if (msgLower.matches(".*(mạnh nhất|cao cấp nhất|đắt nhất|khủng nhất|xịn nhất|top đầu|flagship|hi-end|high-end).*")) {
            orderBy = "pv.price DESC";
        }

        String catHint = null;
        if (msgLower.matches(".*\\b(cpu|vi xử lý|chip|core i|ryzen|epyc|threadripper)\\b.*")) catHint = "vi xử lý";
        else if (msgLower.matches(".*\\b(vga|card đồ họa|card màn hình|gpu|rtx|gtx|radeon|rx)\\b.*")) catHint = "card đồ họa";
        else if (msgLower.matches(".*\\b(mainboard|main|bo mạch)\\b.*")) catHint = "bo mạch chủ";
        else if (msgLower.matches(".*\\b(ram|bộ nhớ trong)\\b.*")) catHint = "ram";
        else if (msgLower.matches(".*\\b(ssd|ổ cứng|nvme|hdd)\\b.*")) catHint = "ổ cứng";
        else if (msgLower.matches(".*\\b(nguồn|psu)\\b.*")) catHint = "nguồn";
        else if (msgLower.matches(".*\\b(màn hình|monitor)\\b.*")) catHint = "màn hình";
        else if (msgLower.matches(".*\\b(vỏ case|thùng máy|case)\\b.*")) catHint = "case";
        else if (msgLower.matches(".*\\b(tản nhiệt|cooler|aio)\\b.*")) catHint = "tản nhiệt";

        return new PriceAndSortFilter(min, max, orderBy, catHint);
    }

    private CatalogSearchResult searchCatalogProducts(String message) {
        String msgLower = message.toLowerCase();
        boolean isHardwareQuery = msgLower.matches(".*(ram|cpu|vga|gpu|card|mainboard|main|nguồn|psu|ssd|màn|chuột|bàn phím|ddr|ghz|mhz|gb|tb|rtx|gtx|intel|amd|ryzen|core|i3|i5|i7|i9|asus|msi|gigabyte|corsair|kingston|epyc|threadripper).*");
        if (!isHardwareQuery) {
            return new CatalogSearchResult("", List.of());
        }

        try {
            PriceAndSortFilter filter = parsePriceAndSortFilter(msgLower);

            List<String> keywords = new ArrayList<>();
            String[] tokens = msgLower.replaceAll("[^a-zA-Z0-9À-ỹ\\s]", " ").split("\\s+");
            Set<String> ignore = Set.of(
                    "shop", "co", "khong", "nao", "hay", "cho", "hoi", "la", "va", "gia", "ban", "mua", "voi", "dung", "de",
                    "tu", "van", "san", "pham", "mau", "manh", "nhat", "re", "hien", "nay", "cua", "toi", "minh", "chiec",
                    "con", "cai", "loai", "tot", "tam", "duoi", "tren", "khoang", "trieu", "cung", "khi", "nhung"
            );
            for (String t : tokens) {
                if (t.length() >= 2 && !ignore.contains(t)) {
                    keywords.add(t);
                }
            }

            if (keywords.isEmpty() && filter.categoryHint() == null) {
                return new CatalogSearchResult("", List.of());
            }

            StringBuilder sql = new StringBuilder("""
                SELECT p.id as product_id, p.name as product_name, p.description as product_desc,
                       COALESCE(b.name, 'Chính hãng') as brand_name,
                       c.name as category_name,
                       pv.id as variant_id, pv.variant_name, pv.price, pv.original_price,
                       pv.stock_quantity as stock, pv.image_url
                FROM product_variants pv
                JOIN products p ON pv.product_id = p.id
                LEFT JOIN brands b ON p.brand_id = b.id
                JOIN categories c ON p.category_id = c.id
                WHERE UPPER(p.status) = 'ACTIVE'
            """);

            List<Object> params = new ArrayList<>();

            if (filter.minPrice() != null) {
                sql.append(" AND pv.price >= ?");
                params.add(filter.minPrice());
            }
            if (filter.maxPrice() != null) {
                sql.append(" AND pv.price <= ?");
                params.add(filter.maxPrice());
            }

            if (!keywords.isEmpty()) {
                sql.append(" AND (");
                int count = Math.min(keywords.size(), 4);
                for (int i = 0; i < count; i++) {
                    if (i > 0) sql.append(" OR ");
                    sql.append("LOWER(p.name) LIKE ? OR LOWER(pv.variant_name) LIKE ? OR LOWER(c.name) LIKE ? OR LOWER(c.slug) LIKE ?");
                    String pattern = "%" + keywords.get(i) + "%";
                    params.add(pattern);
                    params.add(pattern);
                    params.add(pattern);
                    params.add(pattern);
                }
                sql.append(")");
            } else if (filter.categoryHint() != null) {
                sql.append(" AND (LOWER(c.name) LIKE ? OR LOWER(p.name) LIKE ?)");
                String pattern = "%" + filter.categoryHint() + "%";
                params.add(pattern);
                params.add(pattern);
            }

            sql.append(" ORDER BY ").append(filter.orderByClause()).append(" LIMIT 6");

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            if (rows.isEmpty()) {
                return new CatalogSearchResult("TechZone hiện tại không tìm thấy linh kiện nào khớp với từ khóa tìm kiếm này.", List.of());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("DANH SÁCH LINH KIỆN HIỆN CÓ SẴN TRONG KHO CYBERSHOP LIÊN QUAN ĐẾN CÂU HỎI:\n");
            List<AiChatResponse.ProductSummary> products = new ArrayList<>();

            for (Map<String, Object> r : rows) {
                Integer pid = (Integer) r.get("product_id");
                Integer vid = (Integer) r.get("variant_id");
                String pName = (String) r.get("product_name");
                String vName = (String) r.get("variant_name");
                String cName = (String) r.get("category_name");
                String bName = (String) r.get("brand_name");
                String pDesc = (String) r.get("product_desc");
                Number price = (Number) r.get("price");
                Number origPrice = (Number) r.get("original_price");
                Number stock = (Number) r.get("stock");
                String img = (String) r.get("image_url");

                sb.append("- ").append(pName).append(" (Phiên bản: ").append(vName != null ? vName : "Tiêu chuẩn").append(")")
                  .append(" | Thương hiệu: ").append(bName != null ? bName : "Chính hãng")
                  .append(" | Giá bán: ").append(price != null ? MONEY_FMT.format(price.doubleValue()) : "Liên hệ")
                  .append(" | Tồn kho: ").append(stock != null ? stock : 0)
                  .append(" | Link: /san-pham/").append(pid);
                if (pDesc != null && !pDesc.isBlank()) {
                    sb.append(" | Thông số: ").append(pDesc.trim());
                }
                sb.append("\n");

                products.add(AiChatResponse.ProductSummary.builder()
                        .id(pid)
                        .variantId(vid)
                        .name(pName)
                        .brand(bName != null ? bName : "Chính hãng")
                        .description(pDesc)
                        .variantName(vName)
                        .category(cName)
                        .price(price != null ? price.doubleValue() : null)
                        .originalPrice(origPrice != null ? origPrice.doubleValue() : null)
                        .stock(stock != null ? stock.intValue() : 0)
                        .imageUrl(img)
                        .route("/san-pham/" + pid)
                        .build());
            }

            return new CatalogSearchResult(sb.toString(), products);
        } catch (Exception e) {
            log.warn("Lỗi khi tìm kiếm sản phẩm trong catalog: {}", e.getMessage());
            return new CatalogSearchResult("", List.of());
        }
    }

    private boolean isStaffRequest(String message) {
        String msgLower = message.toLowerCase().trim();
        return msgLower.matches(".*(gặp|kết nối|nói chuyện|chuyển sang|liên hệ|chat với)\\s+(nhân viên|staff|tư vấn viên|người thật|chăm sóc khách hàng|kỹ thuật viên|cskh).*")
                || msgLower.matches(".*(nhân viên đâu|có ai trực không|cần người hỗ trợ|gặp cskh|gặp staff|hotline|tổng đài|tư vấn trực tiếp).*");
    }

    private boolean isOutOfScope(String message) {
        String msgLower = message.toLowerCase().trim();
        return msgLower.matches(".*(hóa đơn đỏ|hóa đơn vat|hóa đơn công ty|mua sỉ|chiết khấu b2b|đại lý|hợp đồng mua bán|khiếu nại nhân viên|thái độ phục vụ|bồi thường thiệt hại|chuyển khoản nhầm).*");
    }

    private AiChatResponse buildStaffConnectedResponse(String conversationId) {
        AiChatResponse.StaffInfo staff = getAvailableOnlineStaff();
        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message("**Đã kết nối thành công với chuyên viên hỗ trợ!**\n\n"
                        + "* **Chuyên viên**: " + staff.getName() + " (" + staff.getRole() + ")\n"
                        + "* **Trạng thái**: Đang trực tuyến sẵn sàng hỗ trợ bạn.\n\n"
                        + "Chào bạn! Mình là " + staff.getName() + ", phụ trách hỗ trợ trực tuyến TechZone. Mình đã xem qua yêu cầu của bạn. Bạn cần hỗ trợ chi tiết về vấn đề gì ạ?")
                .messageType("staff_connected")
                .staffInfo(staff)
                .quickActions(List.of(
                        new AiChatResponse.QuickAction("Kết thúc & Quay lại CyberBot", "end_staff_session", null)
                ))
                .build();
    }

    private AiChatResponse buildStaffEndedResponse(String conversationId) {
        if (liveSupportService != null) {
            liveSupportService.closeSession(conversationId, null);
        }
        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message("Dạ, chuyên viên đã kết thúc phiên hỗ trợ. Cảm ơn bạn đã liên hệ TechZone! Chúc bạn một ngày tốt lành! CyberBot đã sẵn sàng tiếp tục đồng hành cùng bạn.")
                .messageType("staff_ended")
                .quickActions(List.of(
                        new AiChatResponse.QuickAction("Đơn hàng gần nhất", "ask", "Đơn hàng gần nhất của tôi thế nào?"),
                        new AiChatResponse.QuickAction("Kiểm tra bảo hành", "ask", "Linh kiện của tôi còn bảo hành bao lâu?"),
                        new AiChatResponse.QuickAction("Thử AI Build PC", "navigate", "/build-pc")
                ))
                .build();
    }

    private AiChatResponse.StaffInfo getAvailableOnlineStaff() {
        try {
            String sql = """
                SELECT u.id, u.full_name, u.email, u.phone, r.name as role_name
                FROM users u
                JOIN roles r ON u.role_id = r.id
                WHERE UPPER(r.name) IN ('STAFF', 'ADMIN')
                ORDER BY u.id ASC
                LIMIT 1
            """;
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
            if (!list.isEmpty()) {
                Map<String, Object> row = list.get(0);
                Integer id = (Integer) row.get("id");
                String name = (String) row.get("full_name");
                String email = (String) row.get("email");
                String phone = (String) row.get("phone");
                return AiChatResponse.StaffInfo.builder()
                        .id(id)
                        .name(name != null ? name : "Trần Hoàng Nam")
                        .role("Chuyên viên tư vấn & CSKH CyberShop")
                        .email(email != null ? email : "support@cybershop.vn")
                        .phone(phone != null ? phone : "1900 8888")
                        .avatar("👨‍💼")
                        .status("online")
                        .build();
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tìm kiếm nhân viên online: {}", e.getMessage());
        }

        return AiChatResponse.StaffInfo.builder()
                .id(1)
                .name("Trần Hoàng Nam")
                .role("Chuyên viên tư vấn & Kỹ thuật CyberShop")
                .email("support@cybershop.vn")
                .phone("1900 8888")
                .avatar("👨‍💼")
                .status("online")
                .build();
    }

    private AiChatResponse handleStaffSessionMessage(String message, CustomerContext ctx, String conversationId) {
        String msgLower = message.toLowerCase().trim();

        // Sửa triệt để BUG-07: Bỏ "cảm ơn", "xong rồi", "bye" khỏi regex đóng phiên.
        // Chỉ đóng phiên khi là lệnh rõ ràng "end_staff_session" hoặc đúng câu lệnh kết thúc chuẩn.
        if (msgLower.equals("end_staff_session") || msgLower.matches("^(kết thúc phiên|quay lại bot|ngắt kết nối)$")) {
            if (liveSupportService != null) {
                liveSupportService.closeSession(conversationId, null);
            }
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message("Dạ, chuyên viên đã kết thúc phiên hỗ trợ trực tiếp. Cảm ơn bạn đã liên hệ CyberShop! Chúc bạn một ngày tuyệt vời! CyberBot đã sẵn sàng tiếp tục đồng hành cùng bạn. 🤖")
                    .messageType("staff_ended")
                    .quickActions(List.of(
                            new AiChatResponse.QuickAction("📦 Đơn hàng gần nhất", "ask", "Đơn hàng gần nhất của tôi thế nào?"),
                            new AiChatResponse.QuickAction("🛡️ Kiểm tra bảo hành", "ask", "Linh kiện của tôi còn bảo hành bao lâu?"),
                            new AiChatResponse.QuickAction("🖥️ Thử AI Build PC", "navigate", "/build-pc")
                    ))
                    .build();
        }

        // Quy tắc bất biến số 2: Tuyệt đối không sinh tin nhắn máy móc nhại lại.
        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message("")
                .messageType("staff_sent")
                .staffInfo(getAvailableOnlineStaff())
                .build();
    }

    private AiChatResponse checkQuickNavigationIntent(String message, List<AiChatRequest.HistoryItem> history, CustomerContext ctx, String conversationId) {
        String msgLower = message.toLowerCase().trim();

        // 0. Lệnh kết nối hoặc kết thúc nhân viên hỗ trợ
        if (msgLower.equals("connect_staff")) {
            return buildStaffConnectedResponse(conversationId);
        }

        if (msgLower.equals("end_staff_session")) {
            return buildStaffEndedResponse(conversationId);
        }

        // 1. Lệnh điều hướng trực tiếp bằng từ khóa rõ ràng
        if (msgLower.matches(".*(xem|mở|kiểm tra|chi tiết)\\s+(đơn|đơn hàng|order).*") && ctx.isMember() && !ctx.recentOrders().isEmpty()) {
            OrderSummary latest = ctx.recentOrders().get(0);
            return buildNavigateResponse(conversationId,
                    "Đang đưa bạn đến trang chi tiết đơn hàng **" + latest.code() + "** ngay đây! 🚀",
                    "/don-hang/" + latest.code(),
                    "Mở chi tiết đơn hàng " + latest.code(),
                    "Trang bảo hành linh kiện", "/bao-hanh");
        }

        if (msgLower.matches(".*(xem|mở|kiểm tra|tra cứu)\\s+(bảo hành|bh).*")) {
            return buildNavigateResponse(conversationId,
                    "Đang đưa bạn đến trang tra cứu bảo hành ngay đây! 🛡️",
                    "/bao-hanh",
                    "Mở trang bảo hành", null, null);
        }

        if (msgLower.matches(".*(mở|vào|thử|đến)\\s+(build pc|ai build pc|tư vấn pc).*")) {
            return buildNavigateResponse(conversationId,
                    "Đang đưa bạn đến công cụ AI Build PC ngay đây! 🖥️",
                    "/build-pc",
                    "Mở công cụ AI Build PC", null, null);
        }

        // 2. Nếu là câu hỏi hoặc chứa từ khóa phần cứng/sản phẩm -> TUYỆT ĐỐI không can thiệp điều hướng nhanh
        boolean hasQuestionMark = msgLower.contains("?");
        boolean hasInquiryWords = msgLower.matches(".*(không|chưa|hả|sao|thế nào|thế|gì|nào|mấy|bao nhiêu|ram|cpu|vga|gpu|card|main|ssd|nguồn|psu|chuột|phím|màn|giá|bán|shop|cửa hàng).*");
        if (hasQuestionMark || hasInquiryWords) {
            return null;
        }

        // 3. Chỉ xét câu trả lời ĐỒNG Ý ngắn gọn (dưới 6 từ, không phải câu hỏi)
        String cleanMsg = msgLower.replaceAll("[,.!?]", " ").trim();
        String[] words = cleanMsg.split("\\s+");
        if (words.length > 6) {
            return null;
        }

        boolean isShortAffirmative = cleanMsg.matches("^(có|dạ có|ok|oke|okee|yes|yep|yeah|được|đồng ý|chuyển|mở|vào|đưa tôi|kết nối|gặp).*");
        if (!isShortAffirmative) {
            return null;
        }

        // 4. Tìm ngữ cảnh từ tin nhắn gần nhất của bot để biết khách đang "đồng ý" với điều gì
        String lastBotText = "";
        if (history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= 0; i--) {
                AiChatRequest.HistoryItem item = history.get(i);
                if ("bot".equalsIgnoreCase(item.getSender()) && item.getText() != null) {
                    lastBotText = item.getText().toLowerCase();
                    break;
                }
            }
        }

        if (lastBotText.isEmpty()) {
            return null;
        }

        if (lastBotText.contains("nhân viên tư vấn") || lastBotText.contains("kết nối bạn với nhân viên") || lastBotText.contains("staff") || lastBotText.contains("chuyên viên")) {
            if (liveSupportService != null) {
                liveSupportService.createOrOpenSession(
                        conversationId,
                        null,
                        ctx.isMember() ? ctx.fullName() : "Khách hàng",
                        ctx.phone(),
                        "Khách hàng đồng ý kết nối chuyên viên tư vấn",
                        history
                );
            }
            return buildStaffConnectedResponse(conversationId);
        }

        if (lastBotText.contains("bảo hành")) {
            return buildNavigateResponse(conversationId,
                    "Đang đưa bạn đến trang Bảo hành của bạn ngay đây!",
                    "/bao-hanh",
                    "Mở trang bảo hành", null, null);
        }

        if (lastBotText.contains("build pc")) {
            return buildNavigateResponse(conversationId,
                    "Đang đưa bạn đến công cụ AI Build PC ngay đây!",
                    "/build-pc",
                    "Mở công cụ AI Build PC", null, null);
        }

        if ((lastBotText.contains("đơn hàng") || lastBotText.contains("chi tiết đơn"))
                && ctx.isMember() && !ctx.recentOrders().isEmpty()) {
            OrderSummary latest = ctx.recentOrders().get(0);
            return buildNavigateResponse(conversationId,
                    "Đang đưa bạn đến trang chi tiết đơn hàng **" + latest.code() + "** ngay đây!",
                    "/don-hang/" + latest.code(),
                    "Mở chi tiết đơn hàng " + latest.code(),
                    "Trang bảo hành linh kiện", "/bao-hanh");
        }

        return null;
    }

    private AiChatResponse buildNavigateResponse(String conversationId, String message, String route, String primaryLabel, String secondaryLabel, String secondaryRoute) {
        List<AiChatResponse.QuickAction> actions = new ArrayList<>();
        actions.add(new AiChatResponse.QuickAction(primaryLabel, "navigate", route));
        if (secondaryLabel != null && secondaryRoute != null) {
            actions.add(new AiChatResponse.QuickAction(secondaryLabel, "navigate", secondaryRoute));
        }
        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(message)
                .messageType("navigate")
                .messageRoute(route)
                .quickActions(actions)
                .build();
    }

    /**
     * Gọi Gemini với cơ chế fallback tự động sang gemini-2.5-flash nếu model chính gặp lỗi HTTP 404/400 (BUG-06)
     */
    private AiChatResponse callGeminiWithFallback(String message, String resolvedQuery, List<AiChatRequest.HistoryItem> history, CustomerContext ctx, String knowledgeContext, String catalogContext, String conversationId) throws Exception {
        try {
            return executeGeminiCall(modelName, message, resolvedQuery, history, ctx, knowledgeContext, catalogContext, conversationId);
        } catch (Exception e) {
            log.warn("Gọi Gemini model '{}' thất bại ({}). Đang tự động thử lại với model dự phòng 'gemini-2.5-flash'...", modelName, e.getMessage());
            if (!"gemini-2.5-flash".equalsIgnoreCase(modelName)) {
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

    private AiChatResponse executeGeminiCall(String targetModel, String message, String resolvedQuery, List<AiChatRequest.HistoryItem> history, CustomerContext ctx, String knowledgeContext, String catalogContext, String conversationId) throws Exception {
        String systemInstruction = buildSystemPrompt(ctx, knowledgeContext, catalogContext);

        String jsonSchemaPrompt = """
            
            BẠN PHẢI TRẢ LỜI ĐÚNG ĐỊNH DẠNG JSON SAU ĐÂY (không bọc code fence markdown):
            {
              "message": "Nội dung câu trả lời súc tích, chuyên nghiệp chuẩn TechZone (hỗ trợ định dạng Markdown rõ ràng, không lạm dụng emoji)",
              "messageType": "info" HOẶC "proposal" HOẶC "navigate",
              "messageRoute": "/don-hang/{orderCode}" HOẶC "/bao-hanh" HOẶC "/build-pc" HOẶC "/san-pham/{productId}" HOẶC null,
              "quickActions": [
                {"label": "Tên nút gợi ý", "action": "navigate", "route": "/đường-dẫn"}
              ]
            }
            """;

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":generateContent?key=" + apiKey;

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
                        "temperature", 0.4
                )
        );

        String jsonPayload = objectMapper.writeValueAsString(body);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String rawResponse = httpResponse.body();

        if (httpResponse.statusCode() != 200) {
            throw new IllegalStateException("Google Gemini API trả về HTTP " + httpResponse.statusCode() + ": " + rawResponse);
        }

        JsonNode root = tolerantMapper.readTree(rawResponse);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText().trim();

        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();

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

    private String buildSystemPrompt(CustomerContext ctx, String knowledgeContext, String catalogContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            BẠN LÀ CYBERBOT - Trợ lý AI thông minh, chuyên nghiệp và am hiểu kỹ thuật của cửa hàng công nghệ TechZone.
            NHIỆM VỤ CỐT LÕI:
            1. Trả lời bằng tiếng Việt chuyên nghiệp, súc tích, chuẩn mực kỹ sư phần cứng TechZone. Hạn chế tối đa sử dụng emoji hoạt hình để đảm bảo giao diện lịch sự, đáng tin cậy.
            2. Trả lời đúng trọng tâm câu hỏi của khách hàng:
               - Khách hỏi về linh kiện / RAM / CPU / VGA / màn hình -> Dựa vào KHO LINH KIỆN THỰC TẾ của TechZone để trả lời chính xác thông số, tồn kho, giá bán và đề xuất nút xem sản phẩm (/san-pham/{id}).
               - Khách hỏi các sản phẩm ngoài danh mục (Laptop, Macbook, Điện thoại...) -> Lịch sự giải thích TechZone chuyên sâu vào linh kiện Máy tính để bàn PC và Máy trạm Workstation, không kinh doanh laptop/điện thoại; gợi ý cấu hình PC để bàn tương đương về ngân sách nhưng hiệu năng vượt trội.
               - Khách hỏi xử lý sự cố (Troubleshooting: Không lên hình, lỏng RAM, màn hình xanh BSOD, quá nhiệt) -> Chẩn đoán nguyên nhân kỹ thuật và hướng dẫn xử lý từng bước (cắm dây vào cổng VGA rời, lau chân RAM, kiểm tra đèn Debug LED, miễn phí vệ sinh tra keo tại shop).
               - Khách hỏi so sánh hoặc tính tương thích -> Phân tích rõ ràng sự khác biệt về socket (LGA1700 vs AM5), chuẩn RAM (DDR4 vs DDR5), công suất nguồn (PSU) và khuyến nghị mẫu phù hợp.
               - Khách hỏi về chính sách bảo hành, GHN, thanh toán -> Dựa vào TRI THỨC TỪ VECTOR DATABASE để giải đáp.
               - Khách hỏi về đơn hàng hoặc bảo hành cá nhân -> Dựa vào THÔNG TIN KHÁCH HÀNG (nếu đã đăng nhập).
            3. HỖ TRỢ ĐIỀU HƯỚNG THÔNG MINH (Smart Navigation):
               - Khi khách hỏi xem danh sách linh kiện trên shop (ví dụ: 'gửi tôi list linh kiện này', 'list những linh kiện bạn nói để tôi xem trên shop'...), bạn PHẢI trả lời chi tiết danh sách linh kiện trong khung chat, TUYỆT ĐỐI KHÔNG tự ý chuyển hướng sang '/build-pc'!
               - Khi khách hỏi về đơn hàng cá nhân, trả lời chi tiết và CHỦ ĐỘNG HỎI: 'Bạn có muốn mình đưa bạn tới trang chi tiết đơn hàng để theo dõi tiến trình không?' kèm nút hành động.
               - Khi khách hỏi bảo hành, đề xuất nút chuyển đến '/bao-hanh'.
               - Khi khách chủ động muốn tự mở công cụ build máy, đề xuất nút chuyển đến '/build-pc'.
               - Khi tư vấn được sản phẩm ưng ý, đề xuất nút chuyển đến '/san-pham/{id}'.
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
            sb.append("Khách hàng: ").append(ctx.fullName()).append(" (SĐT: ").append(ctx.phone()).append(")\n");
            sb.append("DANH SÁCH ĐƠN HÀNG GẦN ĐÂY CỦA KHÁCH:\n");
            for (OrderSummary o : ctx.recentOrders()) {
                sb.append("- Đơn: ").append(o.code())
                        .append(" | Trạng thái: ").append(mapStatusVietnamese(o.status()))
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

    private AiChatResponse buildSmartProductAdvisory(String message, String resolvedQuery, List<AiChatResponse.ProductSummary> products, String conversationId) {
        StringBuilder reply = new StringBuilder();
        List<AiChatResponse.QuickAction> actions = new ArrayList<>();
        String messageType = "info";
        String messageRoute = null;

        if (products == null || products.isEmpty()) {
            reply.append("Dạ, CyberBot đã tra cứu kho linh kiện TechZone nhưng hiện tại chưa tìm thấy linh kiện khớp chính xác với yêu cầu: **").append(resolvedQuery).append("**.\n\n");
            reply.append("* Bạn có thể kiểm tra lại tên mã hoặc liên hệ chuyên viên để shop hỗ trợ kiểm tra nguồn hàng.\n");
            reply.append("* Bạn cũng có thể dùng công cụ **AI Build PC** để chọn các linh kiện có sẵn tương thích 100%!");
            actions.add(new AiChatResponse.QuickAction("Mở AI Build PC", "navigate", "/build-pc"));
            actions.add(new AiChatResponse.QuickAction("Nhờ chuyên viên hỗ trợ", "connect_staff", null));
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType(messageType)
                    .quickActions(actions)
                    .build();
        }

        AiChatResponse.ProductSummary primary = products.get(0);
        boolean isOutOfStock = (primary.getStock() == null || primary.getStock() <= 0);

        if (isOutOfStock) {
            reply.append("### Thông tin sản phẩm: **").append(primary.getName()).append("**\n\n");
            reply.append("* **Thương hiệu**: ").append(primary.getBrand() != null ? primary.getBrand() : "Chính hãng").append("\n");
            reply.append("* **Giá tham khảo**: ").append(primary.getPrice() != null ? MONEY_FMT.format(primary.getPrice()) : "Liên hệ").append("\n");
            reply.append("* **Tình trạng kho**: ⚠️ **Tạm hết hàng tại kho**\n");
            if (primary.getDescription() != null && !primary.getDescription().isBlank()) {
                reply.append("* **Thông số kỹ thuật**: ").append(primary.getDescription().trim()).append("\n");
            }
            reply.append("\n💡 **Gợi ý linh kiện thay thế đang CÒN HÀNG tại TechZone:**\n\n");

            int altCount = 0;
            for (int i = 1; i < products.size() && altCount < 3; i++) {
                AiChatResponse.ProductSummary alt = products.get(i);
                if (alt.getStock() != null && alt.getStock() > 0) {
                    altCount++;
                    reply.append(altCount).append(". **").append(alt.getName()).append("**")
                            .append(" (").append(alt.getVariantName() != null ? alt.getVariantName() : "Tiêu chuẩn").append(")")
                            .append(" — **").append(alt.getPrice() != null ? MONEY_FMT.format(alt.getPrice()) : "Liên hệ").append("**")
                            .append(" (Sẵn: ").append(alt.getStock()).append(" chiếc)\n");
                    if (alt.getDescription() != null && !alt.getDescription().isBlank()) {
                        reply.append("   - *Thông số*: ").append(alt.getDescription().trim()).append("\n");
                    }
                }
            }

            if (altCount == 0) {
                reply.append("Hiện các sản phẩm tương tự cùng phân khúc cũng đang được cập nhật thêm. Bạn có thể nhấn 'Nhờ chuyên viên lên đơn' để kiểm tra kho tổng nhé!\n");
            } else {
                reply.append("\nBạn có thể bấm trực tiếp vào từng thẻ sản phẩm bên dưới để xem chi tiết hoặc thêm vào giỏ hàng!");
            }

            actions.add(new AiChatResponse.QuickAction("Mở AI Build PC", "navigate", "/build-pc"));
            actions.add(new AiChatResponse.QuickAction("Nhờ chuyên viên lên đơn", "connect_staff", null));
        } else {
            reply.append("### Tư vấn chi tiết: **").append(primary.getName()).append("**\n\n");
            reply.append("* **Thương hiệu**: ").append(primary.getBrand() != null ? primary.getBrand() : "Chính hãng").append("\n");
            reply.append("* **Phân loại**: ").append(primary.getCategory() != null ? primary.getCategory() : "Linh kiện").append("\n");
            reply.append("* **Giá niêm yết**: **").append(primary.getPrice() != null ? MONEY_FMT.format(primary.getPrice()) : "Liên hệ").append("**");
            if (primary.getOriginalPrice() != null && primary.getOriginalPrice() > (primary.getPrice() != null ? primary.getPrice() : 0)) {
                reply.append(" *(Giá gốc: ~~").append(MONEY_FMT.format(primary.getOriginalPrice())).append("~~)*");
            }
            reply.append("\n* **Tình trạng kho**: ✅ **Còn hàng** (Sẵn ").append(primary.getStock()).append(" sản phẩm)\n");
            reply.append("* **Chế độ bảo hành**: 36 tháng chính hãng, 1 đổi 1 trong 30 ngày đầu nếu phát sinh lỗi phần cứng NSX.\n");
            if (primary.getDescription() != null && !primary.getDescription().isBlank()) {
                reply.append("* **Thông số nổi bật**: ").append(primary.getDescription().trim()).append("\n");
            }

            if (products.size() > 1) {
                reply.append("\n**Các phiên bản / lựa chọn liên quan khác:**\n");
                for (int i = 1; i < Math.min(products.size(), 4); i++) {
                    AiChatResponse.ProductSummary other = products.get(i);
                    reply.append("• **").append(other.getName()).append("**: ")
                            .append(other.getPrice() != null ? MONEY_FMT.format(other.getPrice()) : "Liên hệ")
                            .append(other.getStock() != null && other.getStock() > 0 ? " (Sẵn hàng)" : " (Hết hàng)")
                            .append("\n");
                }
            }

            reply.append("\n👉 Bạn có thể nhấn vào thẻ sản phẩm bên dưới để xem đầy đủ thông số kỹ thuật hoặc bấm **Thêm vào giỏ hàng**!");

            messageRoute = "/san-pham/" + primary.getId();
            actions.add(new AiChatResponse.QuickAction("Xem chi tiết sản phẩm", "navigate", messageRoute));
            actions.add(new AiChatResponse.QuickAction("Mở AI Build PC", "navigate", "/build-pc"));
            actions.add(new AiChatResponse.QuickAction("Nhờ chuyên viên lên đơn", "connect_staff", null));
        }

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType(messageType)
                .messageRoute(messageRoute)
                .products(products)
                .quickActions(actions)
                .build();
    }

    private AiChatResponse buildCompatibilityAdvisory(String message, String conversationId) {
        StringBuilder reply = new StringBuilder();
        reply.append("### Hướng dẫn kiểm tra tính tương thích phần cứng TechZone:\n\n");
        reply.append("Khi tự chọn hoặc nâng cấp linh kiện PC, bạn cần lưu ý 4 quy tắc tương thích cốt lõi sau:\n\n");
        reply.append("1. **CPU & Mainboard (Socket)**:\n");
        reply.append("   - **Intel Gen 12 / 13 / 14**: Dùng socket **LGA1700** (Mainboard H610, B760, Z790).\n");
        reply.append("   - **Intel Core Ultra (Gen 15 / Arrow Lake)**: Dùng socket **LGA1851** (Mainboard Z890, B860).\n");
        reply.append("   - **AMD Ryzen 7000 / 8000 / 9000**: Dùng socket **AM5** (Mainboard B650, X670, X870).\n");
        reply.append("   - **AMD Ryzen 3000 / 5000**: Dùng socket **AM4** (Mainboard B450, B550, A520).\n");
        reply.append("   *(Tuyệt đối không lắp chéo socket vì sẽ gãy chân socket hoặc cong CPU)*.\n\n");

        reply.append("2. **Mainboard & RAM (DDR4 vs DDR5)**:\n");
        reply.append("   - Mainboard chỉ hỗ trợ **DUY NHẤT một chuẩn RAM** (hoặc DDR4 hoặc DDR5).\n");
        reply.append("   - Khe cắm DDR4 và DDR5 có rãnh khóa (notch) lệch nhau, không thể cắm chung.\n\n");

        reply.append("3. **Công suất nguồn (PSU)**:\n");
        reply.append("   - Card RTX 3050 / RTX 4060 / RX 6600: Cần nguồn tối thiểu **550W – 650W 80 Plus**.\n");
        reply.append("   - Card RTX 4070 / 4070 Super / RX 7700 XT: Cần nguồn tối thiểu **650W – 750W 80 Plus Bronze/Gold**.\n");
        reply.append("   - Card RTX 4080 / 4090: Cần nguồn tối thiểu **850W – 1000W chuẩn ATX 3.0 (PCIe 5.0 12VHPWR)**.\n\n");

        reply.append("4. **Kích thước Card VGA & Vỏ Case / Tản nhiệt**:\n");
        reply.append("   - Đo chiều dài card đồ họa (GPU Clearance) xem vừa kích thước vỏ case không.\n");
        reply.append("   - Tản nhiệt nước AIO 240mm/360mm cần case có vị trí bắt rad nóc hoặc mặt trước phù hợp.\n\n");

        reply.append("👉 Để đảm bảo chuẩn xác 100% không lo xung đột, bạn có thể sử dụng công cụ **AI Build PC** của TechZone để hệ thống tự động kiểm tra tương thích thời gian thực!");

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType("proposal")
                .messageRoute("/build-pc")
                .quickActions(List.of(
                        new AiChatResponse.QuickAction("Mở AI Build PC kiểm tra", "navigate", "/build-pc"),
                        new AiChatResponse.QuickAction("Nhờ chuyên viên kiểm tra", "connect_staff", null)
                ))
                .build();
    }

    private AiChatResponse buildComparisonAdvisory(String message, List<AiChatResponse.ProductSummary> products, String conversationId) {
        StringBuilder reply = new StringBuilder();
        List<AiChatResponse.QuickAction> actions = new ArrayList<>();

        if (products.size() < 2) {
            return buildSmartProductAdvisory(message, message, products, conversationId);
        }

        AiChatResponse.ProductSummary p1 = products.get(0);
        AiChatResponse.ProductSummary p2 = products.get(1);

        reply.append("### So sánh đối chiếu linh kiện TechZone:\n\n");
        reply.append("| Tiêu chí so sánh | **").append(p1.getName()).append("** | **").append(p2.getName()).append("** |\n");
        reply.append("| :--- | :--- | :--- |\n");
        reply.append("| **Thương hiệu** | ").append(p1.getBrand() != null ? p1.getBrand() : "Chính hãng").append(" | ").append(p2.getBrand() != null ? p2.getBrand() : "Chính hãng").append(" |\n");
        reply.append("| **Phiên bản** | ").append(p1.getVariantName() != null ? p1.getVariantName() : "Tiêu chuẩn").append(" | ").append(p2.getVariantName() != null ? p2.getVariantName() : "Tiêu chuẩn").append(" |\n");
        reply.append("| **Giá niêm yết** | **").append(p1.getPrice() != null ? MONEY_FMT.format(p1.getPrice()) : "Liên hệ").append("** | **").append(p2.getPrice() != null ? MONEY_FMT.format(p2.getPrice()) : "Liên hệ").append("** |\n");
        reply.append("| **Tình trạng kho** | ").append(p1.getStock() != null && p1.getStock() > 0 ? "Còn " + p1.getStock() + " chiếc" : "Tạm hết hàng")
                .append(" | ").append(p2.getStock() != null && p2.getStock() > 0 ? "Còn " + p2.getStock() + " chiếc" : "Tạm hết hàng").append(" |\n");

        if (p1.getDescription() != null || p2.getDescription() != null) {
            String d1 = p1.getDescription() != null ? p1.getDescription().replace("\n", " ") : "Thông số tiêu chuẩn";
            String d2 = p2.getDescription() != null ? p2.getDescription().replace("\n", " ") : "Thông số tiêu chuẩn";
            reply.append("| **Thông số tóm tắt** | ").append(d1).append(" | ").append(d2).append(" |\n");
        }

        reply.append("\n💡 **Đánh giá & Khuyến nghị lựa chọn:**\n\n");
        if (p1.getPrice() != null && p2.getPrice() != null) {
            AiChatResponse.ProductSummary cheaper = p1.getPrice() <= p2.getPrice() ? p1 : p2;
            AiChatResponse.ProductSummary higher = p1.getPrice() > p2.getPrice() ? p1 : p2;
            reply.append("* **Lựa chọn tối ưu ngân sách (P/P)**: **").append(cheaper.getName()).append("** giúp tiết kiệm chi phí mà vẫn đáp ứng rất tốt nhu cầu cơ bản đến trung bình khá.\n");
            reply.append("* **Lựa chọn tối đa hiệu năng / đầu tư dài hạn**: **").append(higher.getName()).append("** mang lại sức mạnh xử lý vượt trội, linh hoạt cho tác vụ nặng hoặc đồ họa nâng cao.\n");
        } else {
            reply.append("* Tùy thuộc vào nhu cầu cụ thể (chơi game, dựng hình 3D hay render video) và mức ngân sách tổng thể của dàn máy, bạn có thể cân nhắc giữa 2 mẫu trên.\n");
        }

        reply.append("\nBạn có thể bấm vào thẻ sản phẩm bên dưới để xem chi tiết từng mẫu hoặc nhấn nút để nhờ chuyên viên tư vấn chi tiết hơn!");

        actions.add(new AiChatResponse.QuickAction("Xem " + p1.getName(), "navigate", "/san-pham/" + p1.getId()));
        actions.add(new AiChatResponse.QuickAction("Xem " + p2.getName(), "navigate", "/san-pham/" + p2.getId()));
        actions.add(new AiChatResponse.QuickAction("Mở AI Build PC", "navigate", "/build-pc"));

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType("info")
                .products(products)
                .quickActions(actions)
                .build();
    }

    private AiChatResponse generateRuleBasedResponse(
            String message,
            String resolvedQuery,
            List<AiChatResponse.ProductSummary> products,
            CustomerContext ctx,
            String knowledgeContext,
            String dynamicOrderContext,
            String conversationId) {

        String msgLower = message.toLowerCase();
        List<AiChatResponse.QuickAction> actions = new ArrayList<>();
        String messageType = "info";
        String messageRoute = null;
        StringBuilder reply = new StringBuilder();

        // Branch 1: Tra cứu mã đơn hàng cụ thể xuất hiện trong tin nhắn (dynamicOrderContext)
        if (dynamicOrderContext != null && !dynamicOrderContext.isBlank()) {
            reply.append("Dạ, CyberBot đã tra cứu thông tin đơn hàng theo mã của bạn:\n\n");
            reply.append(dynamicOrderContext).append("\n");
            reply.append("👉 **Bạn có muốn mình đưa bạn tới trang chi tiết đơn hàng để theo dõi tiến trình giao hàng của shipper không?**");

            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\b(TZ-[A-Za-z0-9]+|ORD-[A-Za-z0-9]+|DH[A-Za-z0-9]+)\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(message);
            String foundCode = m.find() ? m.group(1).toUpperCase() : null;

            messageType = "proposal";
            if (foundCode != null) {
                messageRoute = "/don-hang/" + foundCode;
                actions.add(new AiChatResponse.QuickAction("Xem chi tiết đơn " + foundCode, "navigate", messageRoute));
            }
            actions.add(new AiChatResponse.QuickAction("Trang bảo hành linh kiện", "navigate", "/bao-hanh"));
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType(messageType)
                    .messageRoute(messageRoute)
                    .quickActions(actions)
                    .build();
        }

        // Branch 2: Tra cứu đơn hàng của Member
        boolean isOrderQuery = msgLower.contains("đơn") || msgLower.contains("order") || msgLower.contains("vận đơn") || (msgLower.contains("tiến độ") && msgLower.contains("giao"));
        if (isOrderQuery) {
            if (ctx.isMember() && !ctx.recentOrders().isEmpty()) {
                OrderSummary o = ctx.recentOrders().get(0);
                reply.append("Dạ, CyberBot kiểm tra thấy đơn hàng gần nhất của bạn là **").append(o.code()).append("**:\n\n");
                reply.append("* **Linh kiện trong đơn**: ").append(o.items()).append("\n");
                reply.append("* **Trạng thái**: ").append(mapStatusVietnamese(o.status())).append("\n");
                if (o.trackingCode() != null && !o.trackingCode().isBlank()) {
                    reply.append("* **Mã vận đơn GHN**: `").append(o.trackingCode()).append("` (Giao Hàng Nhanh Express)\n");
                }
                reply.append("* **Tổng thanh toán**: ").append(MONEY_FMT.format(o.finalAmount())).append("\n\n");
                reply.append("👉 **Bạn có muốn mình đưa bạn tới trang chi tiết đơn hàng để theo dõi tiến trình bưu tá không?**");

                messageType = "proposal";
                messageRoute = "/don-hang/" + o.code();
                actions.add(new AiChatResponse.QuickAction("Xem chi tiết đơn " + o.code(), "navigate", messageRoute));
                actions.add(new AiChatResponse.QuickAction("Kiểm tra bảo hành", "navigate", "/bao-hanh"));
            } else if (!ctx.isMember()) {
                reply.append("Để tra cứu tình trạng đơn hàng hoặc kiểm tra tiến độ giao linh kiện của bạn, bạn vui lòng **Đăng nhập** tài khoản nhé! 😊\n\n");
                reply.append("TechZone đồng bộ lộ trình giao hàng trực tiếp qua GHN Express, bạn sẽ xem được ngay vị trí bưu kiện của mình.");
                actions.add(new AiChatResponse.QuickAction("Đăng nhập ngay", "navigate", "/dang-nhap"));
            } else {
                reply.append("Hiện tại bạn chưa có đơn hàng nào tại TechZone. Bạn có muốn tham khảo các mẫu linh kiện hot hoặc công cụ AI Build PC không ạ? 🖥️");
                actions.add(new AiChatResponse.QuickAction("Thử AI Build PC", "navigate", "/build-pc"));
            }
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType(messageType)
                    .messageRoute(messageRoute)
                    .quickActions(actions)
                    .build();
        }

        // Branch 3: Sản phẩm ngoài danh mục kinh doanh (Laptop, Macbook, Điện thoại...)
        if (msgLower.matches(".*\\b(laptop|macbook|ipad|máy tính bảng|tablet|iphone|điện thoại|smartphone)\\b.*")) {
            reply.append("Dạ, **TechZone hiện chuyên sâu vào dải sản phẩm Máy tính để bàn (Desktop PC), Máy trạm Workstation và Linh kiện PC lắp ráp chính hãng**, shop hiện **chưa kinh doanh dòng sản phẩm máy tính xách tay (Laptop/Macbook) hay điện thoại di động**.\n\n");
            reply.append("💡 **Gợi ý từ TechZone**:\n");
            reply.append("* Nếu bạn cần hiệu năng cao để làm việc, dựng hình đồ họa hoặc chơi game ổn định, một bộ **Desktop PC cùng tầm giá sẽ mạnh hơn từ 40% đến 60%** so với Laptop cùng ngân sách, đồng thời dễ dàng nâng cấp linh kiện sau này.\n");
            reply.append("* TechZone hỗ trợ **miễn phí công lắp ráp, cài đặt hệ điều hành và giao hàng tận nơi toàn quốc**.\n\n");
            reply.append("Bạn có muốn tham khảo cấu hình Desktop PC tối ưu cho tầm ngân sách của mình không?");

            actions.add(new AiChatResponse.QuickAction("Thử AI Build PC", "navigate", "/build-pc"));
            actions.add(new AiChatResponse.QuickAction("Nhờ chuyên viên tư vấn", "connect_staff", null));
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType("proposal")
                    .messageRoute("/build-pc")
                    .quickActions(actions)
                    .build();
        }

        // Branch 4: Hướng dẫn xử lý sự cố kỹ thuật (Troubleshooting)
        if (msgLower.matches(".*\\b(không lên|đen màn|không tín hiệu|no signal|màn hình xanh|bsod|quạt rú|nóng|sập nguồn|không nhận ram|lau ram|kêu bíp|chập chờn|treo máy)\\b.*")) {
            reply.append("### Chẩn đoán & Hướng dẫn xử lý sự cố kỹ thuật từ TechZone:\n\n");
            reply.append("CyberBot gợi ý các bước kiểm tra nhanh và hiệu quả nhất cho sự cố của bạn:\n\n");

            if (msgLower.matches(".*\\b(không lên|đen màn|không tín hiệu|no signal)\\b.*")) {
                reply.append("1. **Kiểm tra cổng cắm dây xuất hình (HDMI / DisplayPort)**:\n");
                reply.append("   - Nếu máy có card màn hình rời (VGA), **bắt buộc phải cắm dây vào cổng của Card rời (ở phía dưới)**, TUYỆT ĐỐI không cắm vào cổng trên Mainboard (phía trên gần cổng USB).\n");
                reply.append("2. **Vệ sinh và cắm lại chân RAM (Reseat RAM)**:\n");
                reply.append("   - Rút nguồn điện, tháo các thanh RAM ra, dùng gôm/tẩy mềm lau nhẹ phần chân đồng tiếp xúc, sau đó cắm chặt lại vào đúng khe ưu tiên (khe 2 và khe 4 nếu chạy Dual Channel).\n");
                reply.append("3. **Kiểm tra đèn báo Debug LED trên Mainboard**:\n");
                reply.append("   - Quan sát xem đèn LED dừng ở vị trí nào (CPU, DRAM, VGA hay BOOT) để khoanh vùng chính xác linh kiện gặp vấn đề.\n\n");
            } else if (msgLower.matches(".*\\b(màn hình xanh|bsod|treo máy)\\b.*")) {
                reply.append("1. **Kiểm tra xung đột Driver hoặc cập nhật Windows**:\n");
                reply.append("   - Ghi lại mã lỗi dừng (Stop Code như MEMORY_MANAGEMENT, CRITICAL_PROCESS_DIED).\n");
                reply.append("   - Dùng phần mềm DDU gỡ sạch driver card đồ họa và cài lại bản mới nhất từ NVIDIA/AMD.\n");
                reply.append("2. **Kiểm tra profile XMP / EXPO của RAM**:\n");
                reply.append("   - Thử tạm tắt XMP/EXPO trong BIOS về tốc độ mặc định để kiểm tra tính ổn định.\n\n");
            } else {
                reply.append("1. **Kiểm tra nhiệt độ linh kiện & nguồn điện**:\n");
                reply.append("   - Quạt tản nhiệt quay mạnh nhưng máy sập nguồn thường do CPU bị quá nhiệt (khô keo tản nhiệt hoặc bơm tản nước AIO gặp sự cố).\n");
                reply.append("   - Đảm bảo đầu cấp nguồn 8-pin CPU và đầu 12VHPWR/PCIe cho VGA đã được cắm ngập khít hoàn toàn.\n\n");
            }

            reply.append("🔧 **Hỗ trợ miễn phí tại TechZone**:\n");
            reply.append("* Quý khách có thể mang máy đến trực tiếp showroom TechZone để được kỹ thuật viên hỗ trợ vệ sinh, tra keo tản nhiệt và chẩn đoán lỗi hoàn toàn **miễn phí**!\n");
            reply.append("* Hoặc bấm nút bên dưới để kết nối ngay với Chuyên viên kỹ thuật trực tuyến.");

            actions.add(new AiChatResponse.QuickAction("Kết nối chuyên viên kỹ thuật", "connect_staff", null));
            actions.add(new AiChatResponse.QuickAction("Tra cứu bảo hành", "navigate", "/bao-hanh"));
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType("info")
                    .quickActions(actions)
                    .build();
        }

        // Branch 5: Tư vấn tương thích phần cứng (Compatibility)
        if (msgLower.matches(".*\\b(tương thích|lắp được không|cắm được không|chạy chung|đi với nhau|hợp không|nguồn bao nhiêu watt|cần nguồn mấy watt|lga1700|am5|ddr4.*ddr5|ddr5.*ddr4)\\b.*")) {
            return buildCompatibilityAdvisory(message, conversationId);
        }

        // Branch 6: So sánh sản phẩm (Comparison)
        boolean isComparison = (msgLower.contains("so sánh") || msgLower.contains(" vs ") || msgLower.matches(".*(chọn|lấy|nên mua).*?(hay|hoặc|hơn).*"));
        if (isComparison && products.size() >= 2) {
            return buildComparisonAdvisory(message, products, conversationId);
        }

        // Branch 7: Tư vấn build cấu hình PC
        if (msgLower.contains("build") || msgLower.contains("cấu hình") || msgLower.contains("dàn máy") || msgLower.matches(".*(pc gaming|pc đồ họa|pc văn phòng|pc đồ hoạ).*")) {
            reply.append("### Tư vấn cấu hình máy tính TechZone:\n\n");
            reply.append("* Để chọn linh kiện tối ưu ngân sách và đảm bảo **tương thích 100%**, bạn có thể sử dụng tính năng **AI Build PC** độc quyền của TechZone.\n");
            reply.append("* Bạn chỉ cần nhập số tiền dự kiến và nhu cầu (chơi game esports/AAA, đồ họa 3D, dựng phim hay lập trình), hệ thống sẽ tự động phân bổ tỷ lệ ngân sách chuẩn xác từng linh kiện!\n");
            reply.append("* **Đặc quyền tại TechZone**: Miễn phí công lắp ráp, đi dây thẩm mỹ, test stress-test 100% trước khi giao và tặng kèm cài đặt Windows dùng thử.\n\n");
            reply.append("👉 Bạn có muốn mình đưa bạn tới công cụ AI Build PC ngay bây giờ không?");

            messageType = "proposal";
            messageRoute = "/build-pc";
            actions.add(new AiChatResponse.QuickAction("Mở công cụ AI Build PC", "navigate", "/build-pc"));
            actions.add(new AiChatResponse.QuickAction("Nhờ chuyên viên lên cấu hình", "connect_staff", null));
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType(messageType)
                    .messageRoute(messageRoute)
                    .quickActions(actions)
                    .build();
        }

        // Branch 8: Chính sách bảo hành
        if (msgLower.contains("bảo hành") || msgLower.contains("serial") || msgLower.contains("đổi trả") || msgLower.contains("1 đổi 1")) {
            if (ctx.isMember() && !ctx.activeWarranties().isEmpty()) {
                reply.append("CyberBot đã tìm thấy các linh kiện đang trong thời hạn bảo hành của bạn:\n\n");
                for (WarrantySummary w : ctx.activeWarranties()) {
                    reply.append("* **").append(w.productName()).append("**\n");
                    reply.append("  - Serial: `").append(w.serialNumber()).append("`\n");
                    reply.append("  - Hạn bảo hành: ").append(w.endDate()).append(" (**còn ").append(w.daysRemaining()).append(" ngày**)\n");
                }
                reply.append("\n👉 **Bạn có muốn mình đưa bạn tới trang Bảo hành cá nhân để xem chi tiết hoặc tạo yêu cầu hỗ trợ không?**");
                messageType = "proposal";
                messageRoute = "/bao-hanh";
                actions.add(new AiChatResponse.QuickAction("Đến trang Bảo hành", "navigate", "/bao-hanh"));
            } else {
                reply.append("### Chính sách bảo hành chính hãng tại TechZone:\n\n");
                reply.append("* **1 đổi 1 trong 30 ngày đầu**: Áp dụng ngay nếu linh kiện gặp lỗi phần cứng do nhà sản xuất.\n");
                reply.append("* **Thời hạn bảo hành chuẩn hãng**: Từ 36 đến 60 tháng tùy dòng linh kiện (CPU, Main, VGA, Nguồn...).\n");
                reply.append("* **Tem niêm phong & Serial điện tử**: Tra cứu tình trạng bảo hành trực tuyến 24/7 chỉ với Serial máy.\n");
                reply.append("* **Hỗ trợ bảo hành tận tâm**: Kỹ thuật viên tiếp nhận và xử lý nhanh chóng trong 3 - 7 ngày làm việc.\n\n");
                reply.append("👉 Bạn có thể xem chi tiết hoặc tra cứu thẻ bảo hành tại trang Bảo hành!");
                actions.add(new AiChatResponse.QuickAction("Xem trang Bảo hành", "navigate", "/bao-hanh"));
            }
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType(messageType)
                    .messageRoute(messageRoute)
                    .quickActions(actions)
                    .build();
        }

        // Branch 9: Tư vấn chi tiết linh kiện / sản phẩm
        if (!products.isEmpty() || isProductQuery(resolvedQuery)) {
            return buildSmartProductAdvisory(message, resolvedQuery, products, conversationId);
        }

        // Branch 10: Chính sách giao hàng GHN
        if (msgLower.contains("ship") || msgLower.contains("ghn") || msgLower.contains("giao hàng") || msgLower.contains("vận chuyển") || msgLower.contains("bao lâu") || msgLower.contains("đồng kiểm")) {
            reply.append("### Chính sách vận chuyển qua Giao Hàng Nhanh (GHN Express) tại TechZone:\n\n");
            reply.append("* **Thời gian giao hàng**:\n");
            reply.append("  - Khu vực nội thành TP.HCM: Nhận hàng nhanh trong 24 giờ.\n");
            reply.append("  - Các tỉnh thành khác trên toàn quốc: Từ 1 – 3 ngày làm việc.\n");
            reply.append("* **Đặc quyền đồng kiểm (`CHOXEMHANGKHONGTHU`)**:\n");
            reply.append("  - Quý khách được phép mở hộp kiểm tra ngoại quan gói hàng, tem niêm phong nguyên vẹn trước khi nhận hàng và thanh toán.\n");
            reply.append("* **Quy cách đóng gói an toàn**:\n");
            reply.append("  - Toàn bộ linh kiện giữ nguyên hộp Full Box nguyên seal của hãng, đóng thùng carton chèn túi khí chống sốc tiêu chuẩn hàng giá trị cao.\n");
            reply.append("* **Phương thức thanh toán**:\n");
            reply.append("  - Trả tiền mặt khi nhận hàng (COD) hoặc chuyển khoản trực tuyến qua cổng VNPay.\n");

            actions.add(new AiChatResponse.QuickAction("Xem giỏ hàng", "navigate", "/gio-hang"));
            actions.add(new AiChatResponse.QuickAction("Mở AI Build PC", "navigate", "/build-pc"));
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType("info")
                    .quickActions(actions)
                    .build();
        }

        // Branch 11: Thanh toán & VAT
        if (msgLower.contains("thanh toán") || msgLower.contains("vnpay") || msgLower.contains("cod") || msgLower.contains("hóa đơn") || msgLower.contains("vat") || msgLower.contains("trả góp")) {
            reply.append("### Phương thức thanh toán & Xuất hóa đơn VAT tại TechZone:\n\n");
            reply.append("1. **Thanh toán khi nhận hàng (COD)**: Áp dụng trên toàn quốc cho các đơn hàng linh kiện chuẩn.\n");
            reply.append("2. **Thanh toán trực tuyến qua VNPay**: Hỗ trợ quét mã QR qua ứng dụng ngân hàng, thẻ ATM nội địa hoặc thẻ quốc tế Visa/Mastercard an toàn tuyệt đối.\n");
            reply.append("3. **Hóa đơn điện tử VAT**: 100% sản phẩm và linh kiện tại TechZone đều có thể xuất hóa đơn VAT doanh nghiệp theo quy định. Quý khách vui lòng cung cấp Mã số thuế và Tên công ty khi đặt hàng hoặc nhắn chuyên viên hỗ trợ.\n\n");
            reply.append("👉 Bạn cần hỗ trợ thêm về khâu thanh toán hay muốn kết nối chuyên viên để tạo đơn riêng?");

            actions.add(new AiChatResponse.QuickAction("Xem giỏ hàng", "navigate", "/gio-hang"));
            actions.add(new AiChatResponse.QuickAction("Kết nối chuyên viên CSKH", "connect_staff", null));
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType("info")
                    .quickActions(actions)
                    .build();
        }

        // Branch 12: Chào hỏi chung & Menu nghiệp vụ
        reply.append("Xin chào! Mình là **CyberBot** — Trợ lý AI kỹ thuật chính thức của TechZone.\n\n");
        reply.append("Mình có thể hỗ trợ bạn ngay lập tức:\n");
        reply.append("* **Tra cứu linh kiện & kiểm tra kho hàng thời gian thực** (giá bán, tồn kho, thông số)\n");
        reply.append("* **Kiểm tra tiến độ đơn hàng & lộ trình bưu tá GHN Express**\n");
        reply.append("* **Tra cứu thời hạn bảo hành linh kiện & số Serial điện tử**\n");
        reply.append("* **Tư vấn tính tương thích phần cứng & cấu hình máy tính tối ưu**\n");
        reply.append("* **Hướng dẫn xử lý sự cố kỹ thuật máy tính (không lên hình, lỏng RAM, BSOD)**\n");
        reply.append("* **Giải đáp chính sách thanh toán VNPay / COD và bảo hành 1 đổi 1 trong 30 ngày**\n\n");
        reply.append("Bạn cần hỗ trợ điều gì hôm nay?");

        if (ctx.isMember() && !ctx.recentOrders().isEmpty()) {
            actions.add(new AiChatResponse.QuickAction("Đơn hàng gần nhất", "ask", "Đơn hàng gần nhất của tôi thế nào?"));
        }
        actions.add(new AiChatResponse.QuickAction("Kiểm tra bảo hành", "ask", "Linh kiện của tôi còn bảo hành bao lâu?"));
        actions.add(new AiChatResponse.QuickAction("Tư vấn build PC", "navigate", "/build-pc"));
        actions.add(new AiChatResponse.QuickAction("Chính sách GHN", "ask", "Chính sách giao hàng GHN thế nào?"));

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType(messageType)
                .messageRoute(messageRoute)
                .quickActions(actions)
                .build();
    }

    private String mapStatusVietnamese(String status) {
        if (status == null) return "Chờ xử lý";
        return switch (status.toLowerCase()) {
            case "pending" -> "Chờ xác nhận";
            case "confirmed" -> "Đã xác nhận";
            case "preparing" -> "Đang đóng gói linh kiện";
            case "ready_for_pickup" -> "Đã tạo vận đơn GHN, chờ shipper lấy hàng";
            case "delivering" -> "Shipper GHN đang đi giao";
            case "completed" -> "Đã giao thành công";
            case "canceled" -> "Đã hủy đơn";
            default -> status;
        };
    }
}
