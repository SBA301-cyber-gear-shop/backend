package com.project.cybershop.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.AiChatResponse;
import com.project.cybershop.support.internal.service.CatalogVectorSyncService;
import com.project.cybershop.support.internal.service.CustomerAiSupportService;
import com.project.cybershop.support.internal.service.GeminiEmbeddingService;
import com.project.cybershop.support.internal.service.StaffLiveSupportService;
import com.project.cybershop.support.internal.service.impl.CustomerAiSupportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerAiSupportServiceTest {

    private JdbcTemplate jdbcTemplate;
    private GeminiEmbeddingService embeddingService;
    private ObjectMapper objectMapper;
    private StaffLiveSupportService liveSupportService;
    private CatalogVectorSyncService catalogVectorSyncService;
    private CustomerAiSupportServiceImpl supportService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        embeddingService = mock(GeminiEmbeddingService.class);
        objectMapper = new ObjectMapper();
        liveSupportService = mock(StaffLiveSupportService.class);
        catalogVectorSyncService = mock(CatalogVectorSyncService.class);

        supportService = new CustomerAiSupportServiceImpl(jdbcTemplate, embeddingService, objectMapper, liveSupportService, catalogVectorSyncService);
        ReflectionTestUtils.setField(supportService, "apiKey", ""); // test fallback / rule-based logic
    }

    @Test
    @DisplayName("Guest hỏi về chính sách giao hàng GHN -> Trả lời chi tiết và đề xuất đăng nhập nếu muốn tra cứu đơn")
    void testGuestQueryGhnShipping() {
        when(embeddingService.embedText(anyString())).thenReturn("[0.1, 0.2]");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(List.of("### Chính sách vận chuyển GHN\nThời gian giao hàng 24h nội thành, 1-3 ngày toàn quốc"));

        AiChatRequest req = new AiChatRequest();
        req.setMessage("Chính sách giao hàng GHN thế nào?");

        AiChatResponse response = supportService.generateResponse(null, req);

        assertNotNull(response);
        assertEquals("info", response.getMessageType());
        assertTrue(response.getMessage().contains("GHN") || response.getMessage().contains("Giao Hàng Nhanh"));
        assertNotNull(response.getQuickActions());
    }

    @Test
    @DisplayName("Khách hàng đã đăng nhập hỏi về đơn hàng VGA -> Trả về chi tiết đơn, mã GHN và hỏi điều hướng")
    void testCustomerQueryVgaOrder() {
        String email = "customer@techzone.demo";

        // Mock user lookup
        when(jdbcTemplate.queryForList(contains("SELECT id, full_name, phone FROM users"), eq(email)))
                .thenReturn(List.of(Map.of("id", 4, "full_name", "Nguyễn Văn Khách", "phone", "0987654321")));

        // Mock recent order lookup
        when(jdbcTemplate.query(contains("FROM orders o"), any(RowMapper.class), eq(4)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getInt("id")).thenReturn(101);
                    when(rs.getString("order_code")).thenReturn("TZ-VGA999");
                    when(rs.getString("status")).thenReturn("delivering");
                    when(rs.getString("delivery_type")).thenReturn("GHN");
                    when(rs.getString("tracking_code")).thenReturn("GHN888999");
                    when(rs.getDouble("final_amount")).thenReturn(15500000.0);
                    when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
                    when(rs.getString("items")).thenReturn("VGA ASUS RTX 4070 Super 12GB (x1)");
                    return List.of(mapper.mapRow(rs, 0));
                });

        // Mock warranties lookup
        when(jdbcTemplate.query(contains("FROM warranty_cards wc"), any(RowMapper.class), eq(4)))
                .thenReturn(List.of());

        AiChatRequest req = new AiChatRequest();
        req.setMessage("Đơn hàng này VGA của tôi sao rồi?");

        AiChatResponse response = supportService.generateResponse(email, req);

        assertNotNull(response);
        assertEquals("proposal", response.getMessageType());
        assertEquals("/don-hang/TZ-VGA999", response.getMessageRoute());
        assertTrue(response.getMessage().contains("TZ-VGA999"));
        assertTrue(response.getMessage().contains("VGA ASUS RTX 4070 Super"));
        assertTrue(response.getMessage().contains("GHN888999"));
        assertTrue(response.getMessage().contains("Bạn có muốn"));

        // Verify quick actions contain direct navigation
        boolean hasNavAction = response.getQuickActions().stream()
                .anyMatch(a -> "/don-hang/TZ-VGA999".equals(a.getRoute()));
        assertTrue(hasNavAction);
    }

    @Test
    @DisplayName("Khách hàng đồng ý điều hướng ('Có, đưa tôi tới đó') -> Trả về messageType = 'navigate' và messageRoute")
    void testCustomerAffirmativeNavigation() {
        String email = "customer@techzone.demo";

        when(jdbcTemplate.queryForList(contains("SELECT id, full_name, phone FROM users"), eq(email)))
                .thenReturn(List.of(Map.of("id", 4, "full_name", "Nguyễn Văn Khách", "phone", "0987654321")));

        when(jdbcTemplate.query(contains("FROM orders o"), any(RowMapper.class), eq(4)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getInt("id")).thenReturn(101);
                    when(rs.getString("order_code")).thenReturn("TZ-VGA999");
                    when(rs.getString("status")).thenReturn("delivering");
                    when(rs.getString("delivery_type")).thenReturn("GHN");
                    when(rs.getString("tracking_code")).thenReturn("GHN888999");
                    when(rs.getDouble("final_amount")).thenReturn(15500000.0);
                    when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
                    when(rs.getString("items")).thenReturn("VGA ASUS RTX 4070 Super 12GB (x1)");
                    return List.of(mapper.mapRow(rs, 0));
                });

        AiChatRequest req = new AiChatRequest();
        req.setMessage("Có, đưa tôi tới đó");
        req.setHistory(List.of(
                new AiChatRequest.HistoryItem("user", "Đơn hàng của tôi thế nào?"),
                new AiChatRequest.HistoryItem("bot", "Đơn hàng của bạn đang giao. Bạn có muốn mình đưa bạn tới trang chi tiết đơn hàng không?")
        ));

        AiChatResponse response = supportService.generateResponse(email, req);

        assertNotNull(response);
        assertEquals("navigate", response.getMessageType());
        assertEquals("/don-hang/TZ-VGA999", response.getMessageRoute());
        assertTrue(response.getMessage().contains("Đang đưa bạn đến trang chi tiết đơn hàng"));
    }

    @Test
    @DisplayName("Khách hỏi 'shop có ram 8gb 4800 không' -> Không bị nhầm lẫn thành điều hướng đơn hàng")
    void testShopCoRam() {
        String email = "customer@techzone.demo";
        when(jdbcTemplate.queryForList(contains("SELECT id, full_name, phone FROM users"), eq(email)))
                .thenReturn(List.of(Map.of("id", 4, "full_name", "Nguyễn Văn Khách", "phone", "0987654321")));
        when(jdbcTemplate.query(contains("FROM orders o"), any(RowMapper.class), eq(4)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getInt("id")).thenReturn(101);
                    when(rs.getString("order_code")).thenReturn("TZ-D580F54F8DDD4F4BAE1D24A471E966DD");
                    when(rs.getString("status")).thenReturn("delivering");
                    when(rs.getString("delivery_type")).thenReturn("GHN");
                    when(rs.getString("tracking_code")).thenReturn("GHN888999");
                    when(rs.getDouble("final_amount")).thenReturn(10990000.0);
                    when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
                    when(rs.getString("items")).thenReturn("RAM Corsair 16GB");
                    return List.of(mapper.mapRow(rs, 0));
                });

        AiChatRequest req = new AiChatRequest();
        req.setMessage("shop có ram 8gb 4800 không");
        AiChatResponse response = supportService.generateResponse(email, req);
        assertNotEquals("navigate", response.getMessageType());
        assertFalse(response.getMessage().contains("TZ-D580F54F8DDD4F4BAE1D24A471E966DD"));
    }

    @Test
    @DisplayName("Khách hàng hỏi bảo hành -> Trả về danh sách thẻ bảo hành và serial")
    void testCustomerQueryWarranty() {
        String email = "customer@techzone.demo";

        when(jdbcTemplate.queryForList(contains("SELECT id, full_name, phone FROM users"), eq(email)))
                .thenReturn(List.of(Map.of("id", 4, "full_name", "Nguyễn Văn Khách", "phone", "0987654321")));

        when(jdbcTemplate.query(contains("FROM orders o"), any(RowMapper.class), eq(4)))
                .thenReturn(List.of());

        when(jdbcTemplate.query(contains("FROM warranty_cards wc"), any(RowMapper.class), eq(4)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getInt("id")).thenReturn(501);
                    when(rs.getString("serial_number")).thenReturn("SN-RTX4070-998877");
                    when(rs.getString("product_name")).thenReturn("VGA ASUS RTX 4070 Super");
                    when(rs.getTimestamp("end_date")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusDays(365)));
                    when(rs.getString("status")).thenReturn("active");
                    return List.of(mapper.mapRow(rs, 0));
                });

        AiChatRequest req = new AiChatRequest();
        req.setMessage("Linh kiện của tôi còn bảo hành bao lâu?");

        AiChatResponse response = supportService.generateResponse(email, req);

        assertNotNull(response);
        assertEquals("proposal", response.getMessageType());
        assertEquals("/bao-hanh", response.getMessageRoute());
        assertTrue(response.getMessage().contains("SN-RTX4070-998877"));
        assertTrue(response.getMessage().contains("VGA ASUS RTX 4070 Super"));
    }

    @Test
    @DisplayName("Khách hàng hỏi 'shop có ram 8gb 4800 không' -> Không bị chuyển hướng nhầm sang đơn hàng, mà tra cứu linh kiện")
    void testCustomerQueryHardwareCatalogNotHijackedByNavigation() {
        String email = "customer@techzone.demo";

        when(jdbcTemplate.queryForList(contains("SELECT id, full_name, phone FROM users"), eq(email)))
                .thenReturn(List.of(Map.of("id", 4, "full_name", "Minh Anh", "phone", "0987654321")));

        when(jdbcTemplate.query(contains("FROM orders o"), any(RowMapper.class), eq(4)))
                .thenReturn(List.of());

        when(jdbcTemplate.query(contains("FROM warranty_cards wc"), any(RowMapper.class), eq(4)))
                .thenReturn(List.of());

        when(jdbcTemplate.queryForList(contains("FROM product_variants pv"), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of("product_name", "Kingston FURY Beast RGB 8GB DDR5 5600", "variant_name", "Tiêu chuẩn", "price", 3300000, "stock", 50, "product_id", 53)
                ));

        AiChatRequest req = new AiChatRequest();
        req.setMessage("shop có ram 8gb 4800 không");

        AiChatResponse response = supportService.generateResponse(email, req);

        assertNotNull(response);
        assertNotEquals("navigate", response.getMessageType());
        assertTrue(response.getMessage().contains("Kingston FURY Beast") || response.getMessage().contains("linh kiện"));
        assertFalse(response.getMessage().contains("Đang đưa bạn đến trang chi tiết đơn hàng"));
    }

    @Test
    @DisplayName("Hội thoại có ngữ cảnh: Bot đề xuất bảo hành, khách trả lời 'Có' -> Điều hướng chính xác đến /bao-hanh")
    void testContextualNavigationWarranty() {
        String email = "customer@techzone.demo";

        when(jdbcTemplate.queryForList(contains("SELECT id, full_name, phone FROM users"), eq(email)))
                .thenReturn(List.of(Map.of("id", 4, "full_name", "Minh Anh", "phone", "0987654321")));

        AiChatRequest req = new AiChatRequest();
        req.setMessage("Có");
        req.setHistory(List.of(
                new AiChatRequest.HistoryItem("user", "Linh kiện của tôi còn bảo hành không?"),
                new AiChatRequest.HistoryItem("bot", "Bạn có muốn mình đưa bạn tới trang Bảo hành cá nhân không?")
        ));

        AiChatResponse response = supportService.generateResponse(email, req);

        assertNotNull(response);
        assertEquals("navigate", response.getMessageType());
        assertEquals("/bao-hanh", response.getMessageRoute());
        assertTrue(response.getMessage().contains("Bảo hành"));
    }

    @Test
    @DisplayName("Hội thoại có ngữ cảnh: Bot đề xuất Build PC, khách hỏi linh kiện 'shop có ram 8gb 4800 không' -> Không bị ép điều hướng mà tra cứu linh kiện")
    void testContextualQuerySwitchesTopicSmoothly() {
        String email = "customer@techzone.demo";

        when(jdbcTemplate.queryForList(contains("SELECT id, full_name, phone FROM users"), eq(email)))
                .thenReturn(List.of(Map.of("id", 4, "full_name", "Minh Anh", "phone", "0987654321")));

        when(jdbcTemplate.query(contains("FROM orders o"), any(RowMapper.class), eq(4)))
                .thenReturn(List.of());

        when(jdbcTemplate.queryForList(contains("FROM product_variants pv"), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of("product_name", "Kingston FURY Beast RGB 8GB DDR5 5600", "variant_name", "Tiêu chuẩn", "price", 3300000, "stock", 50, "product_id", 53)
                ));

        AiChatRequest req = new AiChatRequest();
        req.setMessage("shop có ram 8gb 4800 không");
        req.setHistory(List.of(
                new AiChatRequest.HistoryItem("bot", "Bạn có muốn mở công cụ AI Build PC ngay bây giờ không?")
        ));

        AiChatResponse response = supportService.generateResponse(email, req);

        assertNotNull(response);
        assertNotEquals("navigate", response.getMessageType());
        assertTrue(response.getMessage().contains("Kingston FURY Beast") || response.getMessage().contains("linh kiện"));
    }

    @Test
    @DisplayName("Khách hỏi ngoài phạm vi (Xuất hóa đơn đỏ VAT) -> CyberBot đề xuất kết nối nhân viên hỗ trợ")
    void testOutOfScopeQueryProposesStaffHandover() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("Tôi muốn xuất hóa đơn đỏ VAT công ty");

        AiChatResponse response = supportService.generateResponse(null, req);

        assertNotNull(response);
        assertEquals("handover_proposal", response.getMessageType());
        assertTrue(response.getMessage().contains("Nhân viên tư vấn") || response.getMessage().contains("chuyên viên"));
        assertTrue(response.getQuickActions().stream().anyMatch(a -> "connect_staff".equals(a.getAction())));
    }

    @Test
    @DisplayName("Khách nhấp 'connect_staff' -> Tự động kết nối nhân viên trực tuyến thành công")
    void testConnectOnlineStaff() {
        when(jdbcTemplate.queryForList(contains("WHERE UPPER(r.name) IN ('STAFF', 'ADMIN')")))
                .thenReturn(List.of(
                        Map.of("id", 2, "full_name", "Trần Hoàng Nam", "email", "nam.th@cybershop.vn", "phone", "0988111222", "role_name", "Staff")
                ));

        AiChatRequest req = new AiChatRequest();
        req.setMessage("connect_staff");

        AiChatResponse response = supportService.generateResponse(null, req);

        assertNotNull(response);
        assertEquals("staff_connected", response.getMessageType());
        assertNotNull(response.getStaffInfo());
        assertEquals("Trần Hoàng Nam", response.getStaffInfo().getName());
        assertEquals("online", response.getStaffInfo().getStatus());
        assertTrue(response.getMessage().contains("Trần Hoàng Nam"));
    }

    @Test
    @DisplayName("Tra cứu linh kiện kho thật -> Trả về danh sách Product Cards để hiển thị UI Card")
    void testCatalogQueryReturnsStructuredProductCards() {
        when(jdbcTemplate.queryForList(contains("FROM product_variants pv"), any(Object[].class)))
                .thenReturn(List.of(
                        Map.of(
                                "product_id", 55,
                                "variant_id", 110,
                                "product_name", "Corsair Vengeance RGB",
                                "variant_name", "16GB DDR5 6000",
                                "category_name", "RAM",
                                "price", 6750000.0,
                                "original_price", 7200000.0,
                                "stock", 50,
                                "image_url", "https://img.test/ram.jpg"
                        )
                ));

        AiChatRequest req = new AiChatRequest();
        req.setMessage("shop có ram corsair 16gb ddr5 không");

        AiChatResponse response = supportService.generateResponse(null, req);

        assertNotNull(response);
        assertNotNull(response.getProducts());
        assertFalse(response.getProducts().isEmpty());
        assertEquals(55, response.getProducts().get(0).getId());
        assertEquals("Corsair Vengeance RGB", response.getProducts().get(0).getName());
        assertEquals(6750000.0, response.getProducts().get(0).getPrice());
        assertEquals("/san-pham/55", response.getProducts().get(0).getRoute());
    }

    @Test
    @DisplayName("Trong chế độ Live Chat với nhân viên (mode: staff) -> Tin nhắn khách được lưu vào DB và trả về staff_sent không kèm tin nhắn máy móc")
    void testStaffModeNormalMessageNoRoboticCannedReply() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("ok");
        req.setMode("staff");
        req.setConversationId("conv-12345");

        AiChatResponse response = supportService.generateResponse(null, req);

        assertNotNull(response);
        assertEquals("staff_sent", response.getMessageType());
        assertTrue(response.getMessage() == null || response.getMessage().isBlank());
        verify(liveSupportService, times(1)).recordCustomerMessage(eq("conv-12345"), any(), any(), eq("ok"));
    }

    @Test
    @DisplayName("Trong chế độ Live Chat với nhân viên -> Khách nhắn 'Dạ cảm ơn anh, để em kiểm tra lại' không bị tự ý đóng phiên, trả về staff_sent")
    void testStaffModeCustomerSaysThanksDoesNotEndSession() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("Dạ cảm ơn anh, để em kiểm tra lại");
        req.setMode("staff");
        req.setConversationId("conv-12345");

        AiChatResponse response = supportService.generateResponse(null, req);

        assertNotNull(response);
        assertEquals("staff_sent", response.getMessageType());
        assertTrue(response.getMessage() == null || response.getMessage().isBlank());
        verify(liveSupportService, times(1)).recordCustomerMessage(eq("conv-12345"), any(), any(), eq("Dạ cảm ơn anh, để em kiểm tra lại"));
        verify(liveSupportService, never()).closeSession(anyString(), any());
    }

    @Test
    @DisplayName("Trong chế độ Live Chat với nhân viên -> Gửi end_staff_session sẽ đóng phiên và trả về staff_ended")
    void testStaffModeEndSessionClosesSession() {
        AiChatRequest req = new AiChatRequest();
        req.setMessage("end_staff_session");
        req.setMode("staff");
        req.setConversationId("conv-12345");

        AiChatResponse response = supportService.generateResponse(null, req);

        assertNotNull(response);
        assertEquals("staff_ended", response.getMessageType());
        assertTrue(response.getMessage().contains("kết thúc"));
        verify(liveSupportService, times(1)).closeSession(eq("conv-12345"), any());
    }

    @Test
    @DisplayName("Khách nói 'list những linh kiện bạn nói để tôi xem trên shop' -> Tự động dùng Vector RAG tìm linh kiện từ lịch sử và trả về Product Cards")
    void testListNhungLinhKienBanNoi() {
        when(catalogVectorSyncService.searchProductsByVector(contains("Blender"), anyInt()))
                .thenReturn(List.of(
                        AiChatResponse.ProductSummary.builder()
                                .id(10)
                                .name("ASUS Dual GeForce RTX 4060 Ti 8GB")
                                .variantName("8GB GDDR6")
                                .category("VGA")
                                .price(10990000.0)
                                .stock(12)
                                .route("/san-pham/10")
                                .build()
                ));

        AiChatRequest req = new AiChatRequest();
        req.setMessage("list những linh kiện bạn nói để tôi xem trên shop");
        req.setHistory(List.of(
                new AiChatRequest.HistoryItem("user", "nâng budget lên 25tr ưu tiên đồ họa để render blender"),
                new AiChatRequest.HistoryItem("bot", "Cấu hình tối ưu Blender 25 triệu: CPU Intel Core i5-13500, VGA RTX 4060 Ti...")
        ));

        AiChatResponse response = supportService.generateResponse(null, req);

        assertNotNull(response);
        assertEquals("info", response.getMessageType());
        assertNotNull(response.getProducts());
        assertFalse(response.getProducts().isEmpty());
        assertEquals("ASUS Dual GeForce RTX 4060 Ti 8GB", response.getProducts().get(0).getName());
        assertNotEquals("navigate", response.getMessageType());
    }
}
