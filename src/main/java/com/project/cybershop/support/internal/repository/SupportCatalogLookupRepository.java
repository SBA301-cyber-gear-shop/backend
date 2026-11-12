package com.project.cybershop.support.internal.repository;

import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.CatalogSearchResult;
import com.project.cybershop.support.internal.model.PcBuildProfile;
import com.project.cybershop.support.internal.model.PriceAndSortFilter;
import com.project.cybershop.support.internal.service.CatalogVectorSyncService;
import com.project.cybershop.support.internal.service.GeminiEmbeddingService;
import com.project.cybershop.support.internal.support.SupportSecurityGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SupportCatalogLookupRepository {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    private final JdbcTemplate jdbcTemplate;
    private final GeminiEmbeddingService embeddingService;
    private final CatalogVectorSyncService catalogVectorSyncService;
    private final SupportSecurityGuard securityGuard;

    private volatile Set<String> dynamicCatalogKeywords = Set.of();
    private volatile long lastLexiconRefreshMs = 0L;

    public Set<String> getDynamicCatalogKeywords() {
        long now = System.currentTimeMillis();
        if (dynamicCatalogKeywords.isEmpty() || (now - lastLexiconRefreshMs) > 600_000L) {
            try {
                List<String> rows = jdbcTemplate.queryForList("""
                    SELECT LOWER(name) FROM brands
                    UNION
                    SELECT LOWER(name) FROM categories
                    UNION
                    SELECT LOWER(slug) FROM categories WHERE slug IS NOT NULL
                """, String.class);
                Set<String> lex = new HashSet<>();
                for (String r : rows) {
                    if (r != null && !r.isBlank()) {
                        lex.add(r.trim().toLowerCase());
                        for (String token : r.trim().toLowerCase().split("[\\s\\-]+")) {
                            if (token.length() >= 3) lex.add(token);
                        }
                    }
                }
                dynamicCatalogKeywords = lex;
                lastLexiconRefreshMs = now;
            } catch (Exception ignored) {}
        }
        return dynamicCatalogKeywords;
    }

    public String findProductNameById(int productId) {
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT name FROM products WHERE id = ? LIMIT 1", String.class, productId);
            if (!names.isEmpty()) {
                return names.get(0);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public AiChatResponse.ProductSummary pickRealComponentBySlot(String categoryKeyword, double maxSlotBudget, String requiredCompatibilityToken) {
        return pickRealComponentBySlot(categoryKeyword, maxSlotBudget, requiredCompatibilityToken, null, null, 25_000_000L);
    }

    public AiChatResponse.ProductSummary pickRealComponentBySlot(String categoryKeyword, double maxSlotBudget, String requiredCompatibilityToken, Double hardPriceCeiling) {
        return pickRealComponentBySlot(categoryKeyword, maxSlotBudget, requiredCompatibilityToken, hardPriceCeiling, null, 25_000_000L);
    }

    public AiChatResponse.ProductSummary pickRealComponentBySlot(
            String categoryKeyword,
            double maxSlotBudget,
            String requiredCompatibilityToken,
            Double hardPriceCeiling,
            PcBuildProfile profile,
            long totalBudgetVal) {
        try {
            String sql = """
                SELECT p.id as product_id, p.name as product_name, p.description as product_desc,
                       p.warranty_period_months as warranty_months,
                       COALESCE(b.name, 'Chính hãng') as brand_name,
                       c.name as category_name,
                       pv.id as variant_id, pv.variant_name, pv.price, pv.original_price,
                       pv.stock_quantity as stock, pv.image_url,
                       spec.specs_summary
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
                WHERE UPPER(p.status) = 'ACTIVE'
                  AND pv.stock_quantity > 0
                  AND (c.slug = ? OR (? != 'cpu' AND LOWER(c.name) LIKE ?) OR (? = 'cpu' AND LOWER(c.name) LIKE '%vi xử lý%'))
            """;

            List<Map<String, Object>> allRows = jdbcTemplate.queryForList(
                    sql + " ORDER BY pv.price ASC, pv.stock_quantity DESC LIMIT 50",
                    categoryKeyword.toLowerCase(),
                    categoryKeyword.toLowerCase(),
                    "%" + categoryKeyword.toLowerCase() + "%",
                    categoryKeyword.toLowerCase()
            );

            if (allRows.isEmpty()) return null;

            String catLower = categoryKeyword.toLowerCase();
            boolean isCpuSlot = catLower.equals("cpu") || catLower.contains("vi xử lý");
            boolean isMainSlot = catLower.contains("main") || catLower.contains("bo mạch");
            boolean isRamSlot = catLower.contains("ram");
            boolean isGpuSlot = catLower.contains("gpu") || catLower.contains("card") || catLower.contains("vga");
            boolean isSsdSlot = catLower.contains("ssd") || catLower.contains("ổ cứng");
            boolean isCoolerSlot = catLower.contains("cooler") || catLower.contains("tản");

            double allowedMax = (hardPriceCeiling != null && hardPriceCeiling > 0)
                    ? hardPriceCeiling
                    : (maxSlotBudget * 1.15);

            String reqToken = (requiredCompatibilityToken != null && !requiredCompatibilityToken.isBlank())
                    ? requiredCompatibilityToken.toLowerCase().trim() : null;

            List<Map<String, Object>> validCandidates = new ArrayList<>();
            for (Map<String, Object> r : allRows) {
                String haystack = (r.get("product_name") + " " + r.get("variant_name") + " " + r.get("specs_summary") + " " + r.get("product_desc")).toLowerCase();
                if (securityGuard.isServerOrWorkstationOnlyPart(haystack, totalBudgetVal)) {
                    continue;
                }
                if (reqToken != null) {
                    if (!haystack.contains(reqToken)) {
                        boolean isUniversalAioCooler = isCoolerSlot && (haystack.contains("360") || haystack.contains("240") || haystack.contains("aio"))
                                && !haystack.contains("sp5") && !haystack.contains("tr5");
                        if (!isUniversalAioCooler) {
                            continue;
                        }
                    }
                }
                validCandidates.add(r);
            }

            if (validCandidates.isEmpty()) {
                validCandidates = new ArrayList<>(allRows);
            }

            Map<String, Object> bestCandidate = null;
            double bestScore = -1e18;

            for (Map<String, Object> r : validCandidates) {
                double price = ((Number) r.get("price")).doubleValue();
                if (price > allowedMax) {
                    continue;
                }

                String haystack = (r.get("product_name") + " " + r.get("variant_name") + " " + r.get("specs_summary") + " " + r.get("product_desc")).toLowerCase();

                double distanceRatio = Math.abs(price - maxSlotBudget) / Math.max(1.0, maxSlotBudget);
                double score = 100.0 - (distanceRatio * 65.0);
                if (price <= maxSlotBudget) {
                    score += (price / Math.max(1.0, maxSlotBudget)) * 25.0;
                }

                if (profile != null) {
                    if (isCpuSlot) {
                        if (totalBudgetVal >= 13_500_000L && securityGuard.isApuProcessor(haystack)) {
                            score -= 85.0;
                        }
                        if (profile.preferredCpuBrand() != null) {
                            if (haystack.contains(profile.preferredCpuBrand())) {
                                score += 35.0;
                            } else {
                                score -= 25.0;
                            }
                        }
                        if ("GAMING".equals(profile.profileType())) {
                            if (haystack.contains("x3d")) score += 45.0;
                            else if (haystack.matches(".*(7500f|9700x|13400f|14400f|14600k|12400f).*")) score += 28.0;
                        } else if ("WORKSTATION_3D".equals(profile.profileType())) {
                            if (haystack.matches(".*(14700f|270k|9900x|9950x|285k).*")) score += 52.0;
                            else if (haystack.matches(".*(14600k|245k|9700x|13400f|14400f).*")) score += 32.0;
                            else if (haystack.contains("7800x3d")) score -= 12.0;
                        }
                    }

                    if (isMainSlot && totalBudgetVal >= 25_000_000L) {
                        if (haystack.contains("ddr5") || haystack.contains("b650") || haystack.contains("x870") || haystack.contains("b860") || haystack.contains("z890") || haystack.contains("z790")) {
                            score += 30.0;
                        } else if (haystack.contains("ddr4")) {
                            score -= 25.0;
                        }
                    }

                    if (isRamSlot && profile.minRamGb() != null) {
                        int minRam = profile.minRamGb();
                        boolean is8Gb = haystack.matches(".*\\b8gb\\b.*");
                        boolean is16Gb = haystack.matches(".*\\b16gb\\b.*");
                        boolean is32GbPlus = haystack.matches(".*\\b(32gb|48gb|64gb)\\b.*");
                        if (minRam >= 32) {
                            if (is32GbPlus) score += 55.0;
                            else if (is16Gb) score -= 20.0;
                            else if (is8Gb) score -= 80.0;
                        } else if (minRam >= 16) {
                            if (is16Gb || is32GbPlus) score += 50.0;
                            else if (is8Gb) score -= 75.0;
                        }
                    }

                    if (isGpuSlot && profile.preferredGpuBrand() != null) {
                        if ("nvidia".equals(profile.preferredGpuBrand())) {
                            if (haystack.contains("rtx") || haystack.contains("geforce")) score += 45.0;
                            else score -= 35.0;
                        } else if ("amd".equals(profile.preferredGpuBrand())) {
                            if (haystack.contains("radeon") || haystack.contains("rx ")) score += 45.0;
                        }
                    }

                    if (isSsdSlot && profile.minSsdGb() != null && profile.minSsdGb() >= 1000) {
                        if (haystack.matches(".*\\b(1tb|2tb|4tb|1000)\\b.*")) score += 45.0;
                        else if (haystack.matches(".*\\b(256gb|500gb)\\b.*")) score -= 45.0;
                    }

                    if (profile.preferAesthetic()) {
                        if (haystack.matches(".*(rgb|argb|o11|aero|ice|titanium|dominator|trident|icue|hydroshift).*")) {
                            score += 18.0;
                        }
                    }
                    if (profile.preferWhite()) {
                        if (haystack.matches(".*(trắng|white|ice|aero).*")) {
                            score += 20.0;
                        }
                    }
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestCandidate = r;
                }
            }

            if (bestCandidate == null) {
                Map<String, Object> cheapest = validCandidates.get(0);
                for (Map<String, Object> r : validCandidates) {
                    if (((Number) r.get("price")).doubleValue() < ((Number) cheapest.get("price")).doubleValue()) {
                        cheapest = r;
                    }
                }
                bestCandidate = cheapest;
            }

            return mapRowToProductSummary(bestCandidate);
        } catch (Exception e) {
            log.warn("Lỗi pickRealComponentBySlot cho category={}: {}", categoryKeyword, e.getMessage());
            return null;
        }
    }

    public AiChatResponse.ProductSummary mapRowToProductSummary(Map<String, Object> r) {
        Integer pid = ((Number) r.get("product_id")).intValue();
        Integer vid = r.get("variant_id") != null ? ((Number) r.get("variant_id")).intValue() : pid;
        String rawSpecs = (String) r.get("specs_summary");
        String cleanSpecs = securityGuard.sanitizeSpecsForUser(rawSpecs);
        return AiChatResponse.ProductSummary.builder()
                .id(pid)
                .variantId(vid)
                .name((String) r.get("product_name"))
                .brand(r.get("brand_name") != null ? (String) r.get("brand_name") : "Chính hãng")
                .description((String) r.get("product_desc"))
                .specs(cleanSpecs)
                .warrantyMonths(r.get("warranty_months") != null ? ((Number) r.get("warranty_months")).intValue() : 36)
                .variantName((String) r.get("variant_name"))
                .category((String) r.get("category_name"))
                .price(r.get("price") != null ? ((Number) r.get("price")).doubleValue() : 0.0)
                .originalPrice(r.get("original_price") != null ? ((Number) r.get("original_price")).doubleValue() : null)
                .stock(r.get("stock") != null ? ((Number) r.get("stock")).intValue() : 0)
                .imageUrl((String) r.get("image_url"))
                .route("/san-pham/" + pid)
                .build();
    }

    public String buildCatalogContextFromSummaries(List<AiChatResponse.ProductSummary> products) {
        if (products == null || products.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH LINH KIỆN HIỆN CÓ SẴN TRONG KHO TECHZONE (TÌM TỪ VECTOR DATABASE & KHO THỜI GIAN THỰC):\n");
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

    public String searchVectorKnowledge(String query) {
        try {
            String vecStr = embeddingService.embedText(query);
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

    public PriceAndSortFilter parsePriceAndSortFilter(String msgLower) {
        Double min = null;
        Double max = null;
        String orderBy = "pv.stock_quantity DESC, pv.price ASC";

        Matcher rangeMatcher = Pattern
                .compile("(\\d+(?:[.,]\\d+)?)\\s*(?:đến|tới|\\-)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|củ)", Pattern.CASE_INSENSITIVE)
                .matcher(msgLower);
        if (rangeMatcher.find()) {
            double v1 = Double.parseDouble(rangeMatcher.group(1).replace(",", ".")) * 1_000_000;
            double v2 = Double.parseDouble(rangeMatcher.group(2).replace(",", ".")) * 1_000_000;
            min = Math.min(v1, v2);
            max = Math.max(v1, v2);
        } else {
            Matcher underMatcher = Pattern
                    .compile("(?:dưới|tối đa|không quá|nhỏ hơn|<)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|củ)", Pattern.CASE_INSENSITIVE)
                    .matcher(msgLower);
            if (underMatcher.find()) {
                max = Double.parseDouble(underMatcher.group(1).replace(",", ".")) * 1_000_000;
            } else {
                Matcher overMatcher = Pattern
                        .compile("(?:trên|từ|hơn|>)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|củ)", Pattern.CASE_INSENSITIVE)
                        .matcher(msgLower);
                if (overMatcher.find()) {
                    min = Double.parseDouble(overMatcher.group(1).replace(",", ".")) * 1_000_000;
                } else {
                    Matcher aroundMatcher = Pattern
                            .compile("(?:tầm|khoảng|quanh|giá)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|củ)", Pattern.CASE_INSENSITIVE)
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

    public CatalogSearchResult searchCatalogProducts(String message) {
        String msgLower = message.toLowerCase();
        boolean isHardwareQuery = msgLower.matches(".*(ram|cpu|chip|chíp|vga|gpu|card|mainboard|main|bo mạch|nguồn|psu|ssd|ổ cứng|tản|case|vỏ|màn|chuột|bàn phím|phím|tai nghe|ddr|ghz|mhz|gb|tb|rtx|gtx|rx|intel|amd|ryzen|core|i3|i5|i7|i9|asus|msi|gigabyte|corsair|kingston|samsung|lexar|deepcool|lian li|arctic|cooler master|epyc|threadripper).*");
        if (!isHardwareQuery) {
            return new CatalogSearchResult("", List.of());
        }

        try {
            PriceAndSortFilter filter = parsePriceAndSortFilter(msgLower);

            List<String> keywords = new ArrayList<>();
            String[] tokens = msgLower.replaceAll("[^a-zA-Z0-9À-ỹ\\s\\-]", " ").split("\\s+");
            Set<String> ignore = new HashSet<>(Arrays.asList(
                    "shop", "co", "khong", "nao", "hay", "cho", "hoi", "la", "va", "gia", "ban", "mua", "voi", "dung", "de",
                    "tu", "van", "san", "pham", "mau", "manh", "nhat", "re", "hien", "nay", "cua", "toi", "minh", "chiec",
                    "con", "cai", "loai", "tot", "tam", "duoi", "tren", "khoang", "trieu", "cung", "khi", "nhung",
                    "nói", "kĩ", "kỹ", "hơn", "về", "tư", "vấn", "giúp", "tôi", "mình", "hỏi", "xem", "chi", "tiết",
                    "đánh", "giá", "thông", "số", "của", "bộ", "dàn", "này", "đó", "kia", "ấy", "sao", "thế", "nào", "như",
                    "ổn", "không", "mạnh", "yếu", "tốt", "ngon", "chơi", "làm", "được", "vậy", "còn", "thì", "nhé", "ạ",
                    "ơi", "bot", "với", "và", "hoặc", "so", "sánh", "giữa", "nên", "lấy", "chọn", "bán", "có", "hàng",
                    "tầm", "khoảng", "dưới", "trên", "triệu", "củ", "rẻ", "nhất", "đắt", "cao", "cấp", "thấp", "hiện", "tại",
                    "bên", "cửa", "linh", "kiện", "sản", "phẩm", "mẫu", "chiếc", "thanh", "cục", "cái", "thêm", "rõ"
            ));
            for (String t : tokens) {
                if (t.length() >= 2 && !ignore.contains(t)) {
                    if (t.equals("chip") || t.equals("chíp")) {
                        if (!keywords.contains("cpu")) keywords.add("cpu");
                        if (!keywords.contains("vi xử lý")) keywords.add("vi xử lý");
                    } else if (t.equals("card") || t.equals("cạc")) {
                        if (!keywords.contains("card đồ họa")) keywords.add("card đồ họa");
                        if (!keywords.contains("rtx")) keywords.add("rtx");
                    } else {
                        if (!keywords.contains(t)) keywords.add(t);
                    }
                }
            }

            if (keywords.isEmpty() && filter.categoryHint() == null) {
                return new CatalogSearchResult("", List.of());
            }

            List<Object> params = new ArrayList<>();
            int kwCount = Math.min(keywords.size(), 8);

            StringBuilder scoreExpr = new StringBuilder("(");
            if (kwCount > 0) {
                for (int i = 0; i < kwCount; i++) {
                    if (i > 0) scoreExpr.append(" + ");
                    String kw = keywords.get(i);
                    int weight = kw.matches(".*\\d+.*") ? 4 : 2;
                    scoreExpr.append("(CASE WHEN LOWER(p.name || ' ' || COALESCE(pv.variant_name, '') || ' ' || c.name || ' ' || COALESCE(c.slug, '')) LIKE ? THEN ").append(weight).append(" ELSE 0 END)");
                    params.add("%" + kw + "%");
                }
            } else {
                scoreExpr.append("1");
            }
            scoreExpr.append(")");

            StringBuilder sql = new StringBuilder("""
                SELECT p.id as product_id, p.name as product_name, p.description as product_desc,
                       p.warranty_period_months as warranty_months,
                       COALESCE(b.name, 'Chính hãng') as brand_name,
                       c.name as category_name,
                       pv.id as variant_id, pv.variant_name, pv.price, pv.original_price,
                       pv.stock_quantity as stock, pv.image_url,
                       spec.specs_summary,
            """);
            sql.append(scoreExpr).append(" as kw_score ");
            sql.append("""
                FROM product_variants pv
                JOIN products p ON pv.product_id = p.id
                LEFT JOIN brands b ON p.brand_id = b.id
                JOIN categories c ON p.category_id = c.id
                LEFT JOIN (
                    SELECT sv.product_variant_id,
                           string_agg(s.name || ': ' || COALESCE(so.display_value, sv.value, ''), ' | ') as specs_summary
                    FROM specification_values sv
                    JOIN specifications s ON sv.specification_id = s.id
                    LEFT JOIN specification_options so ON sv.option_id = so.id
                    GROUP BY sv.product_variant_id
                ) spec ON spec.product_variant_id = pv.id
                WHERE UPPER(p.status) = 'ACTIVE'
            """);

            if (filter.minPrice() != null) {
                sql.append(" AND pv.price >= ?");
                params.add(filter.minPrice());
            }
            if (filter.maxPrice() != null) {
                sql.append(" AND pv.price <= ?");
                params.add(filter.maxPrice());
            }

            if (kwCount > 0) {
                sql.append(" AND (");
                for (int i = 0; i < kwCount; i++) {
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

            sql.append(" ORDER BY kw_score DESC, ").append(filter.orderByClause()).append(" LIMIT 8");

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            if (rows.isEmpty()) {
                return new CatalogSearchResult("TechZone hiện tại không tìm thấy linh kiện nào khớp với từ khóa tìm kiếm này.", List.of());
            }

            StringBuilder sb = new StringBuilder();
            sb.append("DANH SÁCH LINH KIỆN HIỆN CÓ SẴN TRONG KHO TECHZONE LIÊN QUAN ĐẾN CÂU HỎI:\n");
            List<AiChatResponse.ProductSummary> products = new ArrayList<>();

            for (Map<String, Object> r : rows) {
                Integer pid = (Integer) r.get("product_id");
                String pName = (String) r.get("product_name");
                String vName = (String) r.get("variant_name");
                String bName = (String) r.get("brand_name");
                String pDesc = (String) r.get("product_desc");
                Number price = (Number) r.get("price");
                Number stock = (Number) r.get("stock");

                sb.append("- ").append(pName).append(" (Phiên bản: ").append(vName != null ? vName : "Tiêu chuẩn").append(")")
                  .append(" | Thương hiệu: ").append(bName != null ? bName : "Chính hãng")
                  .append(" | Giá bán: ").append(price != null ? MONEY_FMT.format(price.doubleValue()) : "Liên hệ")
                  .append(" | Tồn kho: ").append(stock != null ? stock : 0)
                  .append(" | Link: /san-pham/").append(pid);
                if (pDesc != null && !pDesc.isBlank()) {
                    sb.append(" | Thông số: ").append(pDesc.trim());
                }
                sb.append("\n");

                products.add(mapRowToProductSummary(r));
            }

            return new CatalogSearchResult(sb.toString(), products);
        } catch (Exception e) {
            log.warn("Lỗi khi tìm kiếm sản phẩm trong catalog: {}", e.getMessage());
            return new CatalogSearchResult("", List.of());
        }
    }

    public AiChatResponse.ProductSummary findExactAdvisedProduct(String advisedLine) {
        if (advisedLine == null || advisedLine.isBlank()) return null;
        try {
            String clean = advisedLine
                    .replaceAll("(?i)^(cpu|vga|gpu|card đồ họa|mainboard|bo mạch chủ|main|ram|bộ nhớ|ssd|ổ cứng|nguồn|psu|vỏ case|case|tản nhiệt|màn hình)\\s*[:\\-]\\s*", "")
                    .replaceAll("\\(.*?\\)", "")
                    .replaceAll("—.*", "")
                    .replaceAll("\\|.*", "")
                    .replaceAll("\\*\\*", "")
                    .trim();

            if (clean.length() < 3) return null;

            String exactSql = """
                SELECT p.id as product_id, p.name as product_name, p.description as product_desc,
                       p.warranty_period_months as warranty_months,
                       COALESCE(b.name, 'Chính hãng') as brand_name,
                       c.name as category_name,
                       pv.id as variant_id, pv.variant_name, pv.price, pv.original_price,
                       pv.stock_quantity as stock, pv.image_url,
                       spec.specs_summary
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
                WHERE UPPER(p.status) = 'ACTIVE'
                  AND (LOWER(p.name) LIKE ? OR LOWER(?) LIKE '%' || LOWER(p.name) || '%')
                ORDER BY (CASE WHEN LOWER(p.name) = LOWER(?) THEN 0 ELSE 1 END),
                         pv.stock_quantity DESC, pv.price ASC
                LIMIT 1
            """;
            List<Map<String, Object>> exactRows = jdbcTemplate.queryForList(
                    exactSql, "%" + clean.toLowerCase() + "%", clean.toLowerCase(), clean.toLowerCase()
            );

            if (exactRows.isEmpty()) {
                List<String> modelTokens = new ArrayList<>();
                for (String tk : clean.toLowerCase().replaceAll("[^a-z0-9À-ỹ\\-\\s]", " ").split("\\s+")) {
                    if (tk.length() >= 2 && !Set.of("gb", "tb", "mhz", "bus", "oc", "wifi", "pro", "plus", "dual", "gaming", "box", "tray", "đen", "trắng").contains(tk)) {
                        modelTokens.add(tk);
                    }
                }
                if (!modelTokens.isEmpty()) {
                    StringBuilder tokenSql = new StringBuilder("""
                        SELECT p.id as product_id, p.name as product_name, p.description as product_desc,
                               p.warranty_period_months as warranty_months,
                               COALESCE(b.name, 'Chính hãng') as brand_name,
                               c.name as category_name,
                               pv.id as variant_id, pv.variant_name, pv.price, pv.original_price,
                               pv.stock_quantity as stock, pv.image_url,
                               spec.specs_summary,
                               (
                    """);
                    List<Object> params = new ArrayList<>();
                    for (int i = 0; i < modelTokens.size(); i++) {
                        if (i > 0) tokenSql.append(" + ");
                        tokenSql.append("(CASE WHEN LOWER(p.name || ' ' || COALESCE(pv.variant_name, '')) LIKE ? THEN 2 ELSE 0 END)");
                        params.add("%" + modelTokens.get(i) + "%");
                    }
                    tokenSql.append("""
                        ) as match_score FROM product_variants pv
                        JOIN products p ON pv.product_id = p.id
                        JOIN categories c ON p.category_id = c.id
                        LEFT JOIN brands b ON p.brand_id = b.id
                        LEFT JOIN (
                            SELECT sv.product_variant_id, string_agg(s.name || ': ' || COALESCE(so.display_value, sv.value, ''), ' | ') as specs_summary
                            FROM specification_values sv
                            JOIN specifications s ON sv.specification_id = s.id
                            LEFT JOIN specification_options so ON sv.option_id = so.id
                            GROUP BY sv.product_variant_id
                        ) spec ON spec.product_variant_id = pv.id
                        WHERE UPPER(p.status) = 'ACTIVE' AND (
                    """);
                    for (int i = 0; i < modelTokens.size(); i++) {
                        if (i > 0) tokenSql.append(" OR ");
                        tokenSql.append("LOWER(p.name) LIKE ?");
                        params.add("%" + modelTokens.get(i) + "%");
                    }
                    tokenSql.append(") ORDER BY match_score DESC, pv.stock_quantity DESC LIMIT 1");
                    List<Map<String, Object>> scoredRows = jdbcTemplate.queryForList(tokenSql.toString(), params.toArray());
                    if (!scoredRows.isEmpty() && ((Number) scoredRows.get(0).get("match_score")).intValue() >= 2) {
                        exactRows = scoredRows;
                    }
                }
            }

            if (!exactRows.isEmpty()) {
                return mapRowToProductSummary(exactRows.get(0));
            }
        } catch (Exception e) {
            log.warn("Lỗi findExactAdvisedProduct cho '{}': {}", advisedLine, e.getMessage());
        }

        if (catalogVectorSyncService != null) {
            List<AiChatResponse.ProductSummary> vec = catalogVectorSyncService.searchProductsByVector(advisedLine, 1);
            if (vec != null && !vec.isEmpty()) {
                return vec.get(0);
            }
        }
        return null;
    }
}
