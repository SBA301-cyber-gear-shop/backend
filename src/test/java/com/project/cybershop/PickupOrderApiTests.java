package com.project.cybershop;

import com.project.cybershop.shared.config.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PickupOrderApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @Autowired com.project.cybershop.order.internal.repository.CartItemRepository cartItemRepository;
    String customer, staff, code;
    int userId;

    @BeforeEach
    void setup() {
        customer = "Bearer " + jwt.generateToken("customer@techzone.demo", "CUSTOMER");
        staff = "Bearer " + jwt.generateToken("staff@techzone.demo", "STAFF");
        userId = jdbc.queryForObject("SELECT id FROM users WHERE email='customer@techzone.demo'", Integer.class);
        jdbc.update("INSERT INTO brands VALUES (20,'Kingston')");
        jdbc.update("INSERT INTO categories VALUES (20,'RAM','ram')");
        jdbc.update("INSERT INTO products VALUES (20,20,20,'RAM','active','description',36)");
        jdbc.update("INSERT INTO product_variants (id,product_id,serial_code,variant_name,price,original_price,stock_quantity,image_url) VALUES (201,20,'TEST-RAM','16GB',1000000,1200000,5,NULL)");
        jdbc.update("INSERT INTO carts (user_id,updated_at) VALUES (?,CURRENT_TIMESTAMP)", userId);
        int cartId = jdbc.queryForObject("SELECT id FROM carts WHERE user_id=?", Integer.class, userId);
        jdbc.update("INSERT INTO cart_items (cart_id,product_variant_id,quantity) VALUES (?,201,2)", cartId);
    }

    String payload(int discount, String promo) {
        return """
                {"pickupName":"Khách nhận","pickupPhone":"0900000128","note":"Nhận tại quầy",
                 "method":"store","promotionCode":"%s","expectedTotal":2000000,"expectedDiscount":%d}
                """.formatted(promo, discount);
    }

    String place(String key, int discount, String promo) throws Exception {
        mvc.perform(post("/api/orders").header("Authorization", customer).header("Idempotency-Key", key)
                .contentType("application/json").content(payload(discount,promo)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("pending"));
        return "TZ-" + UUID.fromString(key).toString().replace("-", "").toUpperCase();
    }

    void advance(String next) throws Exception {
        mvc.perform(patch("/api/operations/orders/"+code+"/status").header("Authorization",staff)
                .contentType("application/json").content("{\"status\":\""+next+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
    }

    @Test
    @org.springframework.test.context.jdbc.Sql("/order-aftersales-test-schema.sql")
    void pcAndLooseItemsRemainGroupedThroughWarrantyIntake() throws Exception {
        int cartId = jdbc.queryForObject("SELECT id FROM carts WHERE user_id=?", Integer.class, userId);
        for (int id = 202; id <= 205; id++) {
            jdbc.update("INSERT INTO product_variants (id,product_id,serial_code,variant_name,price,original_price,stock_quantity,image_url) VALUES (?,20,?, 'test',1000000,1200000,5,NULL)", id, "TEST-" + id);
            jdbc.update("INSERT INTO cart_items (cart_id,product_variant_id,quantity) VALUES (?,?,1)", cartId, id);
        }
        String key = UUID.randomUUID().toString();
        String request = """
                {"pickupName":"Khách nhận","pickupPhone":"0900000128","method":"store",
                 "expectedTotal":6000000,"expectedDiscount":0,
                 "pcGroups":[{"parts":[{"variantId":201,"quantity":2,"role":"RAM"},
                                           {"variantId":202,"quantity":1,"role":"CPU"}]}]}
                """;
        mvc.perform(post("/api/orders").header("Authorization", customer).header("Idempotency-Key", key)
                .contentType("application/json").content(request)).andExpect(status().isCreated());
        code = "TZ-" + UUID.fromString(key).toString().replace("-", "").toUpperCase();
        Integer pcId = jdbc.queryForObject("SELECT pc_custom_id FROM order_items WHERE product_variant_id=201", Integer.class);
        assertNotNull(pcId);
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM order_items WHERE pc_custom_id=?", Integer.class, pcId));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM order_items WHERE pc_custom_id IS NULL", Integer.class));
        advance("confirmed"); advance("preparing"); advance("ready_for_pickup");
        mvc.perform(post("/api/operations/orders/" + code + "/collect").header("Authorization", staff)
                .contentType("application/json").content("{\"receivedAmount\":6000000}"))
                .andExpect(status().isOk());
        var items = jdbc.queryForList("SELECT id,quantity FROM order_items ORDER BY id");
        var devices = new java.util.ArrayList<String>();
        for (var item : items) {
            int id = ((Number) item.get("id")).intValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            for (int i = 0; i < quantity; i++) devices.add("{\"orderItemId\":" + id + ",\"serialNumber\":\"PC-" + id + "-" + i + "\"}");
        }
        mvc.perform(post("/api/operations/orders/" + code + "/handover").header("Authorization", staff)
                .contentType("application/json").content("{\"recipientPhone\":\"0900000128\",\"recipientVerified\":true,\"devices\":[" + String.join(",", devices) + "]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/warranty-cards").param("q", code).header("Authorization", staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[?(@.pcCustomId == %d)]".formatted(pcId), org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.data[?(@.pcCustomId == null)]", org.hamcrest.Matchers.hasSize(3)));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM warranty_cards wc JOIN order_items oi ON oi.id=wc.order_item_id WHERE oi.pc_custom_id=?", Integer.class, pcId));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM warranty_cards wc JOIN order_items oi ON oi.id=wc.order_item_id WHERE oi.pc_custom_id IS NULL", Integer.class));
    }

    @Test
    void creationSnapshotsReservesOnceAndRetriesReturnTheSameOrder() throws Exception {
        var key=UUID.randomUUID().toString();
        code=place(key,0,"");
        place(key,0,"");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM orders",Integer.class));
        assertEquals(3,jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=201",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM cart_items",Integer.class));
        jdbc.update("UPDATE product_variants SET price=1 WHERE id=201");
        mvc.perform(get("/api/orders/"+code).header("Authorization",customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].unitPrice").value(1000000))
                .andExpect(jsonPath("$.items[0].productName").value("RAM · 16GB"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
    }

    @Test
    void placementUsesTheCurrentCartQuantityAfterAnEarlierEntityRead() throws Exception {
        Integer itemId = jdbc.queryForObject("SELECT id FROM cart_items", Integer.class);
        assertEquals(2, cartItemRepository.findById(itemId).orElseThrow().getQuantity());
        jdbc.update("UPDATE cart_items SET quantity=3 WHERE id=?", itemId);

        String request = payload(0, "").replace("\"expectedTotal\":2000000", "\"expectedTotal\":3000000");
        mvc.perform(post("/api/orders").header("Authorization", customer)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json").content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].subtotal").value(3000000))
                .andExpect(jsonPath("$.finalAmount").value(3000000));

        assertEquals(2, jdbc.queryForObject(
                "SELECT stock_quantity FROM product_variants WHERE id=201", Integer.class
        ));
    }

    @Test
    void statusChangesRecheckTheDatabaseAfterAnEarlierOrderRead() throws Exception {
        code = place(UUID.randomUUID().toString(), 0, "");
        jdbc.update("UPDATE orders SET status='ready_for_pickup' WHERE order_code=?", code);

        mvc.perform(patch("/api/operations/orders/" + code + "/status")
                        .header("Authorization", staff)
                        .contentType("application/json").content("{\"status\":\"confirmed\"}"))
                .andExpect(status().isBadRequest());

        assertEquals("ready_for_pickup", jdbc.queryForObject(
                "SELECT status FROM orders WHERE order_code=?", String.class, code
        ));
    }

    @Test
    void stalePriceAndUnsupportedPaymentDoNotCreateOrders() throws Exception {
        jdbc.update("UPDATE product_variants SET price=1100000 WHERE id=201");
        mvc.perform(post("/api/orders").header("Authorization",customer).header("Idempotency-Key",UUID.randomUUID())
                .contentType("application/json").content(payload(0,""))).andExpect(status().isBadRequest());
        jdbc.update("UPDATE product_variants SET price=1000000 WHERE id=201");
        mvc.perform(post("/api/orders").header("Authorization",customer).header("Idempotency-Key",UUID.randomUUID())
                .contentType("application/json").content(payload(0,"").replace("\"store\"","\"online\""))).andExpect(status().isBadRequest());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM orders",Integer.class));
        assertEquals(5,jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=201",Integer.class));
    }

    @Test
    void counterPurchaseCompletesWithTwoIndependentWarrantyCardsAndOneReview() throws Exception {
        code=place(UUID.randomUUID().toString(),0,"");
        mvc.perform(patch("/api/operations/orders/"+code+"/status").header("Authorization",staff)
                .contentType("application/json").content("{\"status\":\"ready_for_pickup\"}"))
                .andExpect(status().isBadRequest());
        advance("confirmed"); advance("preparing"); advance("ready_for_pickup");
        mvc.perform(get("/api/orders/"+code).header("Authorization",customer))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        int itemId=jdbc.queryForObject("SELECT id FROM order_items",Integer.class);
        String handover="""
                {"recipientPhone":"0900000128","recipientVerified":true,"devices":[
                 {"orderItemId":%d,"serialNumber":"serial-a"},{"orderItemId":%d,"serialNumber":"SERIAL-B"}]}
                """.formatted(itemId,itemId);
        mvc.perform(post("/api/operations/orders/"+code+"/handover").header("Authorization",staff)
                .contentType("application/json").content(handover)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/operations/orders/"+code+"/collect").header("Authorization",staff)
                .contentType("application/json").content("{\"receivedAmount\":1000000}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/operations/orders/"+code+"/collect").header("Authorization",staff)
                .contentType("application/json").content("{\"receivedAmount\":2000000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
        mvc.perform(post("/api/operations/orders/"+code+"/handover").header("Authorization",staff)
                .contentType("application/json").content(handover.replace("0900000128","0900000999")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/operations/orders/"+code+"/handover").header("Authorization",staff)
                .contentType("application/json").content(handover.replace("SERIAL-B","serial-a")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/operations/orders/"+code+"/handover").header("Authorization",staff)
                .contentType("application/json").content(handover))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("completed"));
        mvc.perform(post("/api/operations/orders/"+code+"/handover").header("Authorization",staff)
                .contentType("application/json").content(handover)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/me/warranties").header("Authorization",customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM warranty_cards",Integer.class));
        assertEquals(3,jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=201",Integer.class));
        for (int i=0;i<2;i++) mvc.perform(put("/api/order-items/"+itemId+"/review").header("Authorization",customer)
                .contentType("application/json").content("{\"rating\":5,\"comment\":\"Sản phẩm tốt\"}"))
                .andExpect(i == 0 ? status().isNoContent() : status().isBadRequest());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM reviews",Integer.class));
        mvc.perform(put("/api/order-items/"+itemId+"/review").header("Authorization",customer)
                .contentType("application/json").content("{\"rating\":4,\"comment\":\"Đã cập nhật nhận xét\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/201/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value("201"))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].comment").value("Sản phẩm tốt"))
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    void counterCollectionRecordsTheAuthenticatedStaffAndChange() throws Exception {
        code = place(UUID.randomUUID().toString(), 0, "");
        String base = "/api/operations/orders/" + code;
        mvc.perform(get(base + "/cash-collection").header("Authorization", staff))
                .andExpect(status().isNoContent());
        mvc.perform(post(base + "/collect").header("Authorization", staff).contentType("application/json")
                .content("{\"receivedAmount\":2100000,\"reference\":\"CT-01\"}"))
                .andExpect(status().isOk());
        int staffId = jdbc.queryForObject("SELECT id FROM users WHERE email='staff@techzone.demo'", Integer.class);
        mvc.perform(get(base + "/cash-collection").header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectorUserId").value(staffId))
                .andExpect(jsonPath("$.collectedAt").isNotEmpty())
                .andExpect(jsonPath("$.receivedAmount").value(2100000))
                .andExpect(jsonPath("$.changeAmount").value(100000))
                .andExpect(jsonPath("$.reference").value("CT-01"));
        mvc.perform(get(base + "/cash-collection").header("Authorization", customer))
                .andExpect(status().isForbidden());
    }

    @Test
    void shippingCodCannotBeCollectedAtTheCounter() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = """
                {"pickupName":"Khách nhận","pickupPhone":"0900000128","method":"cod",
                 "deliveryType":"shipping","shippingFee":30000,"shippingAddress":"Địa chỉ test",
                 "expectedTotal":2000000,"expectedDiscount":0}
                """;
        mvc.perform(post("/api/orders").header("Authorization", customer).header("Idempotency-Key", key)
                .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        code = "TZ-" + key.replace("-", "").toUpperCase();
        String base = "/api/operations/orders/" + code;
        mvc.perform(post(base + "/collect").header("Authorization", staff).contentType("application/json")
                .content("{\"receivedAmount\":2030000}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(base + "/cash-collection").header("Authorization", staff))
                .andExpect(status().isNoContent());
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM payments", String.class));
    }

    @Test
    void customerCannotOperateOrReviewBeforeHandoverAndCannotReadOthersOrders() throws Exception {
        code=place(UUID.randomUUID().toString(),0,"");
        int itemId=jdbc.queryForObject("SELECT id FROM order_items",Integer.class);
        mvc.perform(get("/api/operations/orders").header("Authorization",customer)).andExpect(status().isForbidden());
        mvc.perform(post("/api/operations/orders/"+code+"/collect").header("Authorization",customer)
                .contentType("application/json").content("{\"receivedAmount\":2000000}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/"+code).header("Authorization",staff)).andExpect(status().isNotFound());
        mvc.perform(put("/api/order-items/"+itemId+"/review").header("Authorization",customer)
                .contentType("application/json").content("{\"rating\":5,\"comment\":\"Test\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/order-items/"+itemId+"/review").header("Authorization",staff)
                .contentType("application/json").content("{\"rating\":5,\"comment\":\"Test\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancellationRestoresStockAndVoucherQuotaOnlyOnce() throws Exception {
        jdbc.update("INSERT INTO promotions (id,code,name,discount_type,discount_value,usage_limit,is_active,start_date,end_date) VALUES (20,'SAVE','Test','fixed_amount',100000,1,TRUE,CURRENT_TIMESTAMP-INTERVAL '1' DAY,CURRENT_TIMESTAMP+INTERVAL '1' DAY)");
        code=place(UUID.randomUUID().toString(),100000,"SAVE");
        assertEquals("RESERVED",jdbc.queryForObject("SELECT status FROM promotion_usages",String.class));
        for (int i=0;i<2;i++) mvc.perform(post("/api/orders/"+code+"/cancel").header("Authorization",customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("canceled"))
                .andExpect(jsonPath("$.cancelReason").value("Khách hàng yêu cầu hủy đơn"));
        assertEquals(5,jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=201",Integer.class));
        assertEquals("REVOKED",jdbc.queryForObject("SELECT status FROM promotion_usages",String.class));
        assertEquals("CANCELED",jdbc.queryForObject("SELECT status FROM payments",String.class));
        assertEquals("Khách hàng yêu cầu hủy đơn", jdbc.queryForObject("SELECT cancel_reason FROM orders", String.class));
        mvc.perform(post("/api/operations/orders/"+code+"/cancel").header("Authorization",staff)
                        .contentType("application/json").content("{\"reason\":\"Đổi lý do sau khi hủy\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cancelReason").value("Khách hàng yêu cầu hủy đơn"));
    }

    @Test
    void staffCancellationRequiresAReasonAndKeepsPickupNotesIntact() throws Exception {
        code = place(UUID.randomUUID().toString(), 0, "");
        {
            String base = "/api/operations/orders/";
            String actor = staff;
            for (String body : new String[]{"{}", "{\"reason\":null}", "{\"reason\":\"   \"}",
                    "{\"reason\":\"" + "a".repeat(501) + "\"}"}) {
                mvc.perform(post(base + code + "/cancel").header("Authorization", actor)
                                .contentType("application/json").content(body))
                        .andExpect(status().isBadRequest());
            }
            mvc.perform(post(base + code + "/cancel").header("Authorization", actor))
                    .andExpect(status().isBadRequest());
        }
        assertEquals("pending", jdbc.queryForObject("SELECT status FROM orders", String.class));
        assertEquals(3, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=201", Integer.class));
        mvc.perform(post("/api/operations/orders/" + code + "/cancel").header("Authorization", staff)
                        .contentType("application/json").content("{\"reason\":\"Khách yêu cầu đổi cấu hình\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cancelReason").value("Khách yêu cầu đổi cấu hình"))
                .andExpect(jsonPath("$.note").value("Nhận tại quầy"));
        mvc.perform(get("/api/orders/" + code).header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cancelReason").value("Khách yêu cầu đổi cấu hình"));
        assertEquals(5, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=201", Integer.class));
    }

    @Test
    void handoverAcceptsEquivalentVietnamesePhoneFormatsButRejectsInvalidCharacters() throws Exception {
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/api/orders").header("Authorization", customer).header("Idempotency-Key", key)
                        .contentType("application/json").content(payload(0, "").replace("0900000128", "+84900000128")))
                .andExpect(status().isCreated());
        code = "TZ-" + UUID.fromString(key).toString().replace("-", "").toUpperCase();
        advance("confirmed"); advance("preparing"); advance("ready_for_pickup");
        mvc.perform(post("/api/operations/orders/" + code + "/collect").header("Authorization", staff)
                        .contentType("application/json").content("{\"receivedAmount\":2000000}"))
                .andExpect(status().isOk());
        int itemId = jdbc.queryForObject("SELECT id FROM order_items", Integer.class);
        String handover = """
                {"recipientPhone":"%s","recipientVerified":true,"devices":[
                 {"orderItemId":%d,"serialNumber":"PHONE-A"},{"orderItemId":%d,"serialNumber":"PHONE-B"}]}
                """;
        for (String invalid : new String[]{"abc0900000128", "0900000999", "++84900000128"}) {
            mvc.perform(post("/api/operations/orders/" + code + "/handover").header("Authorization", staff)
                            .contentType("application/json").content(handover.formatted(invalid, itemId, itemId)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/operations/orders/" + code + "/handover").header("Authorization", staff)
                        .contentType("application/json").content(handover.formatted("0900 000 128", itemId, itemId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("completed"));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM warranty_cards", Integer.class));
    }
}
