package com.project.cybershop.support.internal.service.impl;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.CustomerContext;
import com.project.cybershop.support.internal.model.CustomerContext.OrderSummary;
import com.project.cybershop.support.internal.model.SupportClassifiedIntent;
import com.project.cybershop.support.internal.repository.SupportCatalogLookupRepository;
import com.project.cybershop.support.internal.repository.SupportCustomerContextRepository;
import com.project.cybershop.support.internal.service.StaffLiveSupportService;
import com.project.cybershop.support.internal.service.SupportIntentResolver;
import com.project.cybershop.support.internal.support.SupportLlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SupportIntentResolverImpl implements SupportIntentResolver {

    private static final Map<String, Integer> VN_WORD_NUMBERS = Map.ofEntries(
            Map.entry("một trăm", 100),
            Map.entry("chín mươi lăm", 95),
            Map.entry("chín mươi", 90),
            Map.entry("chín chục", 90),
            Map.entry("tám mươi lăm", 85),
            Map.entry("tám mươi", 80),
            Map.entry("tám chục", 80),
            Map.entry("bảy mươi lăm", 75),
            Map.entry("bảy mươi", 70),
            Map.entry("bảy chục", 70),
            Map.entry("sáu mươi lăm", 65),
            Map.entry("sáu mươi", 60),
            Map.entry("sáu chục", 60),
            Map.entry("năm mươi lăm", 55),
            Map.entry("năm lăm", 55),
            Map.entry("năm mươi", 50),
            Map.entry("năm chục", 50),
            Map.entry("bốn mươi lăm", 45),
            Map.entry("bốn lăm", 45),
            Map.entry("bốn mươi", 40),
            Map.entry("bốn chục", 40),
            Map.entry("ba mươi lăm", 35),
            Map.entry("ba lăm", 35),
            Map.entry("ba mươi", 30),
            Map.entry("ba chục", 30),
            Map.entry("hai mươi lăm", 25),
            Map.entry("hai lăm", 25),
            Map.entry("hai mươi", 20),
            Map.entry("hai chục", 20),
            Map.entry("mười tám", 18),
            Map.entry("mười lăm", 15),
            Map.entry("mười hai", 12),
            Map.entry("mười", 10)
    );

    private final SupportCatalogLookupRepository catalogRepository;
    private final SupportCustomerContextRepository customerContextRepository;
    private final StaffLiveSupportService liveSupportService;
    private final SupportLlmClient llmClient;

    @Override
    public SupportClassifiedIntent classifyHybridIntent(String message, List<AiChatRequest.HistoryItem> history) {
        return llmClient.classifyIntentWithLlm(message, history);
    }

    @Override
    public Set<String> extractTargetCategories(String text) {
        if (text == null) return Set.of();
        String m = text.toLowerCase();
        Set<String> cats = new HashSet<>();

        if (m.matches(".*\\b(ram|bộ nhớ|thanh ram)\\b.*")) {
            cats.add("RAM");
        }
        if (m.matches(".*\\b(main|mainboard|bo mạch|bo mạch chủ|motherboard|bo mach)\\b.*")) {
            cats.add("MAINBOARD");
        }
        if (m.matches(".*\\b(card|vga|gpu|card màn hình|card đồ họa|cạc|cạc màn hình)\\b.*")) {
            cats.add("VGA");
        }
        if (m.matches(".*\\b(cpu|chip|chíp|vi xử lý|bộ vi xử lý|processor)\\b.*")) {
            cats.add("CPU");
        }
        if (m.matches(".*\\b(ssd|ổ cứng|ổ ssd|nvme|m2|m\\.2|ổ lưu trữ|storage|hdd)\\b.*")) {
            cats.add("SSD");
        }
        if (m.matches(".*\\b(nguồn|psu|nguồn máy tính|cục nguồn|bộ nguồn)\\b.*")) {
            cats.add("NGUỒN");
        }
        if (m.matches(".*\\b(case|vỏ case|thùng máy|vỏ máy|thùng case|vỏ)\\b.*")) {
            cats.add("CASE");
        }
        if (m.matches(".*\\b(tản|tản nhiệt|cooler|fan cpu|quạt tản|tản cpu|quạt)\\b.*")) {
            cats.add("TẢN NHIỆT");
        }

        return cats;
    }

    @Override
    public List<String> extractComponentLines(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> lines = new ArrayList<>();

        if (text.contains("🔹") || (text.contains("• **") && text.contains("—"))) {
            for (String rawLine : text.split("\\r?\\n")) {
                String l = rawLine.trim();
                if (l.startsWith("🔹") || (l.startsWith("• **") && l.contains(":"))) {
                    String afterColon = l.contains(":") ? l.substring(l.indexOf(":") + 1) : l;
                    if (afterColon.contains("—")) {
                        afterColon = afterColon.substring(0, afterColon.indexOf("—"));
                    }
                    if (afterColon.contains("|")) {
                        afterColon = afterColon.substring(0, afterColon.indexOf("|"));
                    }
                    String clean = afterColon.replace("🔹", "").replace("**", "").replace("•", "").trim();
                    if (clean.length() > 3) {
                        lines.add(clean);
                    }
                }
            }
            if (!lines.isEmpty()) {
                return lines;
            }
        }

        for (String rawLine : text.split("\\r?\\n")) {
            String l = rawLine.trim();
            if (l.isBlank()) continue;
            String lower = l.toLowerCase();

            if (lower.contains("tổng chi phí") || lower.contains("chi phí dự kiến") || lower.contains("bạn có muốn")
                    || lower.contains("chúc bạn") || lower.contains("xin chào") || lower.contains("tuyệt vời")
                    || lower.contains("lưu ý") || lower.contains("chưa bao gồm") || lower.contains("tự tay tùy chỉnh")
                    || lower.contains("trong dàn pc trên") || lower.contains("những phần chưa bao gồm")) {
                continue;
            }

            boolean isNumberedOrBullet = l.matches("^(\\d+[\\.\\)]|[\\*\\-•🔹]).*");
            boolean hasColon = l.contains(":");
            boolean hasHardwareKeyword = lower.matches(".*(cpu|ram|vga|gpu|card|mainboard|main|ssd|nguồn|psu|tản nhiệt|vỏ case|case|màn hình|intel|amd|ryzen|geforce|rtx).*");

            if (isNumberedOrBullet && hasColon && hasHardwareKeyword) {
                String prefix = l.substring(0, l.indexOf(":")).replaceAll("[^a-zA-Z0-9À-ỹ\\s]", "").trim();
                String afterColon = l.substring(l.indexOf(":") + 1).trim();

                String clean = afterColon.replaceAll("\\*\\*", "")
                        .replaceAll("\\(~?[0-9.,]+\\s*(đ|vnd|triệu|k).*?\\)", "")
                        .replaceAll("\\((bắt buộc|optix|tận dụng|đọc/ghi|tray|tiêu chuẩn).*?\\)", "")
                        .trim();

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

    @Override
    public boolean isProductQuery(String message) {
        if (message == null) return false;
        String m = message.toLowerCase().trim();
        if (m.matches(".*(ram|cpu|vga|gpu|card|mainboard|main|nguồn|psu|ssd|ổ cứng|màn hình|chuột|bàn phím|ddr|ghz|mhz|gb|tb|rtx|gtx|intel|amd|ryzen|core|i3|i5|i7|i9|asus|msi|gigabyte|corsair|kingston|linh kiện|mua máy|cấu hình|pc).*")) {
            return true;
        }
        for (String kw : catalogRepository.getDynamicCatalogKeywords()) {
            if (m.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isReferralComponentQuery(String message) {
        if (message == null) return false;
        String m = message.toLowerCase().trim();
        if (m.contains("add_all_to_cart") || m.matches(".*(thêm|cho|bỏ|đưa|chốt|lấy|mua).*giỏ.*") || m.matches(".*vào giỏ.*")) {
            return true;
        }
        if (m.matches(".*(nâng cấp|nâng món|nâng linh kiện|tiền dư|dư đó|còn dư|còn thừa|đổi sang|thay bằng|hạ xuống|giảm xuống|hiệu năng|kỹ hơn|kĩ hơn|chi tiết hơn|nói rõ|giải thích|phân tích|đánh giá|review|fps|mạnh không|mượt không|ổn không|chơi được|làm được|tại sao|vì sao|ưu nhược điểm).*")) {
            return false;
        }
        if (!extractTargetCategories(m).isEmpty() && m.matches(".*(thêm|cho|lấy|mua|chốt|đưa|bỏ|chọn|order).*") && !m.matches(".*(tư vấn|nói|giải thích|chi tiết|kỹ|kĩ|đánh giá|sao|thế nào).*")) {
            return true;
        }
        boolean hasListOrShow = m.matches(".*(list|danh sách|cho xem|gửi|show|link|thêm|cho.*vào|bỏ.*vào|chốt|lấy|order|cart|vào giỏ|giỏ hàng).*");
        boolean hasComponent = m.matches(".*(linh kiện|sản phẩm|cấu hình|món|phần cứng|dàn pc|dàn máy|bộ này|dàn này|món này|món trên).*");
        boolean hasReferralWords = m.matches(".*(này|đó|kia|bạn nói|vừa nói|vừa tư vấn|vừa nêu|vừa đề xuất|vừa báo|vừa rồi|trên shop|ở shop|tại shop|ở cửa hàng|trên|vào giỏ|giỏ hàng).*");

        return (hasListOrShow && hasComponent && hasReferralWords)
                || m.matches(".*(list.*linh kiện|danh sách.*linh kiện|gửi.*linh kiện.*(bạn nói|vừa nói|này|đó|trên)|xem.*linh kiện.*(bạn nói|vừa nói|này|đó|trên)).*")
                || m.matches(".*(thêm.*(linh kiện|món|bộ|cấu hình).*(vào giỏ|giỏ hàng|trên)|cho.*(linh kiện|món).*(vào giỏ|giỏ hàng)|mua.*(bộ này|các món này|linh kiện trên)|chốt.*(bộ này|dàn này)).*")
                || m.matches(".*(thêm vào giỏ|cho vào giỏ|bỏ vào giỏ|mua luôn|chốt đơn|lấy bộ này|chốt bộ này|cho các món.*vào giỏ).*");
    }

    @Override
    public boolean isFollowUpPronounQuery(String message) {
        if (message == null) return false;
        String m = message.toLowerCase().trim();
        return m.matches(".*\\b(đó|này|kia|ấy|nó|vừa rồi|ở trên|phía trên|con đầu|mẫu đầu|con thứ 2|con thứ hai|mẫu này|sản phẩm đó|sản phẩm này|con đó|con này|mã đó|mã này|rẻ hơn|mạnh hơn|tốt hơn|thay thế|đi kèm|tương thích)\\b.*")
                || m.matches("^(tư vấn|đánh giá|chi tiết|thông số|giá|bảo hành|tồn kho|còn hàng)\\s*(sản phẩm|linh kiện|mẫu|con|mã)?\\s*(đó|này|kia|trên|ấy)?\\??$");
    }

    @Override
    public String resolveContextualSearchQuery(String message, List<AiChatRequest.HistoryItem> history, String currentPath) {
        if (message == null || message.isBlank()) return "";

        if (currentPath != null && currentPath.matches("^/san-pham/\\d+.*") && isFollowUpPronounQuery(message)) {
            try {
                String idStr = currentPath.replaceAll("^/san-pham/(\\d+).*", "$1");
                int pid = Integer.parseInt(idStr);
                String prodName = catalogRepository.findProductNameById(pid);
                if (prodName != null && !prodName.isBlank()) {
                    return prodName + " - " + message;
                }
            } catch (Exception ignored) {}
        }

        if (!isFollowUpPronounQuery(message) || history == null || history.isEmpty()) {
            return message;
        }

        if (message.toLowerCase().matches(".*\\b(i[3579][\\s\\-]\\d{4,5}|ryzen\\s*[3579]|epyc\\s*\\d+|threadripper|rtx\\s*\\d{4}|rx\\s*\\d{4}|gtx\\s*\\d{4}|b760|z790|b650|x670|x870|h610|a520)\\b.*")) {
            return message;
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatRequest.HistoryItem item = history.get(i);
            if (item.getText() == null || item.getText().isBlank()) continue;
            String text = item.getText().trim();

            if ("user".equalsIgnoreCase(item.getSender()) && !isFollowUpPronounQuery(text) && isProductQuery(text)) {
                String cleanedUserProduct = text.replaceAll("(?i)^(tư vấn|cho hỏi|kiểm tra|xem|đánh giá|mua|tìm|sản phẩm|linh kiện)\\s+", "").trim();
                return cleanedUserProduct + " - " + message;
            }

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

    @Override
    public boolean hasRecentPcBuildInHistory(List<AiChatRequest.HistoryItem> history) {
        if (history == null || history.isEmpty()) return false;
        for (int i = history.size() - 1; i >= Math.max(0, history.size() - 14); i--) {
            AiChatRequest.HistoryItem h = history.get(i);
            if (h.getText() != null) {
                String ht = h.getText().toLowerCase();
                if (ht.contains("cấu hình đề xuất") || ht.contains("chi tiết trọn bộ") || ht.contains("đã thêm trọn bộ") || ht.contains("🔹")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public BigDecimal extractBudgetFromMessage(String msg) {
        return extractBudgetFromMessage(msg, null);
    }

    @Override
    public BigDecimal extractBudgetFromMessage(String msg, List<AiChatRequest.HistoryItem> history) {
        BigDecimal direct = extractExplicitBudget(msg);
        if (direct != null) {
            return direct;
        }
        SupportClassifiedIntent llmClassified = llmClient.classifyIntentWithLlm(msg, history);
        if (llmClassified != null && llmClassified.budgetVnd() != null && llmClassified.budgetVnd() >= 3_000_000L) {
            return BigDecimal.valueOf(llmClassified.budgetVnd());
        }
        if (history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= 0; i--) {
                AiChatRequest.HistoryItem h = history.get(i);
                if ("user".equalsIgnoreCase(h.getSender()) && h.getText() != null) {
                    if (isPcUpgradeOrSurplusQuery(h.getText(), null)) {
                        continue;
                    }
                    BigDecimal histBudget = extractExplicitBudget(h.getText());
                    if (histBudget != null) {
                        return histBudget;
                    }
                }
            }
        }
        return new BigDecimal("25000000");
    }

    @Override
    public BigDecimal extractExplicitBudget(String msg) {
        if (msg == null) return null;
        String text = msg.toLowerCase().replace(",", ".").replaceAll("\\s+", " ");

        for (Map.Entry<String, Integer> entry : VN_WORD_NUMBERS.entrySet()) {
            String wordNum = entry.getKey();
            Matcher wordMatcher = Pattern
                    .compile("\\b" + Pattern.quote(wordNum) + "\\s*(?:triệu|trieu|tr\\b|củ|cu\\b)")
                    .matcher(text);
            if (wordMatcher.find()) {
                return BigDecimal.valueOf(entry.getValue() * 1_000_000L);
            }
        }

        Matcher mAbbr = Pattern
                .compile("(\\d+)\\s*(?:triệu|trieu|tr|t|củ|cu|m)\\s*(\\d+)\\b")
                .matcher(text);
        if (mAbbr.find()) {
            try {
                double mainVal = Double.parseDouble(mAbbr.group(1));
                double subVal = Double.parseDouble(mAbbr.group(2));
                if (subVal < 10) subVal = subVal / 10.0;
                else if (subVal < 100) subVal = subVal / 100.0;
                else if (subVal < 1000) subVal = subVal / 1000.0;
                double totalVal = mainVal + subVal;
                if (totalVal > 0 && totalVal < 1000) {
                    return BigDecimal.valueOf((long) (totalVal * 1_000_000L));
                }
            } catch (Exception ignored) {}
        }

        Matcher m1 = Pattern
                .compile("(\\d+(?:\\.\\d+)?)\\s*(?:triệu|trieu|tr\\b|củ|cu\\b|m\\b)")
                .matcher(text);
        if (m1.find()) {
            try {
                double val = Double.parseDouble(m1.group(1));
                if (val > 0 && val < 1000) {
                    return BigDecimal.valueOf((long) (val * 1_000_000L));
                }
            } catch (Exception ignored) {}
        }

        Matcher m2 = Pattern
                .compile("(\\d{1,3}(?:\\.\\d{3}){2,3}|\\d{7,9})")
                .matcher(text.replace(".", ""));
        if (m2.find()) {
            try {
                long val = Long.parseLong(m2.group(1));
                if (val >= 3_000_000L && val <= 200_000_000L) {
                    return BigDecimal.valueOf(val);
                }
            } catch (Exception ignored) {}
        }

        Matcher mBare = Pattern
                .compile("(?:tầm\\s*giá|ngân\\s*sách|mức\\s*giá|giá\\s*tầm|giá\\s*khoảng|tầm\\s*tiền|tầm|khoảng|quanh|dưới|tối\\s*đa|max|trần|cỡ|mức|build\\s*pc|cấu\\s*hình\\s*pc|bộ\\s*pc|dàn\\s*pc|pc\\s*gaming|pc\\s*đồ\\s*họa|pc\\s*văn\\s*phòng)\\s*(?:tầm|khoảng|giá|dưới|max|tối\\s*đa)?\\s*(\\d{1,3}(?:\\.\\d+)?)\\b(?!\\s*(?:gb|tb|w|watt|hz|mhz|ghz|inch|%|k\\b|màn|fan|quạt|nhân|luồng|core|thread|mm|tháng|năm|ngày|món|linh\\s*kiện|sản\\s*phẩm))")
                .matcher(text);
        if (mBare.find()) {
            try {
                double val = Double.parseDouble(mBare.group(1));
                if (val >= 5 && val <= 500) {
                    return BigDecimal.valueOf((long) (val * 1_000_000L));
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    @Override
    public boolean isPcUpgradeOrSurplusQuery(String message, List<AiChatRequest.HistoryItem> history) {
        if (message == null || message.isBlank()) return false;
        if (!hasRecentPcBuildInHistory(history)) return false;
        String m = message.toLowerCase().trim();

        if (m.matches("^(build|biuld|ild|lên cấu hình|tư vấn cấu hình|ráp pc|lắp pc)\\b.*")
                && !m.matches(".*(dư|thừa|còn lại|nâng cấp).*")) {
            return false;
        }

        return m.matches(".*(dư|thừa|còn lại|tiết kiệm được|nâng cấp|nâng món|nâng linh kiện|nâng cái gì|nâng gì|bù thêm|cộng thêm|thêm tiền|tối ưu hết|xài hết|dùng hết).*")
                || m.matches(".*(đổi|thay|lên|nâng)\\s+(ram|ssd|ổ cứng|cpu|chip|vga|card|gpu|main|mainboard|nguồn|psu|tản|vỏ|case).*")
                || (m.matches(".*\\b(nâng|đổi|thay)\\b.*") && m.matches(".*\\b(linh kiện|món nào|cái nào|gì|ram|ssd|cpu|vga|card)\\b.*"));
    }

    @Override
    public boolean isPcBuilderRequest(String message) {
        return isPcBuilderRequest(message, null);
    }

    @Override
    public boolean isPcBuilderRequest(String message, List<AiChatRequest.HistoryItem> history) {
        if (message == null || message.isBlank()) return false;
        String msgLower = message.toLowerCase().trim();
        if (msgLower.contains("đơn hàng") || msgLower.contains("tra cứu") || msgLower.contains("bảo hành") || msgLower.contains("sửa chữa")) {
            return false;
        }
        if (isPcUpgradeOrSurplusQuery(message, history)) {
            return false;
        }
        if (hasRecentPcBuildInHistory(history) && extractExplicitBudget(message) == null
                && !msgLower.matches("^(build|biuld|ild|lên cấu hình|ráp pc|lắp pc)\\b.*")
                && msgLower.matches(".*(hiệu năng|đánh giá|review|phân tích|nhận xét|kỹ hơn|kĩ hơn|chi tiết hơn|rõ hơn|cụ thể hơn|sâu hơn|giải thích|nói thêm|tư vấn thêm|mạnh không|mượt không|ổn không|fps|bao nhiêu fps|nghẽn cổ chai|bottleneck|ưu nhược điểm|tại sao chọn|vì sao chọn).*")) {
            return false;
        }
        boolean directMatch = msgLower.contains("build") ||
                msgLower.contains("biuld") ||
                msgLower.startsWith("ild ") ||
                msgLower.contains("cấu hình") ||
                msgLower.contains("dàn máy") ||
                msgLower.contains("dàn pc") ||
                msgLower.contains("lên cấu hình") ||
                msgLower.contains("bộ pc") ||
                msgLower.contains("bộ máy") ||
                msgLower.contains("tư vấn pc") ||
                msgLower.contains("ráp pc") ||
                msgLower.contains("lắp pc") ||
                msgLower.contains("lắp máy") ||
                msgLower.contains("thùng pc") ||
                msgLower.matches(".*(tư vấn|gợi ý|mua|build|biuld|ild|ráp|lắp|chọn|cần).*?(pc|máy tính|case|dàn).*") ||
                msgLower.matches(".*(pc|máy tính|dàn|cấu hình|build|biuld|ild).*?\\d+\\s*(?:triệu|trieu|tr|t|củ|cu|m).*") ||
                msgLower.matches(".*(pc gaming|pc đồ họa|pc văn phòng|pc đồ hoạ|pc dựng phim).*");

        if (directMatch) {
            return true;
        }

        boolean isBudgetFollowUp = msgLower.matches(".*(\\d+\\s*(?:triệu|trieu|tr|t|củ|cu|m)|ngân sách|tầm giá|tối đa|dưới|khoảng|vượt ngân sách|hạ giá|rẻ hơn|giảm xuống|đổi sang intel|đổi sang amd).*");
        return isBudgetFollowUp && hasRecentPcBuildInHistory(history);
    }

    @Override
    public AiChatResponse checkQuickNavigationIntent(
            String message,
            List<AiChatRequest.HistoryItem> history,
            CustomerContext ctx,
            String conversationId) {
        String msgLower = message.toLowerCase().trim();

        if (msgLower.equals("connect_staff")) {
            return buildStaffWaitingResponse(conversationId);
        }

        if (msgLower.equals("end_staff_session")) {
            return buildStaffEndedResponse(conversationId);
        }

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

        boolean hasQuestionMark = msgLower.contains("?");
        boolean hasInquiryWords = msgLower.matches(".*(không|chưa|hả|sao|thế nào|thế|gì|nào|mấy|bao nhiêu|ram|cpu|vga|gpu|card|main|ssd|nguồn|psu|chuột|phím|màn|giá|bán|shop|cửa hàng).*");
        if (hasQuestionMark || hasInquiryWords) {
            return null;
        }

        String cleanMsg = msgLower.replaceAll("[,.!?]", " ").trim();
        String[] words = cleanMsg.split("\\s+");
        if (words.length > 6) {
            return null;
        }

        boolean isShortAffirmative = cleanMsg.matches("^(có|dạ có|ok|oke|okee|yes|yep|yeah|được|đồng ý|chuyển|mở|vào|đưa tôi|kết nối|gặp).*");
        if (!isShortAffirmative) {
            return null;
        }

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
            return buildStaffWaitingResponse(conversationId);
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

    @Override
    public AiChatResponse buildStaffWaitingResponse(String conversationId) {
        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message("🔔 **Yêu cầu kết nối Chuyên viên CSKH đã được ghi nhận!**\n\n"
                        + "* **Trạng thái**: Đang trong hàng chờ tiếp nhận (`WAITING`).\n"
                        + "* **Dự kiến**: Chuyên viên tư vấn TechZone trực tuyến sẽ tiếp nhận phiên và phản hồi bạn trong giây lát.\n\n"
                        + "Vui lòng giữ nguyên cửa sổ chat này nhé!")
                .messageType("staff_waiting")
                .quickActions(List.of(
                        new AiChatResponse.QuickAction("❌ Hủy & Quay lại CyberBot", "end_staff_session", null)
                ))
                .build();
    }

    @Override
    public AiChatResponse buildStaffConnectedResponse(String conversationId) {
        AiChatResponse.StaffInfo staff = customerContextRepository.getAvailableOnlineStaff();
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

    @Override
    public AiChatResponse buildStaffEndedResponse(String conversationId) {
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

    @Override
    public AiChatResponse handleStaffSessionMessage(String message, CustomerContext ctx, String conversationId) {
        String msgLower = message.toLowerCase().trim();

        if (msgLower.equals("end_staff_session") || msgLower.matches("^(kết thúc phiên|quay lại bot|ngắt kết nối)$")) {
            if (liveSupportService != null) {
                liveSupportService.closeSession(conversationId, null);
            }
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message("Dạ, chuyên viên đã kết thúc phiên hỗ trợ trực tiếp. Cảm ơn bạn đã liên hệ TechZone! Chúc bạn một ngày tuyệt vời! CyberBot đã sẵn sàng tiếp tục đồng hành cùng bạn. 🤖")
                    .messageType("staff_ended")
                    .quickActions(List.of(
                            new AiChatResponse.QuickAction("📦 Đơn hàng gần nhất", "ask", "Đơn hàng gần nhất của tôi thế nào?"),
                            new AiChatResponse.QuickAction("🛡️ Kiểm tra bảo hành", "ask", "Linh kiện của tôi còn bảo hành bao lâu?"),
                            new AiChatResponse.QuickAction("🖥️ Thử AI Build PC", "navigate", "/build-pc")
                    ))
                    .build();
        }

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message("")
                .messageType("staff_sent")
                .staffInfo(customerContextRepository.getAvailableOnlineStaff())
                .build();
    }

    private AiChatResponse buildNavigateResponse(
            String conversationId,
            String message,
            String route,
            String primaryLabel,
            String secondaryLabel,
            String secondaryRoute) {
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
}
