package com.project.cybershop.support.internal.knowledge;

import com.project.cybershop.support.internal.service.GeminiEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeVectorInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final GeminiEmbeddingService embeddingService;

    public record KnowledgeDoc(String category, String title, String content, String metadata) {}

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1. Chuẩn hóa Index sang HNSW để đạt Recall 100% trên tập dữ liệu nhỏ (BUG-02)
            try {
                jdbcTemplate.execute("DROP INDEX IF EXISTS idx_cyberbot_vectors");
                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_cyberbot_vectors_hnsw
                    ON cyberbot_knowledge_vectors
                    USING hnsw (embedding vector_cosine_ops)
                """);
                log.info("Khởi tạo / kiểm tra HNSW Vector Index thành công (idx_cyberbot_vectors_hnsw).");
            } catch (Exception idxEx) {
                log.warn("Không thể tạo HNSW index (có thể pgvector chưa kích hoạt hoặc bảng chưa tồn tại): {}", idxEx.getMessage());
            }

            // 2. Đếm riêng các bản ghi tri thức chính sách (không đếm PRODUCT_CATALOG để tránh Race Condition)
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cyberbot_knowledge_vectors WHERE doc_category != 'PRODUCT_CATALOG'", Integer.class);

            if (count != null && count > 0) {
                log.info("Kho tri thức văn bản chính sách CyberBot đã có {} bản ghi, bỏ qua khởi tạo.", count);
                return;
            }

            log.info("Bắt đầu nạp tài liệu chính sách & thông số kỹ thuật vào Vector DB CyberBot...");
            List<KnowledgeDoc> docs = getSeedKnowledge();

            for (KnowledgeDoc doc : docs) {
                String vectorStr = embeddingService.embedDocument(doc.title(), doc.content());
                jdbcTemplate.update(
                        "INSERT INTO cyberbot_knowledge_vectors (doc_category, doc_title, chunk_content, metadata, embedding) " +
                                "VALUES (?, ?, ?, ?::jsonb, ?::vector)",
                        doc.category(), doc.title(), doc.content(), doc.metadata(), vectorStr
                );
            }

            log.info("Nạp thành công {} tài liệu tri thức vào Vector Database CyberBot!", docs.size());
        } catch (Exception e) {
            log.warn("Không thể khởi tạo Vector DB (có thể bảng chưa sẵn sàng hoặc kết nối hạn chế): {}", e.getMessage());
        }
    }

    private List<KnowledgeDoc> getSeedKnowledge() {
        return List.of(
                new KnowledgeDoc(
                        "POLICY_WARRANTY",
                        "Chính sách bảo hành 1 đổi 1 và điều kiện bảo hành",
                        """
                        1. Chính sách 1 đổi 1: CyberShop áp dụng chính sách 1 đổi 1 trong vòng 30 ngày đầu tiên kể từ ngày mua nếu sản phẩm gặp lỗi phần cứng do nhà sản xuất.
                        2. Thời hạn bảo hành: Mỗi linh kiện có thời hạn bảo hành chính hãng riêng (CPU, VGA, Mainboard thường từ 36 tháng; RAM, SSD từ 36 - 60 tháng; Nguồn từ 36 - 120 tháng).
                        3. Điều kiện bảo hành hợp lệ: Sản phẩm phải còn nguyên tem bảo hành của CyberShop và nhà phân phối, số Serial Number (SN) trùng khớp trên hóa đơn điện tử, không bị biến dạng cơ học.
                        4. Các trường hợp từ chối bảo hành: Sản phẩm bị cháy nổ IC, chập điện do thiên tai/sét đánh, chân socket CPU bị cong/gãy, rách tem bảo hành, vào nước hoặc ẩm mốc oxi hóa, hư hỏng do tự ý ép xung quá mức.
                        5. Quy trình gửi bảo hành: Khách hàng có thể mang trực tiếp đến cửa hàng tại Quận 12 TP.HCM hoặc gửi chuyển phát bưu điện/GHN đến shop. Thời gian xử lý kiểm định kỹ thuật từ 24h đến 72h làm việc.
                        """,
                        "{\"source\": \"chinh-sach-bao-hanh\", \"type\": \"policy\"}"
                ),
                new KnowledgeDoc(
                        "POLICY_SHIPPING_GHN",
                        "Chính sách vận chuyển giao hàng qua Giao Hàng Nhanh (GHN Express)",
                        """
                        1. Đối tác vận chuyển chính thức: CyberShop hợp tác độc quyền với Giao Hàng Nhanh (GHN Express) để giao hàng toàn quốc 63 tỉnh thành.
                        2. Thời gian giao hàng dự kiến: Nội thành TP.HCM nhận hàng trong 24 giờ; Các tỉnh thành miền Nam từ 1 - 2 ngày; Miền Trung và miền Bắc từ 2 - 4 ngày làm việc.
                        3. Chính sách đồng kiểm khi nhận hàng: Toàn bộ đơn hàng CyberShop đều được cài đặt lưu ý 'CHOXEMHANGKHONGTHU' (Cho xem hàng không cho thử). Quý khách được quyền mở hộp kiểm tra ngoại quan linh kiện, số lượng, tem niêm phong trước khi thanh toán cho shipper.
                        4. Xử lý sự cố vận chuyển: Nếu bưu kiện bị móp méo, rách vỡ hộp ngoài hoặc nghi ngờ hư hỏng do vận chuyển, quý khách có quyền từ chối nhận hàng và gọi ngay hotline shop để được gửi bưu kiện mới thay thế.
                        5. Tra cứu vận đơn: Khách hàng có thể tra cứu hành trình trực tiếp tại trang chi tiết đơn hàng CyberShop hoặc trên trang theo dõi của GHN với mã bưu gửi (ví dụ: LYFEPB).
                        """,
                        "{\"source\": \"chinh-sach-van-chuyen\", \"type\": \"policy\"}"
                ),
                new KnowledgeDoc(
                        "POLICY_PAYMENT",
                        "Phương thức thanh toán online VNPay và trả tiền mặt khi nhận hàng (COD)",
                        """
                        1. Thanh toán trực tuyến VNPay: Hỗ trợ thanh toán qua cổng VNPAY với thẻ ATM nội địa (hơn 40 ngân hàng), quét mã VNPAY-QR, hoặc thẻ tín dụng quốc tế Visa/MasterCard. Đơn hàng được xác nhận thanh toán thành công tức thì.
                        2. Thanh toán khi nhận hàng (COD): Quý khách thanh toán tiền mặt trực tiếp cho nhân viên giao hàng GHN khi nhận bưu kiện tại nhà. Áp dụng cho các đơn hàng linh kiện có giá trị dưới 20 triệu đồng.
                        3. Nhận hàng tại cửa hàng: Quý khách có thể đặt online chọn 'Nhận tại cửa hàng', đến quầy kiểm tra máy và thanh toán tiền mặt hoặc chuyển khoản tại chỗ.
                        4. Chính sách hoàn tiền: Đơn hàng hủy hợp lệ đã thanh toán qua VNPay sẽ được hoàn tiền về tài khoản ngân hàng của quý khách trong vòng 3 - 5 ngày làm việc theo quy định ngân hàng.
                        """,
                        "{\"source\": \"chinh-sach-thanh-toan\", \"type\": \"policy\"}"
                ),
                new KnowledgeDoc(
                        "HARDWARE_COMPATIBILITY",
                        "Cẩm nang tương thích phần cứng máy tính và công suất nguồn khuyến nghị",
                        """
                        1. Socket CPU và Mainboard:
                           - Intel Gen 12, 13, 14: Sử dụng socket LGA1700 (Mainboard H610, B760, Z790).
                           - AMD Ryzen 3000, 5000: Sử dụng socket AM4 (Mainboard A520, B450, B550).
                           - AMD Ryzen 7000, 9000: Sử dụng socket AM5 (Mainboard A620, B650, X670) và bắt buộc dùng RAM DDR5.
                        2. Chuẩn bộ nhớ RAM:
                           - DDR4 và DDR5 có rãnh cắm khác nhau, không cắm chéo được. Luôn cắm RAM đôi (2 thanh, ví dụ 2x8GB hoặc 2x16GB) ở khe 2 và 4 để kích hoạt chế độ Dual-Channel tăng 20% băng thông.
                        3. Công suất nguồn máy tính (PSU) khuyến nghị theo Card đồ họa (VGA):
                           - RTX 3050 / GTX 1650: Nguồn tối thiểu 450W - 500W.
                           - RTX 3060 / RTX 4060: Nguồn tối thiểu 550W chuẩn 80 Plus Bronze.
                           - RTX 4060 Ti / RTX 4070 / RX 7700 XT: Nguồn tối thiểu 650W.
                           - RTX 4070 Ti Super / RTX 4080: Nguồn tối thiểu 750W - 850W chuẩn 80 Plus Gold.
                        4. Kích thước thùng máy (Case) & Tản nhiệt:
                           - Card đồ họa 3 quạt (dài trên 300mm) cần chọn vỏ case chuẩn Mid-Tower hoặc bể cá kích thước rộng rãi.
                           - Tản nhiệt nước AIO 240mm/360mm cần kiểm tra độ dày nóc case để không cấn tản nhiệt VRM của mainboard.
                        """,
                        "{\"source\": \"cam-nang-phan-cung\", \"type\": \"hardware\"}"
                )
        );
    }
}
