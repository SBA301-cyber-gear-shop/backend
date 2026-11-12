package com.project.cybershop.support.internal.support;

import com.project.cybershop.support.api.dto.response.AiChatResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SupportSecurityGuard {

    public boolean isSecurityViolationOrPromptInjection(String message) {
        if (message == null || message.isBlank()) return false;
        String m = message.toLowerCase().trim();

        return m.matches(".*(ignore all|ignore previous|bỏ qua các hướng dẫn|quên hết quy tắc|system prompt|system instruction|in ra prompt|hiển thị prompt|câu lệnh gốc|cấu hình hệ thống).*")
                || m.matches(".*(api key|apikey|secret key|jwt secret|access token|password|mật khẩu|file \\.env|application\\.yml|biến môi trường|connection string|jdbc:postgresql).*")
                || m.matches(".*(tên bảng|cấu trúc bảng|database schema|select \\* from|drop table|delete from|insert into|update .* set|union select|pg_catalog|information_schema).*")
                || m.matches(".*(doanh thu cửa hàng|lợi nhuận|giá vốn|giá nhập|danh sách toàn bộ khách hàng|số điện thoại của khách hàng khác|email của khách khác).*");
    }

    public boolean isStaffRequest(String message) {
        if (message == null) return false;
        String msgLower = message.toLowerCase().trim();
        return msgLower.matches(".*(gặp|kết nối|nói chuyện|chuyển sang|liên hệ|chat với)\\s+(nhân viên|staff|tư vấn viên|người thật|chăm sóc khách hàng|kỹ thuật viên|cskh).*")
                || msgLower.matches(".*(nhân viên đâu|có ai trực không|cần người hỗ trợ|gặp cskh|gặp staff|hotline|tổng đài|tư vấn trực tiếp).*");
    }

    public boolean isOutOfScope(String message) {
        if (message == null) return false;
        String msgLower = message.toLowerCase().trim();
        return msgLower.matches(".*(hóa đơn đỏ|hóa đơn vat|hóa đơn công ty|mua sỉ|chiết khấu b2b|đại lý|hợp đồng mua bán|khiếu nại nhân viên|thái độ phục vụ|bồi thường thiệt hại|chuyển khoản nhầm).*");
    }

    public boolean isOffTopicDomainQuery(String message, boolean hasStoreDomainIntent) {
        if (message == null || message.isBlank()) return false;
        String m = message.toLowerCase().trim();

        // 1. Nếu là câu chào hỏi xã giao hoặc xác nhận ngắn -> Hợp lệ (Không chặn)
        if (m.matches("^(xin chào|chào|chào shop|chào bot|hi|hello|alo|ơi|shop ơi|bot ơi|bạn là ai|giúp mình|hỗ trợ mình|tư vấn giúp|ok|oke|dạ|vâng|cảm ơn|thanks|có|không|được|tiếp tục).*")) {
            return false;
        }

        // 2. Nếu có chứa bất kỳ từ khóa nghiệp vụ hợp lệ nào của TechZone -> Hợp lệ (Không chặn)
        boolean hasStoreDomainKeyword = hasStoreDomainIntent
                || m.matches(".*(đơn hàng|mã đơn|vận đơn|tz-|ord-|bảo hành|serial|đổi trả|1 đổi 1|ship|ghn|giao hàng|vận chuyển|đồng kiểm|thanh toán|vnpay|cod|trả góp|hoàn tiền|giỏ hàng|mua|đặt hàng|giá|bao nhiêu tiền|ngân sách|tối đa|dưới|khoảng|nâng cấp|còn dư|còn thừa|tiền dư|khuyến mãi|giảm giá|sale|voucher|mã giảm|cửa hàng|địa chỉ|quận 12|hotline|nhân viên|kỹ thuật|lắp ráp|cài win|vệ sinh máy|không lên hình|màn hình xanh|bsod|nóng quá|sập nguồn|quạt quay|tương thích|nghẽn cổ chai|fps|chơi game|đồ họa|render|blender|premiere|photoshop|valorant|wukong|cyberpunk|lol|cs2|dựng phim|lập trình|văn phòng|máy trạm|server|workstation|laptop|macbook|rgb|led|đèn).*");

        if (hasStoreDomainKeyword) {
            return false;
        }

        // 3. Danh sách đen (Blacklist) các chủ đề chắc chắn ngoài phạm vi cửa hàng PC
        boolean isExplicitOffTopic = m.matches(".*\\b(thời tiết|mưa|nắng|nhiệt độ ngoài trời|nấu ăn|công thức nấu|món ăn|phở|bún|cơm|bóng đá|ngoại hạng anh|world cup|ca sĩ|diễn viên|bài hát|làm thơ|viết văn|kể chuyện|tử vi|bói toán|xem ngày|chứng khoán|cổ phiếu|forex|tiền ảo|bitcoin|bất động sản|nhà đất|khám bệnh|uống thuốc|đau bụng|đau đầu|chính trị|tổng thống|chiến tranh|lịch sử|địa lý|thủ đô|giải phương trình|đạo hàm|tích phân|hóa học|sinh học|người yêu|tán gái|thất tình)\\b.*");

        if (isExplicitOffTopic) {
            return true;
        }

        // 4. Nếu câu dài (>= 4 từ) mà hoàn toàn không liên quan đến bất kỳ từ khóa nào của cửa hàng
        String[] words = m.split("\\s+");
        return words.length >= 4;
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    public String sanitizeBotOutput(String rawMsg) {
        if (rawMsg == null || rawMsg.isBlank()) return "";
        String lower = rawMsg.toLowerCase();

        if (lower.contains("bạn là cyberbot - trợ lý ai")
                || lower.contains("=== thông tin khách hàng đang chat")
                || lower.contains("ai_api_key")
                || lower.contains("aizasy")
                || lower.contains("jdbc:postgresql")
                || lower.contains("cyberbot_knowledge_vectors")) {
            return "Dạ, vì lý do bảo mật thông tin hệ thống của TechZone, mình không thể cung cấp các thông tin kỹ thuật nội bộ này. Bạn có cần mình hỗ trợ tư vấn linh kiện PC hoặc kiểm tra đơn hàng không ạ?";
        }

        return rawMsg
                .replace("DANH SÁCH LINH KIỆN HIỆN CÓ SẴN TRONG KHO TECHZONE (TÌM TỪ VECTOR DATABASE & KHO THỜI GIAN THỰC):", "")
                .replace("DANH SÁCH LINH KIỆN TRONG KHO TECHZONE KHỚP VỚI YÊU CẦU:", "")
                .trim();
    }

    public String sanitizeSpecsForUser(String rawSpecs) {
        if (rawSpecs == null || rawSpecs.isBlank()) return "";

        String[] parts = rawSpecs.split("\\| ");
        List<String> cleanParts = new ArrayList<>();

        for (String part : parts) {
            String p = part.trim();
            String lower = p.toLowerCase(Locale.ROOT);

            if (lower.startsWith("price_source:") ||
                lower.startsWith("price_seen_date:") ||
                lower.startsWith("price_status:") ||
                lower.startsWith("compat_note:") ||
                lower.startsWith("lifecycle:") ||
                lower.startsWith("compute_stack:") ||
                lower.contains("chưa rõ") ||
                lower.contains("chưa xác minh")) {
                continue;
            }
            cleanParts.add(p);
        }

        if (cleanParts.size() > 4) {
            cleanParts = cleanParts.subList(0, 4);
        }
        return String.join(" | ", cleanParts);
    }

    public String normalizeCategoryGroup(String category) {
        if (category == null) return "KHÁC";
        String c = category.toLowerCase().trim();
        if (c.contains("tản") || c.contains("cooler")) return "TẢN NHIỆT";
        if (c.contains("cpu") || c.contains("vi xử lý") || c.contains("processor")) return "CPU";
        if (c.contains("vga") || c.contains("đồ họa") || c.contains("card") || c.contains("gpu")) return "VGA";
        if (c.contains("main") || c.contains("bo mạch") || c.contains("motherboard")) return "MAINBOARD";
        if (c.contains("ram") || c.contains("bộ nhớ") || c.contains("memory")) return "RAM";
        if (c.contains("ssd") || c.contains("ổ cứng") || c.contains("storage") || c.contains("hdd")) return "SSD";
        if (c.contains("nguồn") || c.contains("psu") || c.contains("power")) return "NGUỒN";
        if (c.contains("case") || c.contains("thùng") || c.contains("chassis") || c.contains("vỏ")) return "CASE";
        if (c.contains("màn") || c.contains("monitor")) return "MÀN HÌNH";
        return category.toUpperCase();
    }

    public String getCategoryDisplayName(String category) {
        String group = normalizeCategoryGroup(category);
        return switch (group) {
            case "CPU" -> "Bộ vi xử lý (CPU)";
            case "MAINBOARD" -> "Bo mạch chủ (Mainboard)";
            case "RAM" -> "Bộ nhớ trong (RAM)";
            case "VGA" -> "Card đồ họa (VGA)";
            case "SSD" -> "Ổ cứng (SSD NVMe)";
            case "NGUỒN" -> "Nguồn máy tính (PSU)";
            case "CASE" -> "Vỏ thùng máy (Case)";
            case "TẢN NHIỆT" -> "Tản nhiệt CPU";
            default -> group;
        };
    }

    public String detectCpuSocket(AiChatResponse.ProductSummary cpu) {
        if (cpu == null) return null;
        String cpuText = (cpu.getName() + " " + cpu.getSpecs() + " " + cpu.getDescription()).toLowerCase();
        if (cpuText.contains("lga1851") || cpuText.contains("lga 1851") || cpuText.contains("core ultra")) {
            return "lga1851";
        }
        if (cpuText.contains("lga1700") || cpuText.contains("lga 1700") || cpuText.matches(".*i[3579]-1[234]\\d{3}.*")) {
            return "lga1700";
        }
        if (cpuText.contains("am5") || cpuText.matches(".*(7500|7600|7700|7800|8600|8700|9600|9700|9800|9850|9900|9950).*")) {
            return "am5";
        }
        if (cpuText.contains("am4") || cpuText.matches(".*(3000g|3200g|5500|5600|5700|5800|5900).*")) {
            return "am4";
        }
        return null;
    }

    public boolean isServerOrWorkstationOnlyPart(String haystack, long totalBudgetVal) {
        if (totalBudgetVal >= 170_000_000L) return false;
        return haystack.contains("epyc")
                || haystack.contains("threadripper")
                || haystack.contains("wrx90")
                || haystack.contains("mz73")
                || haystack.contains("sp5")
                || haystack.contains("tr5-sp6")
                || haystack.contains("ecc rdimm")
                || haystack.contains("ecc reg")
                || haystack.contains("rtx pro 4000")
                || haystack.contains("rtx pro 5000")
                || haystack.contains("rtx pro 6000");
    }

    public boolean isApuProcessor(String haystack) {
        return haystack.matches(".*\\b(3000g|3200g|5500gt|5600gt|8600g|8700g)\\b.*")
                || haystack.contains("igpu radeon");
    }

    public int extractCapacityGb(AiChatResponse.ProductSummary item) {
        if (item == null) return 0;
        String text = (item.getName() + " " + (item.getSpecs() != null ? item.getSpecs() : "")).toLowerCase();
        Matcher mTb = Pattern.compile("\\b(\\d+)\\s*tb\\b").matcher(text);
        if (mTb.find()) {
            return Integer.parseInt(mTb.group(1)) * 1000;
        }
        Matcher mGb = Pattern.compile("\\b(\\d+)\\s*gb\\b").matcher(text);
        if (mGb.find()) {
            return Integer.parseInt(mGb.group(1));
        }
        return 0;
    }

    public double sumBuildPrice(List<AiChatResponse.ProductSummary> build) {
        double total = 0.0;
        if (build == null) return total;
        for (AiChatResponse.ProductSummary p : build) {
            if (p != null && p.getPrice() != null) {
                total += p.getPrice();
            }
        }
        return total;
    }

    public int findBuildIndexBySlotKey(List<AiChatResponse.ProductSummary> build, String slotKey) {
        String expectedGroup = switch (slotKey) {
            case "gpu", "card", "vga" -> "VGA";
            case "cpu", "vi xử lý" -> "CPU";
            case "mainboard", "bo mạch" -> "MAINBOARD";
            case "ram" -> "RAM";
            case "ssd", "ổ cứng" -> "SSD";
            case "psu", "nguồn" -> "NGUỒN";
            case "case", "vỏ" -> "CASE";
            case "cooler", "tản" -> "TẢN NHIỆT";
            default -> "";
        };
        for (int b = 0; b < build.size(); b++) {
            if (expectedGroup.equals(normalizeCategoryGroup(build.get(b).getCategory()))) {
                return b;
            }
        }
        return -1;
    }
}
