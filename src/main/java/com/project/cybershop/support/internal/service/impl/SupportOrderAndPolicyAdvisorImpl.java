package com.project.cybershop.support.internal.service.impl;

import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.model.CustomerContext;
import com.project.cybershop.support.internal.model.CustomerContext.OrderSummary;
import com.project.cybershop.support.internal.model.CustomerContext.ServiceRequestSummary;
import com.project.cybershop.support.internal.model.CustomerContext.WarrantySummary;
import com.project.cybershop.support.internal.model.StorePromotionContext;
import com.project.cybershop.support.internal.model.StorePromotionContext.FlashSaleSummary;
import com.project.cybershop.support.internal.model.StorePromotionContext.PromotionSummary;
import com.project.cybershop.support.internal.repository.SupportCustomerContextRepository;
import com.project.cybershop.support.internal.service.SupportIntentResolver;
import com.project.cybershop.support.internal.service.SupportOrderAndPolicyAdvisor;
import com.project.cybershop.support.internal.service.SupportPcBuildSolver;
import com.project.cybershop.support.internal.service.SupportProductAdvisor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SupportOrderAndPolicyAdvisorImpl implements SupportOrderAndPolicyAdvisor {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###đ");

    private final SupportCustomerContextRepository customerContextRepository;
    private final SupportIntentResolver intentResolver;
    private final SupportPcBuildSolver pcBuildSolver;
    private final SupportProductAdvisor productAdvisor;

    @Override
    public AiChatResponse generateRuleBasedResponse(
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

            Matcher m = Pattern
                    .compile("\\b(TZ-[A-Za-z0-9]+|ORD-[A-Za-z0-9]+|DH[A-Za-z0-9]+)\\b", Pattern.CASE_INSENSITIVE)
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
                reply.append("* **Trạng thái**: ").append(customerContextRepository.mapStatusVietnamese(o.status())).append("\n");
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
        if (msgLower.matches(".*(tương thích|(?:lắp|cắm|gắn|ghép|chạy|đi).*(?:với|chung|vào|cùng).*(?:được không|ổn không|hợp không|sao không)|lắp được không|cắm được không|gắn được không|chạy chung|đi với nhau|hợp không|nguồn bao nhiêu watt|cần nguồn mấy watt|lga1700|am5|ddr4.*ddr5|ddr5.*ddr4).*")) {
            return productAdvisor.buildCompatibilityAdvisory(message, conversationId);
        }

        // Branch 6: So sánh sản phẩm (Comparison)
        boolean isComparison = (msgLower.contains("so sánh") || msgLower.contains(" vs ") || msgLower.matches(".*(chọn|lấy|nên mua).*?(hay|hoặc|hơn).*"));
        if (isComparison && !products.isEmpty()) {
            return productAdvisor.buildComparisonAdvisory(message, products, conversationId);
        }

        // Branch 7: Tư vấn build cấu hình PC
        if (intentResolver.isPcBuilderRequest(message)) {
            BigDecimal budget = intentResolver.extractBudgetFromMessage(message);
            return pcBuildSolver.formatSmartPcBuildAdvisory(message, budget, conversationId);
        }

        // Branch 8: Chính sách bảo hành & Tiến độ sửa chữa kỹ thuật viên
        if (msgLower.contains("bảo hành") || msgLower.contains("serial") || msgLower.contains("đổi trả") || msgLower.contains("1 đổi 1") || msgLower.contains("sửa chữa") || msgLower.contains("sửa xong chưa") || msgLower.contains("tiếp nhận")) {
            if (ctx.isMember() && ctx.activeServiceRequests() != null && !ctx.activeServiceRequests().isEmpty()) {
                reply.append("### Tiến độ phiếu tiếp nhận bảo hành & sửa chữa của bạn:\n\n");
                for (ServiceRequestSummary sr : ctx.activeServiceRequests()) {
                    reply.append("* **Mã phiếu**: `").append(sr.requestCode()).append("`\n");
                    reply.append("  - Thiết bị: **").append(sr.productName()).append("** (Serial: `").append(sr.serialNumber()).append("`)\n");
                    reply.append("  - Trạng thái kỹ thuật: **").append(sr.statusVietnamese()).append("**\n");
                    reply.append("  - Nội dung lỗi: ").append(sr.issueDescription()).append("\n");
                    reply.append("  - Dự kiến hoàn tất/trả máy: **").append(sr.expectedReturnDate()).append("**\n\n");
                }
                if (!ctx.activeWarranties().isEmpty()) {
                    reply.append("Ngoài ra, bạn có **").append(ctx.activeWarranties().size()).append(" linh kiện** khác đang có thẻ bảo hành điện tử còn hiệu lực.\n\n");
                }
                reply.append("👉 **Bạn có muốn mình đưa bạn tới trang Bảo hành để xem chi tiết lộ trình xử lý không?**");
                messageType = "proposal";
                messageRoute = "/bao-hanh";
                actions.add(new AiChatResponse.QuickAction("Xem tiến độ Bảo hành", "navigate", "/bao-hanh"));
            } else if (ctx.isMember() && !ctx.activeWarranties().isEmpty()) {
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
        if (!products.isEmpty() || intentResolver.isProductQuery(resolvedQuery)) {
            return productAdvisor.buildSmartProductAdvisory(message, resolvedQuery, products, conversationId);
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

            actions.add(new AiChatResponse.QuickAction("🛒 Xem giỏ hàng", "navigate", "/gio-hang"));
            actions.add(new AiChatResponse.QuickAction("💳 Thanh toán ngay", "navigate", "/thanh-toan"));
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

        // Branch 13: Khuyến mãi, Voucher, Mã giảm giá & Flash Sale tự động
        if (msgLower.matches(".*\\b(khuyến mãi|khuyen mai|voucher|mã giảm|ma giam|mã giảm giá|sale|flash sale|giảm giá|giam gia|ưu đãi|uu dai|discount|săn sale|san sale)\\b.*")) {
            StorePromotionContext promoCtx = customerContextRepository.fetchActivePromotionsAndFlashSales();
            reply.append("### Các chương trình khuyến mãi & ưu đãi hot nhất tại TechZone:\n\n");
            if (!promoCtx.activeVouchers().isEmpty()) {
                reply.append("🎁 **Mã Voucher giảm giá đang áp dụng:**\n");
                for (PromotionSummary p : promoCtx.activeVouchers()) {
                    String discount = "percentage".equalsIgnoreCase(p.discountType()) ? (p.discountValue() != null ? p.discountValue().intValue() + "%" : "Ưu đãi") : (p.discountValue() != null ? MONEY_FMT.format(p.discountValue().doubleValue()) : "Ưu đãi");
                    reply.append("* Mã `").append(p.code()).append("`: Giảm **").append(discount).append("** - ").append(p.name()).append(" (HSD: ").append(p.endDate()).append(")\n");
                }
                reply.append("\n");
            }
            if (!promoCtx.activeFlashSales().isEmpty()) {
                reply.append("⚡ **Sản phẩm Flash Sale giá sốc hôm nay:**\n");
                for (FlashSaleSummary fs : promoCtx.activeFlashSales()) {
                    reply.append("* **").append(fs.productName()).append("** (").append(fs.variantName()).append(")\n");
                    reply.append("  - Giá sốc: **").append(MONEY_FMT.format(fs.livePrice())).append("** (Giá gốc: ~~").append(MONEY_FMT.format(fs.originalPrice())).append("~~ - ").append(fs.discountDesc()).append(")\n");
                }
                reply.append("\n");
            }
            if (promoCtx.activeVouchers().isEmpty() && promoCtx.activeFlashSales().isEmpty()) {
                reply.append("Hiện tại TechZone đang cập nhật các đợt Flash Sale mới trong ngày. Bạn vui lòng theo dõi trang chủ để săn deal giá tốt nhất nhé!\n\n");
            }
            reply.append("👉 Bạn hãy nhập mã voucher tại bước Thanh toán giỏ hàng để được hưởng ưu đãi nhé!");
            actions.add(new AiChatResponse.QuickAction("Khám phá sản phẩm", "navigate", "/san-pham"));
            actions.add(new AiChatResponse.QuickAction("🛒 Xem giỏ hàng", "navigate", "/gio-hang"));
            return AiChatResponse.builder()
                    .conversationId(conversationId)
                    .message(reply.toString())
                    .messageType("info")
                    .messageRoute("/san-pham")
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
        actions.add(new AiChatResponse.QuickAction("Tư vấn cấu hình PC", "ask", "Tư vấn cấu hình PC 25 triệu"));
        actions.add(new AiChatResponse.QuickAction("Chính sách GHN", "ask", "Chính sách giao hàng GHN thế nào?"));

        return AiChatResponse.builder()
                .conversationId(conversationId)
                .message(reply.toString())
                .messageType(messageType)
                .messageRoute(messageRoute)
                .quickActions(actions)
                .build();
    }
}
