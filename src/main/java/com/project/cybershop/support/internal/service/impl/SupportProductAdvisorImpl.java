package com.project.cybershop.support.internal.service.impl;

import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.repository.SupportCatalogLookupRepository;
import com.project.cybershop.support.internal.repository.SupportCustomerContextRepository;
import com.project.cybershop.support.internal.service.SupportProductAdvisor;
import com.project.cybershop.support.internal.support.SupportLlmClient;
import com.project.cybershop.support.internal.support.SupportSecurityGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportProductAdvisorImpl implements SupportProductAdvisor {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    private final SupportCatalogLookupRepository catalogRepository;
    private final SupportCustomerContextRepository customerContextRepository;
    private final SupportSecurityGuard securityGuard;
    private final SupportLlmClient llmClient;

    @Override
    public AiChatResponse buildSmartProductAdvisory(
            String message,
            String resolvedQuery,
            List<AiChatResponse.ProductSummary> products,
            String conversationId) {
        StringBuilder reply = new StringBuilder();
        List<AiChatResponse.QuickAction> actions = new ArrayList<>();
        String messageType = "info";
        String messageRoute = null;

        if (products == null || products.isEmpty()) {
            reply.append("Dạ, CyberBot đã tra cứu kho linh kiện TechZone nhưng hiện tại chưa tìm thấy linh kiện khớp chính xác với yêu cầu: **").append(resolvedQuery).append("**.\n\n");
            reply.append("* Bạn có thể kiểm tra lại tên mã hoặc liên hệ chuyên viên để shop hỗ trợ kiểm tra nguồn hàng.\n");
            reply.append("* Bạn cũng có thể nhắn CyberBot để được tư vấn các linh kiện tương đương có sẵn tại cửa hàng!");
            actions.add(new AiChatResponse.QuickAction("🛒 Xem giỏ hàng", "navigate", "/gio-hang"));
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

            actions.add(new AiChatResponse.QuickAction("🛒 Thêm tất cả vào giỏ hàng", "add_all_to_cart", "add_all_to_cart"));
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
            String cleanPrimarySpecs = securityGuard.sanitizeSpecsForUser(primary.getSpecs() != null ? primary.getSpecs() : primary.getDescription());
            if (!cleanPrimarySpecs.isBlank()) {
                reply.append("* **Thông số kỹ thuật cốt lõi**: ").append(cleanPrimarySpecs).append("\n");
            } else if (primary.getDescription() != null && !primary.getDescription().isBlank()) {
                reply.append("* **Thông số nổi bật**: ").append(primary.getDescription().trim()).append("\n");
            }

            reply.append("\n#### 🔍 Đánh giá chuyên môn & Tư vấn thực tế từ Kỹ sư TechZone:\n");
            reply.append(buildExpertHardwareInsightForProduct(primary)).append("\n");
            String productFacts = primary.getName() + " | Giá: " + (primary.getPrice() != null ? MONEY_FMT.format(primary.getPrice()) : "Liên hệ")
                    + " | Tồn kho: " + primary.getStock() + " | Thông số: " + cleanPrimarySpecs;
            String aiProductCommentary = llmClient.generateGroundedCommentary("PRODUCT_CONSULT", message, productFacts, null);
            if (aiProductCommentary != null && !aiProductCommentary.isBlank()) {
                reply.append("* **Giải đáp trực tiếp nhu cầu của bạn**: ").append(aiProductCommentary).append("\n");
            }

            if (primary.getPrice() != null && primary.getPrice() > 0) {
                String promoAdvisory = customerContextRepository.buildDynamicPromotionAdvisory(primary.getPrice());
                if (!promoAdvisory.isBlank()) {
                    reply.append(promoAdvisory);
                }
            }

            if (products.size() > 1) {
                reply.append("\n**Các lựa chọn / sản phẩm liên quan đang sẵn hàng tại kho:**\n");
                for (int i = 1; i < Math.min(products.size(), 4); i++) {
                    AiChatResponse.ProductSummary other = products.get(i);
                    String otherSpecs = securityGuard.sanitizeSpecsForUser(other.getSpecs() != null ? other.getSpecs() : other.getDescription());
                    reply.append("• **").append(other.getName()).append("** (").append(securityGuard.getCategoryDisplayName(other.getCategory())).append("): **")
                            .append(other.getPrice() != null ? MONEY_FMT.format(other.getPrice()) : "Liên hệ").append("**")
                            .append(other.getStock() != null && other.getStock() > 0 ? " — _Sẵn " + other.getStock() + " chiếc_" : " — _Tạm hết hàng_")
                            .append(otherSpecs.isBlank() ? "" : " | _" + otherSpecs + "_")
                            .append("\n");
                }
            }

            reply.append("\n👉 Bạn có thể nhấn vào thẻ sản phẩm bên dưới để xem đầy đủ thông số kỹ thuật hoặc bấm **Thêm vào giỏ hàng**!");

            messageRoute = "/san-pham/" + primary.getId();
            actions.add(new AiChatResponse.QuickAction("🛒 Thêm vào giỏ hàng", "add_all_to_cart", "add_all_to_cart"));
            actions.add(new AiChatResponse.QuickAction("Xem chi tiết sản phẩm", "navigate", messageRoute));
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

    @Override
    public String buildExpertHardwareInsightForProduct(AiChatResponse.ProductSummary p) {
        if (p == null) return "";
        String catGroup = securityGuard.normalizeCategoryGroup(p.getCategory());
        String haystack = (p.getName() + " " + (p.getVariantName() != null ? p.getVariantName() : "") + " "
                + (p.getSpecs() != null ? p.getSpecs() : "") + " " + (p.getDescription() != null ? p.getDescription() : "")).toLowerCase();
        StringBuilder sb = new StringBuilder();

        switch (catGroup) {
            case "CPU" -> {
                String socket = securityGuard.detectCpuSocket(p);
                String socketUpper = socket != null ? socket.toUpperCase() : "Chuẩn hãng";
                if (haystack.contains("x3d")) {
                    sb.append("* **Sức mạnh kiến trúc 3D V-Cache**: Dòng vi xử lý chuyên dụng đỉnh cao cho Gaming nhờ bộ nhớ đệm L3 xếp chồng dung lượng cực lớn (96MB+), giúp xử lý lệnh dựng hình (Draw Calls) tức thì, đẩy FPS game eSports/AAA vượt trội hơn 25–35% và giữ mức `1% Low FPS` cực kỳ ổn định không bị drop khung hình.\n");
                } else if (haystack.matches(".*(14700|14900|270k|285k|9900x|9950x).*")) {
                    sb.append("* **Hiệu năng Đa nhân / Workstation vượt trội**: Sở hữu số lượng nhân/luồng lớn cùng xung nhịp Boost cao, xử lý cực nhanh các tác vụ dựng hình 3D (Blender, Maya), Render video 4K, biên dịch code nặng đồng thời chiến mượt mọi tựa game AAA ở thiết lập đồ họa cao nhất.\n");
                } else if (securityGuard.isApuProcessor(haystack)) {
                    sb.append("* **Đồ họa tích hợp (APU) đa năng**: Tích hợp sẵn nhân đồ họa Radeon mạnh mẽ ngay trong CPU, cho phép xuất hình làm việc đồ họa 2D và chơi mượt các tựa game eSports (LMHT, Valorant, FC Online) mà chưa cần lắp thêm Card đồ họa rời.\n");
                } else {
                    sb.append("* **Hiệu năng trên giá thành (P/P) tối ưu**: Xung nhịp đơn nhân (Single-Core) cao kết hợp kiến trúc đa luồng hiện đại, đảm bảo kéo mượt các dòng Card đồ họa rời tầm trung – cận cao cấp (như RTX 4060 / RTX 5060 / RTX 5070) mà không xảy ra hiện tượng nghẽn cổ chai.\n");
                }
                if ("am5".equals(socket)) {
                    sb.append("* **Khuyến nghị phối ghép**: Tương thích chuẩn **Socket AM5** (đi cùng Mainboard **B650 / X670 / X870**) và bắt buộc sử dụng **RAM chuẩn DDR5** (tối ưu nhất ở Bus 6000MHz AMD EXPO). Nền tảng AM5 hỗ trợ nâng cấp CPU thế hệ mới lâu dài 3–5 năm không cần thay bo mạch chủ.");
                } else if ("lga1851".equals(socket)) {
                    sb.append("* **Khuyến nghị phối ghép**: Tương thích chuẩn **Socket LGA1851** thế hệ mới (đi cùng Mainboard **B860 / Z890**) và **RAM chuẩn DDR5** băng thông cao.");
                } else if ("lga1700".equals(socket)) {
                    sb.append("* **Khuyến nghị phối ghép**: Tương thích chuẩn **Socket LGA1700** (đi cùng Mainboard **B760 / Z790**; khuyến nghị bản **DDR5** để tối ưu băng thông cho các tựa game và ứng dụng đời mới).");
                } else {
                    sb.append("* **Khuyến nghị phối ghép**: Tương thích chuẩn **Socket ").append(socketUpper).append("** (đi cùng Mainboard **B450 / B550** và **RAM DDR4 3200MHz/3600MHz** tiết kiệm chi phí).");
                }
            }
            case "VGA" -> {
                if (haystack.contains("5080") || haystack.contains("5090") || haystack.contains("4080") || haystack.contains("4090")) {
                    sb.append("* **Phân khúc Hiệu năng**: Flagship 4K Ultra & AI Workstation — Chinh phục mọi tựa game AAA nặng nhất ở độ phân giải **4K Max Setting + Full Ray Tracing** với công nghệ **NVIDIA DLSS 4 Multi-Frame Generation**, đồng thời sở hữu lượng nhân CUDA/Tensor khổng lồ cho huấn luyện AI và Render 3D chuyên nghiệp.\n");
                    sb.append("* **Khuyến nghị nguồn & lắp đặt**: Cần bộ nguồn công suất thực từ **850W – 1000W+ chuẩn ATX 3.1 (cáp PCIe 5.0 12V-2x6)** và vỏ Case không gian rộng rãi.");
                } else if (haystack.contains("5070") || haystack.contains("4070") || haystack.contains("9070") || haystack.contains("7800 xt")) {
                    sb.append("* **Phân khúc Hiệu năng**: Chiến thần Gaming **2K (1440p) Ultra & 4K** — Cân mượt mọi bom tấn AAA (Black Myth: Wukong, Cyberpunk 2077) ở mức 100–140+ FPS, đồng thời tăng tốc phần cứng cực nhanh cho Blender Cycles (OptiX) và dựng phim Premiere 4K.\n");
                    sb.append("* **Khuyến nghị nguồn & CPU**: Phối ghép hoàn hảo với CPU **Core i5-14600K / i7-14700F / Ryzen 7 7800X3D / 9700X** và bộ nguồn **750W 80 Plus Bronze/Gold chuẩn ATX 3.1**.");
                } else {
                    sb.append("* **Phân khúc Hiệu năng**: Tối ưu hoàn hảo cho **Full HD (1080p) Ultra & 2K (1440p) High Setting** — Đạt 250–450+ FPS ở các tựa game eSports (Valorant, CS2, PUBG) và 75–110+ FPS ở các tựa game AAA hiện đại nhờ kiến trúc nhân đồ họa mới hỗ trợ **DLSS 4 / Ray Tracing / bộ mã hóa NVENC AV1** cho nhu cầu vừa chơi game vừa Livestream mượt mà.\n");
                    sb.append("* **Khuyến nghị nguồn & CPU**: Hoạt động mát mẻ, tiết kiệm điện năng; chỉ cần bộ nguồn thực từ **550W – 650W 80 Plus** và kết hợp tốt với mọi CPU từ **Core i5-13400F / 14400F** hoặc **Ryzen 5 7500F / Ryzen 7 7800X3D**.");
                }
            }
            case "RAM" -> {
                int cap = securityGuard.extractCapacityGb(p);
                boolean isDdr5 = haystack.contains("ddr5");
                sb.append("* **Đánh giá băng thông & Đa nhiệm**: Chuẩn bộ nhớ **").append(isDdr5 ? "DDR5 thế hệ mới" : "DDR4 ổn định cao").append("**");
                if (cap >= 32) {
                    sb.append(" dung lượng lớn **").append(cap).append("GB**, đáp ứng dư dả cho cả chiến game AAA nặng lẫn dựng hình 3D Blender, After Effects, chạy máy ảo và mở hàng chục tab tác vụ cùng lúc mà không bao giờ tràn bộ nhớ.\n");
                } else if (cap >= 16) {
                    sb.append(" dung lượng **16GB** — mức chuẩn tiêu chuẩn vàng cho Gaming hiện đại và làm việc đa nhiệm mượt mà. Bạn hoàn toàn có thể cắm thêm 1 thanh 16GB cùng mã sau này để nâng lên **32GB Dual-Channel** gấp đôi băng thông.\n");
                } else {
                    sb.append(" dung lượng **8GB**, phù hợp cho cấu hình văn phòng hoặc eSports phổ thông (khuyến nghị gắn 2 thanh chạy kênh đôi Dual-Channel để tối ưu FPS).\n");
                }
                sb.append("* **Lưu ý kỹ thuật**: Hỗ trợ profile ép xung tự động **Intel XMP 3.0 / AMD EXPO** trong BIOS giúp RAM chạy đúng tốc độ Bus công bố chỉ với 1 cú click.");
            }
            case "MAINBOARD" -> {
                boolean isDdr5 = haystack.contains("ddr5") || haystack.contains("b650") || haystack.contains("x870") || haystack.contains("b860") || haystack.contains("z890");
                boolean hasWifi = haystack.contains("wifi") || haystack.contains("wi-fi");
                sb.append("* **Độ bền mạch VRM & Kết nối**: Trang bị dàn phase nguồn VRM kỹ thuật số có giáp tản nhiệt dày dặn, đảm bảo cấp dòng điện ổn định cho CPU chạy Max Boost liên tục không bị tuột xung (VRM Throttling).\n");
                sb.append("* **Khả năng mở rộng**: Hỗ trợ chuẩn **RAM ").append(isDdr5 ? "DDR5" : "DDR4").append("**, khe cắm ổ cứng **SSD M.2 PCIe Gen4/Gen5** tốc độ cao");
                if (hasWifi) {
                    sb.append(" và **tích hợp sẵn module Wi-Fi + Bluetooth tốc độ cao** cực kỳ tiện lợi không cần đi dây mạng LAN.");
                } else {
                    sb.append(" cùng cổng mạng LAN tốc độ cao ổn định.");
                }
            }
            case "SSD" -> {
                int cap = securityGuard.extractCapacityGb(p);
                sb.append("* **Tốc độ truy xuất dữ liệu**: Chuẩn **NVMe M.2 PCIe** cho tốc độ đọc/ghi tuần tự vượt trội gấp 6–12 lần ổ SSD SATA truyền thống, giúp khởi động Windows chỉ trong **5–7 giây**, rút ngắn tối đa thời gian nạp màn chơi (loading screen) và đọc/ghi file video 4K / project đồ họa lớn.\n");
                sb.append("* **Không gian lưu trữ**: Dung lượng **").append(cap >= 1000 ? (cap / 1000) + "TB" : cap + "GB").append("** đáp ứng tốt cho hệ điều hành, phần mềm chuyên dụng và các tựa game yêu thích.");
            }
            case "NGUỒN" -> {
                sb.append("* **An toàn điện năng & Độ ổn định**: Đạt chứng nhận hiệu suất thực **80 Plus**, trang bị tụ điện chịu nhiệt cao và đầy đủ các mạch bảo vệ công nghiệp (**OVP, OPP, SCP, UVP**), giữ đường điện 12V cung cấp cho CPU và Card đồ họa luôn phẳng và an toàn tuyệt đối khi tải nặng liên tục.");
            }
            case "TẢN NHIỆT" -> {
                boolean isAio = haystack.contains("aio") || haystack.contains("360") || haystack.contains("240") || haystack.contains("nước");
                sb.append("* **Hiệu quả giải nhiệt**: ").append(isAio
                        ? "Tản nhiệt nước **AIO** hiệu suất cao với két nước Radiator lớn và bơm tốc độ cao, giúp kiểm soát nhiệt độ các dòng CPU đa nhân (Core i7/i9, Ryzen 7/9) luôn mát mẻ ở mức **60–72°C** khi full-load kèm hiệu ứng thẩm mỹ chuẩn Show-case."
                        : "Tản nhiệt khí tháp đồng ống dẫn nhiệt trực tiếp (Heatpipes), vận hành cực kỳ bền bỉ, êm ái và giữ nhiệt độ CPU ổn định hơn vượt trội so với tản nhiệt mặc định.");
            }
            case "CASE" -> {
                sb.append("* **Không gian lắp đặt & Luồng gió (Airflow)**: Thiết kế mặt kính cường lực hiện đại kết hợp khoang giấu dây rộng rãi, tối ưu luồng gió đối lưu hút khí mát từ mặt trước/đáy và đẩy khí nóng ra sau/nóc, giúp toàn bộ linh kiện bên trong luôn thoáng mát.");
            }
            default -> {
                sb.append("* **Đánh giá thực tế**: Sản phẩm chính hãng phân phối tại TechZone với độ hoàn thiện cao, thông số chuẩn xác đáp ứng tốt nhu cầu làm việc và giải trí cường độ cao, hưởng trọn đặc quyền bảo hành 1 đổi 1 trong 30 ngày đầu.");
            }
        }
        return sb.toString();
    }

    @Override
    public AiChatResponse buildCompatibilityAdvisory(String message, String conversationId) {
        StringBuilder reply = new StringBuilder();
        String m = message != null ? message.toLowerCase() : "";

        boolean mentionsIntelCpu = m.matches(".*\\b(intel|core i|i3|i5|i7|i9|12100|12400|13400|14400|14600|14700|14900|lga1700)\\b.*");
        boolean mentionsCoreUltra = m.matches(".*\\b(core ultra|245k|265k|270k|285k|lga1851)\\b.*");
        boolean mentionsAmdAm5Cpu = m.matches(".*\\b(ryzen|7500f|7600|7700|7800x3d|8600g|8700g|9600x|9700x|9800x3d|9900x|9950x|am5)\\b.*");
        boolean mentionsAmdMain = m.matches(".*\\b(a520|b450|b550|a620|b650|x670|x870|am4|am5)\\b.*");
        boolean mentionsIntel1700Main = m.matches(".*\\b(h610|b660|b760|z690|z790|lga1700)\\b.*");
        boolean mentionsIntel1851Main = m.matches(".*\\b(b860|z890|lga1851)\\b.*");

        reply.append("### 🔍 Kết quả kiểm tra tính tương thích phần cứng từ Kỹ sư TechZone:\n\n");

        if ((mentionsIntelCpu && (mentionsAmdMain || mentionsIntel1851Main))
                || (mentionsCoreUltra && (mentionsAmdMain || mentionsIntel1700Main))
                || (mentionsAmdAm5Cpu && (mentionsIntel1700Main || mentionsIntel1851Main))) {
            reply.append("⚠️ **KẾT LUẬN TRỰC TIẾP: KHÔNG TƯƠNG THÍCH (LỆCH CHUẨN SOCKET CPU & MAINBOARD)!**\n");
            if (mentionsIntelCpu && mentionsAmdMain) {
                reply.append("* **Nguyên nhân**: CPU Intel Gen 12/13/14 sử dụng **Socket LGA1700**, trong khi Mainboard dòng B450/B550/B650/X870 là của **AMD (Socket AM4/AM5)**. Hai hãng có cấu trúc chân tiếp xúc hoàn toàn khác nhau, tuyệt đối không thể lắp chung.\n");
                reply.append("* **Giải pháp chuẩn**: Nếu dùng CPU Intel Gen 12/13/14, bạn hãy chọn Mainboard **H610, B760 hoặc Z790**. Nếu muốn dùng Mainboard B650/X870, bạn hãy chọn CPU **AMD Ryzen 7000 / 9000 Series**.\n\n");
            } else if (mentionsAmdAm5Cpu && (mentionsIntel1700Main || mentionsIntel1851Main)) {
                reply.append("* **Nguyên nhân**: CPU AMD Ryzen 7000/9000 sử dụng **Socket AM5**, không thể lắp lên Mainboard của Intel (**B760 / Z790 / B860 / Z890**).\n");
                reply.append("* **Giải pháp chuẩn**: Với CPU AMD Socket AM5, bạn bắt buộc phải đi cùng Mainboard **B650, X670 hoặc X870** và **RAM DDR5**.\n\n");
            } else {
                reply.append("* **Nguyên nhân**: Thế hệ CPU và dòng Chipset Mainboard bạn nêu thuộc 2 chuẩn Socket vật lý khác nhau (LGA1700 vs LGA1851 vs AM5).\n\n");
            }
        } else if ((mentionsAmdAm5Cpu || mentionsCoreUltra || m.matches(".*\\b(b650|x670|x870|b860|z890|am5|lga1851)\\b.*")) && m.contains("ddr4") && !m.contains("ddr5")) {
            reply.append("⚠️ **KẾT LUẬN TRỰC TIẾP: KHÔNG TƯƠNG THÍCH (LỆCH CHUẨN BỘ NHỚ RAM DDR4 vs DDR5)!**\n");
            reply.append("* **Nguyên nhân**: Nền tảng **AMD Socket AM5 (Mainboard B650/X870)** và **Intel Core Ultra (Mainboard B860/Z890)** chỉ hỗ trợ **DUY NHẤT chuẩn RAM DDR5**, hoàn toàn không thể cắm thanh RAM DDR4 do khác vị trí rãnh khóa vật lý và kiến trúc điện áp.\n");
            reply.append("* **Giải pháp chuẩn**: Bạn cần chọn **RAM DDR5 (khuyến nghị Bus 5600MHz – 6000MHz)** cho cấu hình này.\n\n");
        }
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

    @Override
    public AiChatResponse buildComparisonAdvisory(
            String message,
            List<AiChatResponse.ProductSummary> products,
            String conversationId) {
        StringBuilder reply = new StringBuilder();
        List<AiChatResponse.QuickAction> actions = new ArrayList<>();

        List<AiChatResponse.ProductSummary> resolvedPair = new ArrayList<>();
        if (message != null) {
            String cleaned = message.toLowerCase()
                    .replaceAll("^(so sánh|đối chiếu|nên chọn|nên mua|chọn|lấy)\\s*(giữa)?\\s*", "")
                    .trim();
            String[] parts = cleaned.split("\\s+(?:và|vs|với|hay|hoặc)\\s+");
            if (parts.length >= 2) {
                AiChatResponse.ProductSummary sideA = catalogRepository.findExactAdvisedProduct(parts[0].trim());
                AiChatResponse.ProductSummary sideB = catalogRepository.findExactAdvisedProduct(parts[1].trim());
                if (sideA != null) resolvedPair.add(sideA);
                if (sideB != null && (sideA == null || !sideB.getId().equals(sideA.getId()))) resolvedPair.add(sideB);
            }
        }
        if (resolvedPair.size() < 2 && products != null) {
            for (AiChatResponse.ProductSummary p : products) {
                if (resolvedPair.stream().noneMatch(existing -> existing.getId().equals(p.getId()))) {
                    resolvedPair.add(p);
                    if (resolvedPair.size() >= 2) break;
                }
            }
        }

        if (resolvedPair.size() < 2) {
            return buildSmartProductAdvisory(message, message, products, conversationId);
        }

        AiChatResponse.ProductSummary p1 = resolvedPair.get(0);
        AiChatResponse.ProductSummary p2 = resolvedPair.get(1);
        products = resolvedPair;

        reply.append("### So sánh đối chiếu linh kiện TechZone:\n\n");
        reply.append("| Tiêu chí so sánh | **").append(p1.getName()).append("** | **").append(p2.getName()).append("** |\n");
        reply.append("| :--- | :--- | :--- |\n");
        reply.append("| **Thương hiệu** | ").append(p1.getBrand() != null ? p1.getBrand() : "Chính hãng").append(" | ").append(p2.getBrand() != null ? p2.getBrand() : "Chính hãng").append(" |\n");
        reply.append("| **Phiên bản** | ").append(p1.getVariantName() != null ? p1.getVariantName() : "Tiêu chuẩn").append(" | ").append(p2.getVariantName() != null ? p2.getVariantName() : "Tiêu chuẩn").append(" |\n");
        reply.append("| **Giá niêm yết** | **").append(p1.getPrice() != null ? MONEY_FMT.format(p1.getPrice()) : "Liên hệ").append("** | **").append(p2.getPrice() != null ? MONEY_FMT.format(p2.getPrice()) : "Liên hệ").append("** |\n");
        reply.append("| **Tình trạng kho** | ").append(p1.getStock() != null && p1.getStock() > 0 ? "Còn " + p1.getStock() + " chiếc" : "Tạm hết hàng")
                .append(" | ").append(p2.getStock() != null && p2.getStock() > 0 ? "Còn " + p2.getStock() + " chiếc" : "Tạm hết hàng").append(" |\n");

        String s1 = securityGuard.sanitizeSpecsForUser(p1.getSpecs() != null ? p1.getSpecs() : p1.getDescription());
        String s2 = securityGuard.sanitizeSpecsForUser(p2.getSpecs() != null ? p2.getSpecs() : p2.getDescription());
        if (!s1.isBlank() || !s2.isBlank()) {
            reply.append("| **Thông số kỹ thuật** | ").append(s1.isBlank() ? "Tiêu chuẩn" : s1.replace("|", " / ")).append(" | ").append(s2.isBlank() ? "Tiêu chuẩn" : s2.replace("|", " / ")).append(" |\n");
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
        String compareFacts = "Mẫu 1: " + p1.getName() + " (" + (p1.getPrice() != null ? MONEY_FMT.format(p1.getPrice()) : "") + ", " + s1 + ") vs Mẫu 2: "
                + p2.getName() + " (" + (p2.getPrice() != null ? MONEY_FMT.format(p2.getPrice()) : "") + ", " + s2 + ")";
        String aiCompareCommentary = llmClient.generateGroundedCommentary("PRODUCT_COMPARE", message, compareFacts, null);
        if (aiCompareCommentary != null && !aiCompareCommentary.isBlank()) {
            reply.append("* **Phân tích chuyên sâu từ Kỹ sư TechZone**: ").append(aiCompareCommentary).append("\n");
        }

        reply.append("\nBạn có thể bấm vào thẻ sản phẩm bên dưới để xem chi tiết từng mẫu hoặc nhấn nút để nhờ chuyên viên tư vấn chi tiết hơn!");

        actions.add(new AiChatResponse.QuickAction("🛒 Thêm vào giỏ hàng", "add_all_to_cart", "add_all_to_cart"));
        actions.add(new AiChatResponse.QuickAction("Xem " + p1.getName(), "navigate", "/san-pham/" + p1.getId()));
        actions.add(new AiChatResponse.QuickAction("Xem " + p2.getName(), "navigate", "/san-pham/" + p2.getId()));

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType("info")
                .products(products)
                .quickActions(actions)
                .build();
    }
}
