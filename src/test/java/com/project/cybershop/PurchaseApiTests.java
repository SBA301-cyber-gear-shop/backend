package com.project.cybershop;

import com.project.cybershop.shared.config.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PurchaseApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    private String token;
    private Integer userId;

    @BeforeEach
    void seed() {
        userId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'customer@techzone.demo'", Integer.class);
        token = "Bearer " + jwt.generateToken("customer@techzone.demo", "CUSTOMER");
        jdbc.update("INSERT INTO brands VALUES (10, 'Kingston')");
        jdbc.update("INSERT INTO categories VALUES (10, 'Bộ nhớ RAM', 'ram')");
        jdbc.update("INSERT INTO products VALUES (10, 10, 10, 'Kingston RAM', 'active', 'RAM description', 36)");
        jdbc.update("INSERT INTO product_variants VALUES (101, 10, 'RAM16', '16GB', 1200000, 1300000, 3, 'ram.png')");
        jdbc.update("INSERT INTO product_variants VALUES (102, 10, 'RAM32', '32GB', 2200000, 2300000, 2, 'ram32.png')");
        jdbc.update("INSERT INTO specifications VALUES (10, 'Dung lượng')");
        jdbc.update("INSERT INTO specification_options VALUES (10, '16GB')");
        jdbc.update("INSERT INTO specification_values VALUES (10, 101, 10, 10, NULL)");
        jdbc.update("INSERT INTO category_specifications VALUES (10, 10, TRUE, TRUE)");
    }

    private void addItem(int variant, int quantity) throws Exception {
        mvc.perform(post("/api/orders/cart/items").header("Authorization", token)
                .contentType("application/json")
                .content("{\"productVariantId\":" + variant + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void batchAddToCartPersistsItemsToDatabase() throws Exception {
        mvc.perform(post("/api/orders/cart/items/batch").header("Authorization", token)
                .contentType("application/json")
                .content("""
                        {
                            "items": [
                                {"productVariantId": 101, "quantity": 1},
                                {"productVariantId": 102, "quantity": 2}
                            ]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM cart_items", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT quantity FROM cart_items WHERE product_variant_id=101", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT quantity FROM cart_items WHERE product_variant_id=102", Integer.class));
    }

    @Test
    void registrationCreatesACartOwnedByTheNewUser() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("""
                {"fullName":"Khách mới","email":"new-buyer@example.com","phone":"0987654321","password":"Example123!","address":""}
                """))
                .andExpect(status().isCreated());
        Integer newUserId = jdbc.queryForObject("SELECT id FROM users WHERE email='new-buyer@example.com'", Integer.class);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM carts WHERE user_id=?", Integer.class, newUserId));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM carts WHERE user_id IS NULL", Integer.class));
    }

    @Test
    void catalogIsPublicAndContainsVariantSpecificPriceImageAndPrimarySpecs() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("101"))
                .andExpect(jsonPath("$[0].productId").value("10"))
                .andExpect(jsonPath("$[0].price").value(1200000))
                .andExpect(jsonPath("$[0].image").value("ram.png"))
                .andExpect(jsonPath("$[0].primarySpecs[0]").value("Dung lượng"))
                .andExpect(jsonPath("$[0].specs['Dung lượng']").value("16GB"))
                .andExpect(jsonPath("$[0].averageRating").value(0))
                .andExpect(jsonPath("$[0].reviewCount").value(0))
                .andExpect(jsonPath("$[0].buyerCount").value(0))
                .andExpect(jsonPath("$[1].price").value(2200000));
    }

    @Test
    void unauthenticatedCartAndCheckoutReturn401() throws Exception {
        mvc.perform(get("/api/orders/cart")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/checkout/validate")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/checkout/details/preview").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void catalogCountsDistinctCompletedBuyersAndDoesNotMultiplyRatingsByQuantity() throws Exception {
        int secondBuyer = jdbc.queryForObject("SELECT id FROM users WHERE email='staff@techzone.demo'", Integer.class);
        int[] buyers = {userId, userId, secondBuyer, secondBuyer, userId, secondBuyer};
        String[] statuses = {"completed", "completed", "completed", "pending", "canceled", "completed"};
        int[] quantities = {3, 1, 2, 100, 10, 1};
        for (int i = 0; i < buyers.length; i++) {
            jdbc.update("INSERT INTO orders (id,user_id,order_code,total_amount,final_amount,status) VALUES (?,?,?,1000000,1000000,?)",
                    601 + i, buyers[i], "STATS-" + i, statuses[i]);
            jdbc.update("INSERT INTO order_items (id,order_id,product_variant_id,warranty_period_months,quantity,unit_price,subtotal) VALUES (?,?,?,36,?,1000000,1000000)",
                    611 + i, 601 + i, i == 5 ? 102 : 101, quantities[i]);
        }
        jdbc.update("INSERT INTO reviews (order_item_id,rating,comment) VALUES (611,5,'Tốt'),(612,4,'Khá tốt'),(614,1,'Đơn chưa nhận'),(615,1,'Đơn hủy')");
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].averageRating").value(4.5))
                .andExpect(jsonPath("$[0].reviewCount").value(2))
                .andExpect(jsonPath("$[0].buyerCount").value(2))
                .andExpect(jsonPath("$[1].averageRating").value(0))
                .andExpect(jsonPath("$[1].reviewCount").value(0))
                .andExpect(jsonPath("$[1].buyerCount").value(1));

        mvc.perform(put("/api/order-items/611/review").header("Authorization", token)
                .contentType("application/json").content("{\"rating\":3,\"comment\":\"Đã sửa nhận xét\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].averageRating").value(3.5))
                .andExpect(jsonPath("$[0].reviewCount").value(2))
                .andExpect(jsonPath("$[0].buyerCount").value(2));
    }

    @Test
    void cartMergesQuantityUsesCartItemIdAndNeverReservesStock() throws Exception {
        addItem(101, 1);
        addItem(101, 1);
        mvc.perform(get("/api/orders/cart").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(2400000));
        Integer itemId = jdbc.queryForObject("SELECT id FROM cart_items", Integer.class);
        mvc.perform(put("/api/orders/cart/items/" + itemId).header("Authorization", token)
                .contentType("application/json").content("{\"quantity\":3}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/orders/cart/items/" + itemId).header("Authorization", token)
                .contentType("application/json").content("{\"quantity\":4}"))
                .andExpect(status().isBadRequest());
        assertEquals(3, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=101", Integer.class));
        mvc.perform(delete("/api/orders/cart/items/" + itemId).header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/orders/cart").header("Authorization", token))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void cartReadsCurrentDataFromTheExistingProductAndVariant() throws Exception {
        addItem(101, 2);
        jdbc.update("UPDATE products SET name='Kingston RAM mới', status='ACTIVE' WHERE id=10");
        jdbc.update("UPDATE product_variants SET variant_name='16GB mới', price=1000000, image_url='new-ram.png', stock_quantity=2 WHERE id=101");

        mvc.perform(get("/api/orders/cart").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productVariantId").value(101))
                .andExpect(jsonPath("$.data.items[0].productName").value("Kingston RAM mới"))
                .andExpect(jsonPath("$.data.items[0].variantName").value("16GB mới"))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("new-ram.png"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(1000000))
                .andExpect(jsonPath("$.data.items[0].stockQuantity").value(2))
                .andExpect(jsonPath("$.data.items[0].available").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(2000000));
    }

    @Test
    void cartRejectsMissingInactiveAndOutOfStockVariants() throws Exception {
        mvc.perform(post("/api/orders/cart/items").header("Authorization", token)
                .contentType("application/json").content("{\"productVariantId\":999,\"quantity\":1}"))
                .andExpect(status().isNotFound());

        for (String productStatus : new String[]{"DRAFT", "ARCHIVED"}) {
            jdbc.update("UPDATE products SET status=? WHERE id=10", productStatus);
            mvc.perform(post("/api/orders/cart/items").header("Authorization", token)
                    .contentType("application/json").content("{\"productVariantId\":101,\"quantity\":1}"))
                    .andExpect(status().isNotFound());
        }

        jdbc.update("UPDATE products SET status='ACTIVE' WHERE id=10");
        jdbc.update("UPDATE product_variants SET stock_quantity=0 WHERE id=101");
        mvc.perform(post("/api/orders/cart/items").header("Authorization", token)
                .contentType("application/json").content("{\"productVariantId\":101,\"quantity\":1}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM cart_items", Integer.class));
    }

    @Test
    void anotherCustomerCannotEditOrDeleteAnItem() throws Exception {
        addItem(101, 1);
        Integer itemId = jdbc.queryForObject("SELECT id FROM cart_items", Integer.class);
        String otherToken = "Bearer " + jwt.generateToken("staff@techzone.demo", "STAFF");
        mvc.perform(put("/api/orders/cart/items/" + itemId).header("Authorization", otherToken)
                .contentType("application/json").content("{\"quantity\":2}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/orders/cart/items/" + itemId).header("Authorization", otherToken))
                .andExpect(status().isNotFound());
        assertEquals(1, jdbc.queryForObject("SELECT quantity FROM cart_items WHERE id=?", Integer.class, itemId));
    }

    @Test
    void checkoutRejectsWholeCartWhenStockChangesAndStillAllowsRemovingInactiveProducts() throws Exception {
        addItem(101, 2);
        addItem(102, 1);
        jdbc.update("UPDATE product_variants SET stock_quantity=1 WHERE id=101");
        mvc.perform(post("/api/checkout/validate").header("Authorization", token))
                .andExpect(status().isBadRequest());
        jdbc.update("UPDATE products SET status='inactive' WHERE id=10");
        mvc.perform(get("/api/orders/cart").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].available").value(false));
        Integer itemId = jdbc.queryForObject("SELECT id FROM cart_items WHERE product_variant_id=101", Integer.class);
        mvc.perform(put("/api/orders/cart/items/" + itemId).header("Authorization", token)
                .contentType("application/json").content("{\"quantity\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/orders/cart/items/" + itemId).header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/orders/cart").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productVariantId").value(102));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM cart_items WHERE id=?", Integer.class, itemId));
    }

    @Test
    void previewUsesCurrentPricesCapsVoucherAndDoesNotConsumeIt() throws Exception {
        addItem(101, 2);
        jdbc.update("UPDATE product_variants SET price=1000000 WHERE id=101");
        jdbc.update("INSERT INTO promotions VALUES (10,'SAVE','Voucher','fixed',3000000,1,TRUE,CURRENT_TIMESTAMP - INTERVAL '1' DAY,CURRENT_TIMESTAMP + INTERVAL '1' DAY)");
        mvc.perform(post("/api/checkout/details/preview").header("Authorization", token)
                .contentType("application/json").content("""
                        {"pickupName":"Người nhận","pickupPhone":"0900000128","note":"Nhận buổi chiều","promotionCode":"save"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(2000000))
                .andExpect(jsonPath("$.discountAmount").value(2000000))
                .andExpect(jsonPath("$.finalAmount").value(0));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM promotion_usages", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=101", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM cart_items", Integer.class));
    }

    @Test
    void reservedVoucherUsesQuotaAndBlocksReuseBySameCustomer() throws Exception {
        addItem(101, 1);
        jdbc.update("INSERT INTO promotions VALUES (10,'SAVE','Voucher','percent',10,1,TRUE,CURRENT_TIMESTAMP - INTERVAL '1' DAY,CURRENT_TIMESTAMP + INTERVAL '1' DAY)");
        jdbc.update("INSERT INTO orders (id,user_id,order_code,total_amount,final_amount,status) VALUES (99,?,'RESERVED-ORDER',1200000,1080000,'pending')", userId);
        jdbc.update("INSERT INTO promotion_usages VALUES (10,10,?,99,'RESERVED',120000)", userId);
        mvc.perform(post("/api/checkout/details/preview").header("Authorization", token)
                .contentType("application/json").content("""
                        {"pickupName":"Khách","pickupPhone":"0900000128","promotionCode":"SAVE"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewRechecksPromotionTermsInsteadOfReusingAnEarlierRead() throws Exception {
        addItem(101, 1);
        jdbc.update("INSERT INTO promotions VALUES (10,'SAVE','Voucher','fixed_amount',100000,5,TRUE,CURRENT_TIMESTAMP - INTERVAL '1' DAY,CURRENT_TIMESTAMP + INTERVAL '1' DAY)");
        String details = """
                {"pickupName":"Khách","pickupPhone":"0900000128","promotionCode":"SAVE"}
                """;
        mvc.perform(post("/api/checkout/details/preview").header("Authorization", token)
                .contentType("application/json").content(details))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(100000));
        jdbc.update("UPDATE promotions SET discount_value=200000 WHERE id=10");
        mvc.perform(post("/api/checkout/details/preview").header("Authorization", token)
                .contentType("application/json").content(details))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(200000));
        jdbc.update("UPDATE promotions SET is_active=FALSE WHERE id=10");
        mvc.perform(post("/api/checkout/details/preview").header("Authorization", token)
                .contentType("application/json").content(details))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM promotion_usages", Integer.class));
    }

    @Test
    void partialCheckoutValidatesAndOnlyClearsPurchasedItems() throws Exception {
        addItem(101, 1);
        addItem(102, 1);

        // 1. Validate only variant 101
        mvc.perform(post("/api/checkout/validate").header("Authorization", token)
                .contentType("application/json")
                .content("""
                        {"itemVariantIds":[101]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productVariantId").value(101))
                .andExpect(jsonPath("$.totalAmount").value(1200000));

        // 2. Place order for only variant 101
        mvc.perform(post("/api/orders").header("Authorization", token)
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .contentType("application/json")
                .content("""
                        {
                            "pickupName":"Khách Hàng",
                            "pickupPhone":"0900000128",
                            "note":"Giao nhanh",
                            "method":"store",
                            "expectedTotal":1200000,
                            "expectedDiscount":0,
                            "itemVariantIds":[101]
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(1200000));

        // 3. Verify cart still contains variant 102
        Integer remainingCartItems = jdbc.queryForObject(
                "SELECT count(*) FROM cart_items WHERE product_variant_id = 102", Integer.class);
        assertEquals(1, remainingCartItems);

        Integer purchasedCartItems = jdbc.queryForObject(
                "SELECT count(*) FROM cart_items WHERE product_variant_id = 101", Integer.class);
        assertEquals(0, purchasedCartItems);
    }
}
