package com.project.cybershop.support.internal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.service.CatalogVectorSyncService;
import com.project.cybershop.support.internal.service.GeminiEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogVectorSyncServiceImpl implements CatalogVectorSyncService, ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final GeminiEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    @Override
    public void run(ApplicationArguments args) {
        // Tự động kiểm tra và khởi tạo Vector DB cho kho hàng khi khởi động nếu chưa có
        CompletableFuture.runAsync(() -> {
            try {
                int count = getCatalogVectorCount();
                if (count == 0) {
                    log.info("Chưa có Vector Index cho kho hàng linh kiện TechZone. Bắt đầu đồng bộ tự động...");
                    syncCatalogVectors();
                } else {
                    log.info("Kho hàng đã có {} linh kiện được nhúng trong Vector DB.", count);
                }
            } catch (Exception e) {
                log.warn("Không thể kiểm tra hoặc đồng bộ kho hàng vào Vector DB lúc khởi động: {}", e.getMessage());
            }
        });
    }

    /**
     * Đồng bộ tự động mỗi ngày vào lúc 03:00 sáng.
     */
    @Scheduled(cron = "${app.ai.catalog-sync-cron:0 0 3 * * ?}")
    public void scheduledDailySync() {
        log.info("Bắt đầu tiến trình cập nhật Vector DB kho hàng định kỳ hàng ngày (03:00 AM)...");
        syncCatalogVectors();
    }

    @Override
    public int getCatalogVectorCount() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cyberbot_knowledge_vectors WHERE doc_category = 'PRODUCT_CATALOG'",
                    Integer.class
            );
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Lỗi khi đếm vector kho hàng: {}", e.getMessage());
            return 0;
        }
    }

    private record PreparedCatalogItem(
            String docTitle,
            String content,
            String metaJson,
            String fullTextToEmbed
    ) {}

    @Override
    public Map<String, Object> syncCatalogVectors() {
        long startTime = System.currentTimeMillis();

        try {
            String sql = """
                SELECT p.id as product_id, p.name as product_name, p.description, p.warranty_period_months,
                       c.name as category_name, c.slug as category_slug,
                       COALESCE(b.name, 'Chính hãng') as brand_name,
                       pv.id as variant_id, pv.variant_name, pv.price, pv.original_price,
                       pv.stock_quantity, pv.image_url,
                       spec.specs_summary,
                       rev.avg_rating, rev.review_count
                FROM product_variants pv
                JOIN products p ON pv.product_id = p.id
                JOIN categories c ON p.category_id = c.id
                LEFT JOIN brands b ON p.brand_id = b.id
                LEFT JOIN (
                    SELECT sv.product_variant_id,
                           string_agg(s.name || ': ' || COALESCE(so.display_value, sv.value, ''), ' | ') as specs_summary
                    FROM specification_values sv
                    JOIN specifications s ON sv.specification_id = s.id
                    LEFT JOIN specification_options so ON sv.option_id = so.id
                    GROUP BY sv.product_variant_id
                ) spec ON spec.product_variant_id = pv.id
                LEFT JOIN (
                    SELECT oi.product_variant_id,
                           ROUND(AVG(r.rating)::numeric, 1) as avg_rating,
                           COUNT(r.id) as review_count
                    FROM reviews r
                    JOIN order_items oi ON r.order_item_id = oi.id
                    JOIN orders o ON oi.order_id = o.id
                    WHERE o.status = 'completed'
                    GROUP BY oi.product_variant_id
                ) rev ON rev.product_variant_id = pv.id
                WHERE UPPER(p.status) = 'ACTIVE'
                ORDER BY p.id ASC, pv.price ASC
            """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows.isEmpty()) {
                log.warn("Không tìm thấy sản phẩm ACTIVE nào trong kho để đồng bộ Vector DB.");
                return Map.of("synced", 0, "message", "Kho hàng không có sản phẩm ACTIVE");
            }

            log.info("Tìm thấy {} biến thể sản phẩm trong kho. Đang chuẩn bị nội dung và nhúng Vector theo lô...", rows.size());

            List<PreparedCatalogItem> preparedItems = new ArrayList<>(rows.size());
            List<String> textsToEmbed = new ArrayList<>(rows.size());

            for (Map<String, Object> r : rows) {
                try {
                    Integer productId = (Integer) r.get("product_id");
                    Integer variantId = (Integer) r.get("variant_id");
                    String productName = (String) r.get("product_name");
                    String variantName = (String) r.get("variant_name");
                    String categoryName = (String) r.get("category_name");
                    String brandName = (String) r.get("brand_name");
                    String description = (String) r.get("description");
                    Number priceNum = (Number) r.get("price");
                    Number origPriceNum = (Number) r.get("original_price");
                    Number stockNum = (Number) r.get("stock_quantity");
                    String imageUrl = (String) r.get("image_url");

                    double price = priceNum != null ? priceNum.doubleValue() : 0.0;
                    double originalPrice = origPriceNum != null ? origPriceNum.doubleValue() : price;
                    int stock = stockNum != null ? stockNum.intValue() : 0;

                    String docTitle = "[" + categoryName + "] " + productName + (variantName != null && !variantName.isBlank() ? " (" + variantName + ")" : "");

                    StringBuilder content = new StringBuilder();
                    content.append("Linh kiện: ").append(productName).append("\n");
                    content.append("Phiên bản: ").append(variantName != null ? variantName : "Tiêu chuẩn").append("\n");
                    content.append("Danh mục phần cứng: ").append(categoryName).append("\n");
                    content.append("Thương hiệu: ").append(brandName).append("\n");
                    content.append("Giá bán tại TechZone: ").append(MONEY_FMT.format(price)).append("\n");
                    content.append("Tồn kho hiện có: ").append(stock > 0 ? stock + " sản phẩm sẵn sàng lắp ráp" : "Tạm hết hàng").append("\n");
                    
                    String specsSummary = (String) r.get("specs_summary");
                    if (specsSummary != null && !specsSummary.isBlank()) {
                        content.append("Thông số kỹ thuật chuẩn: ").append(specsSummary).append("\n");
                    }
                    Number avgRatingNum = (Number) r.get("avg_rating");
                    Number reviewCountNum = (Number) r.get("review_count");
                    if (avgRatingNum != null && reviewCountNum != null && reviewCountNum.intValue() > 0) {
                        content.append("Đánh giá khách hàng: ").append(avgRatingNum.doubleValue()).append("/5 sao (")
                               .append(reviewCountNum.intValue()).append(" đánh giá thực tế)\n");
                    }

                    if (description != null && !description.isBlank()) {
                        content.append("Mô tả kỹ thuật: ").append(description).append("\n");
                    }

                    String catLower = categoryName.toLowerCase();
                    if (catLower.contains("vga") || catLower.contains("đồ họa") || catLower.contains("card")) {
                        if (price >= 12_000_000) {
                            content.append("Phân khúc: Card đồ họa cao cấp (High-End) chuyên trị Game AAA 2K/4K Max Setting, Ray Tracing, Render 3D Blender/Lumion nặng và huấn luyện AI.\n");
                        } else if (price >= 5_500_000) {
                            content.append("Phân khúc: Card đồ họa tầm trung (Mid-Range) tối ưu cho Gaming Full HD / 2K mượt mà, dựng phim Premiere và thiết kế đồ họa 3D.\n");
                        } else {
                            content.append("Phân khúc: Card đồ họa phổ thông tiết kiệm ngân sách cho Game eSports (LOL, Valorant, FO4) và đồ họa 2D văn phòng.\n");
                        }
                    } else if (catLower.contains("cpu") || catLower.contains("vi xử lý")) {
                        content.append("Ứng dụng: Xử lý đa luồng, render CPU, dựng hình kiến trúc, lập trình, tính toán giả lập, chơi game esport, dựng hình đồ họa.\n");
                    } else if (catLower.contains("ram") || catLower.contains("bộ nhớ")) {
                        content.append("Ứng dụng: Đa nhiệm nhiều ứng dụng, mở nhiều tab trình duyệt, chạy file 3D/Blender nặng không tràn bộ nhớ, chạy máy ảo Docker.\n");
                    } else if (catLower.contains("ssd") || catLower.contains("ổ cứng") || catLower.contains("storage")) {
                        content.append("Ứng dụng: Tốc độ đọc ghi cao, khởi động Windows siêu tốc, load scene đồ họa nặng nhanh chóng, lưu trữ project phim 4K.\n");
                    } else if (catLower.contains("nguồn") || catLower.contains("psu")) {
                        content.append("Ứng dụng: Cung cấp điện áp ổn định, bảo vệ linh kiện cao cấp, chuẩn 80 Plus Bronze/Gold, đủ công suất cho CPU và VGA mạnh.\n");
                    } else if (catLower.contains("màn hình") || catLower.contains("monitor")) {
                        content.append("Ứng dụng: Màn hình đồ họa màu chuẩn 100% sRGB/DCI-P3, tần số quét cao gaming, tấm nền IPS góc nhìn rộng.\n");
                    }

                    Number warrantyNum = (Number) r.get("warranty_period_months");
                    int warrantyMonths = warrantyNum != null ? warrantyNum.intValue() : 36;

                    Map<String, Object> metaMap = new LinkedHashMap<>();
                    metaMap.put("productId", productId);
                    metaMap.put("variantId", variantId);
                    metaMap.put("name", productName);
                    metaMap.put("variantName", variantName);
                    metaMap.put("category", categoryName);
                    metaMap.put("brand", brandName);
                    metaMap.put("price", price);
                    metaMap.put("originalPrice", originalPrice);
                    metaMap.put("stock", stock);
                    metaMap.put("warrantyMonths", warrantyMonths);
                    if (specsSummary != null && !specsSummary.isBlank()) {
                        metaMap.put("specs", specsSummary);
                    }
                    if (avgRatingNum != null && reviewCountNum != null && reviewCountNum.intValue() > 0) {
                        metaMap.put("rating", avgRatingNum.doubleValue());
                        metaMap.put("reviewCount", reviewCountNum.intValue());
                    }
                    metaMap.put("imageUrl", imageUrl);
                    metaMap.put("route", "/san-pham/" + productId);

                    String metaJson = objectMapper.writeValueAsString(metaMap);
                    String fullText = docTitle + "\n" + content;

                    preparedItems.add(new PreparedCatalogItem(docTitle, content.toString(), metaJson, fullText));
                    textsToEmbed.add(fullText);
                } catch (Exception ex) {
                    log.warn("Lỗi khi chuẩn bị bản ghi sản phẩm: {}", ex.getMessage());
                }
            }

            // 1. Nhúng hàng loạt toàn bộ danh sách sản phẩm trong bộ nhớ trước (BUG-04)
            log.info("Bắt đầu gọi batchEmbedDocuments cho {} sản phẩm...", textsToEmbed.size());
            List<String> vectorStrings = embeddingService.batchEmbedDocuments(textsToEmbed);

            if (vectorStrings.size() != preparedItems.size()) {
                log.error("Số lượng vector trả về ({}) không khớp số lượng sản phẩm ({})!", vectorStrings.size(), preparedItems.size());
                return Map.of("status", "ERROR", "message", "Lỗi bất đồng bộ số lượng vector");
            }

            // 2. Chuẩn bị batch parameters
            List<Object[]> batchParams = new ArrayList<>(preparedItems.size());
            for (int i = 0; i < preparedItems.size(); i++) {
                PreparedCatalogItem item = preparedItems.get(i);
                String vecStr = vectorStrings.get(i);
                batchParams.add(new Object[]{ item.docTitle(), item.content(), item.metaJson(), vecStr });
            }

            // 3. Thực hiện xóa và nạp mới nguyên tử trong một Transaction duy nhất (kho vector không bao giờ rỗng giữa chừng)
            executeAtomicCatalogBatch(batchParams);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Đã đồng bộ thành công {} linh kiện vào Vector DB trong {}ms", batchParams.size(), elapsed);

            return Map.of(
                    "status", "SUCCESS",
                    "syncedCount", batchParams.size(),
                    "durationMs", elapsed
            );
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi đồng bộ kho hàng vào Vector DB: {}", e.getMessage(), e);
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    @Transactional
    public void executeAtomicCatalogBatch(List<Object[]> batchParams) {
        jdbcTemplate.update("DELETE FROM cyberbot_knowledge_vectors WHERE doc_category = 'PRODUCT_CATALOG'");
        jdbcTemplate.batchUpdate("""
            INSERT INTO cyberbot_knowledge_vectors (doc_category, doc_title, chunk_content, metadata, embedding)
            VALUES ('PRODUCT_CATALOG', ?, ?, ?::jsonb, ?::vector)
        """, batchParams);
    }

    @Override
    public List<AiChatResponse.ProductSummary> searchProductsByVector(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            String queryVector = embeddingService.embedText(query);
            int safeLimit = Math.min(Math.max(limit, 1), 10);

            // Sửa triệt để BUG-01 & Mở rộng Context: JOIN products p & brands b để lấy thông số kỹ thuật (live_description) và thương hiệu (live_brand)
            // Truyền đủ 3 tham số queryVector cho 3 vị trí ?::vector + 1 safeLimit cho LIMIT ?
            String sql = """
                SELECT v.doc_title, v.chunk_content, v.metadata,
                       pv.price as live_price, pv.stock_quantity as live_stock,
                       p.description as live_description,
                       p.warranty_period_months as live_warranty,
                       COALESCE(b.name, 'Chính hãng') as live_brand,
                       1 - (v.embedding <=> ?::vector) as similarity
                FROM cyberbot_knowledge_vectors v
                LEFT JOIN product_variants pv ON pv.id = (v.metadata->>'variantId')::int
                LEFT JOIN products p ON p.id = (v.metadata->>'productId')::int
                LEFT JOIN brands b ON b.id = p.brand_id
                WHERE v.doc_category = 'PRODUCT_CATALOG'
                  AND 1 - (v.embedding <=> ?::vector) >= 0.45
                ORDER BY v.embedding <=> ?::vector
                LIMIT ?
            """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, queryVector, queryVector, queryVector, safeLimit);
            if (rows.isEmpty()) {
                return List.of();
            }

            List<AiChatResponse.ProductSummary> results = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                String metaStr = r.get("metadata") != null ? r.get("metadata").toString() : null;
                if (metaStr == null || metaStr.isBlank()) continue;

                try {
                    JsonNode meta = objectMapper.readTree(metaStr);
                    Integer productId = meta.has("productId") ? meta.get("productId").asInt() : null;
                    Integer variantId = meta.has("variantId") ? meta.get("variantId").asInt() : null;
                    String name = meta.has("name") ? meta.get("name").asText() : "";
                    String variantName = meta.has("variantName") && !meta.get("variantName").isNull() ? meta.get("variantName").asText() : null;
                    String category = meta.has("category") ? meta.get("category").asText() : "Linh kiện";
                    double price = meta.has("price") ? meta.get("price").asDouble() : 0.0;
                    Double originalPrice = meta.has("originalPrice") && !meta.get("originalPrice").isNull() ? meta.get("originalPrice").asDouble() : null;
                    int stock = meta.has("stock") ? meta.get("stock").asInt() : 0;
                    String imageUrl = meta.has("imageUrl") && !meta.get("imageUrl").isNull() ? meta.get("imageUrl").asText() : null;
                    String route = meta.has("route") ? meta.get("route").asText() : ("/san-pham/" + productId);

                    // Cập nhật giá, tồn kho, thương hiệu và mô tả kỹ thuật thời gian thực từ câu JOIN
                    Number livePrice = (Number) r.get("live_price");
                    Number liveStock = (Number) r.get("live_stock");
                    String liveBrand = (String) r.get("live_brand");
                    String liveDescription = (String) r.get("live_description");

                    if (livePrice != null) {
                        price = livePrice.doubleValue();
                    }
                    if (liveStock != null) {
                        stock = liveStock.intValue();
                    }

                    String specs = meta.has("specs") && !meta.get("specs").isNull() ? meta.get("specs").asText() : null;
                    Double rating = meta.has("rating") && !meta.get("rating").isNull() ? meta.get("rating").asDouble() : null;
                    Integer reviewCount = meta.has("reviewCount") && !meta.get("reviewCount").isNull() ? meta.get("reviewCount").asInt() : null;
                    Integer warrantyMonths = r.get("live_warranty") != null
                            ? ((Number) r.get("live_warranty")).intValue()
                            : (meta.has("warrantyMonths") && !meta.get("warrantyMonths").isNull() ? meta.get("warrantyMonths").asInt() : 36);

                    results.add(AiChatResponse.ProductSummary.builder()
                            .id(productId)
                            .variantId(variantId)
                            .name(name)
                            .brand(liveBrand != null ? liveBrand : "Chính hãng")
                            .description(liveDescription)
                            .specs(specs)
                            .warrantyMonths(warrantyMonths)
                            .rating(rating)
                            .reviewCount(reviewCount)
                            .variantName(variantName)
                            .category(category)
                            .price(price)
                            .originalPrice(originalPrice)
                            .stock(stock)
                            .imageUrl(imageUrl)
                            .route(route)
                            .build());
                } catch (Exception ex) {
                    log.warn("Lỗi khi parse metadata sản phẩm từ vector: {}", ex.getMessage());
                }
            }

            return results;
        } catch (Exception e) {
            log.warn("Lỗi khi tìm kiếm Vector RAG cho kho hàng: {}", e.getMessage());
            return List.of();
        }
    }
}
