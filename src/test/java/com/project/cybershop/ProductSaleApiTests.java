package com.project.cybershop;

import com.project.cybershop.promotion.api.PromotionApi;
import com.project.cybershop.promotion.internal.service.ProductSaleService;
import com.project.cybershop.shared.config.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:product-sale;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @Transactional
class ProductSaleApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @Autowired ProductSaleService sales;
    @Autowired PromotionApi promotions;
    @MockitoBean Clock clock;
    String admin, customer, staff;
    int userId;

    void at(String local) {
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Bangkok"));
        when(clock.instant()).thenReturn(LocalDateTime.parse(local).atZone(ZoneId.of("Asia/Bangkok")).toInstant());
    }
    @BeforeEach void seed() {
        at("2026-11-08T10:00:00");
        admin = "Bearer " + jwt.generateToken("admin@techzone.demo", "ADMIN");
        customer = "Bearer " + jwt.generateToken("customer@techzone.demo", "CUSTOMER");
        staff = "Bearer " + jwt.generateToken("staff@techzone.demo", "STAFF");
        userId = jdbc.queryForObject("SELECT id FROM users WHERE email='customer@techzone.demo'", Integer.class);
        jdbc.update("INSERT INTO brands VALUES (200,'Test')");
        jdbc.update("INSERT INTO categories VALUES (200,'Laptop','laptop')");
        jdbc.update("INSERT INTO products VALUES (200,200,200,'Laptop sale','active','',12)");
        jdbc.update("INSERT INTO products VALUES (201,200,200,'Không sale','active','',12)");
        jdbc.update("INSERT INTO product_variants VALUES (2001,200,'LAP-A','8GB',20000,20000,20,NULL)");
        jdbc.update("INSERT INTO product_variants VALUES (2002,200,'LAP-B','16GB',40000,40000,20,NULL)");
        jdbc.update("INSERT INTO product_variants VALUES (2011,201,'OTHER','8GB',30000,30000,20,NULL)");
        jdbc.update("INSERT INTO carts (user_id) VALUES (?)", userId);
    }
    String payload(int percentage, boolean enabled, String start, String end) {
        return """
                {"enabled":%s,"percentage":%d,"start":"%s","end":"%s"}
                """.formatted(enabled, percentage, start, end);
    }
    void activeSale(int percentage) throws Exception {
        mvc.perform(put("/api/admin/product-sales/200").header("Authorization", admin).contentType("application/json")
                        .content(payload(percentage, true, "2026-11-08T00:00", "2026-11-09T00:00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }
    void addToCart() throws Exception {
        mvc.perform(post("/api/orders/cart/items").header("Authorization", customer).contentType("application/json")
                        .content("{\"productVariantId\":2001,\"quantity\":1}"))
                .andExpect(status().isOk());
    }
    int price(int variantId) { return jdbc.queryForObject("SELECT price FROM product_variants WHERE id=?", Integer.class, variantId); }

    String fixedPayload(int amount) {
        return """
                {"enabled":true,"discountType":"fixed_amount","discountValue":%d,
                 "start":"2026-11-08T00:00","end":"2026-11-09T00:00"}
                """.formatted(amount);
    }

    @Test void fixedAmountSaleAppliesPerSkuAndRestoresPricesAfterEnd() throws Exception {
        mvc.perform(put("/api/admin/product-sales/200").header("Authorization", admin).contentType("application/json").content(fixedPayload(5000)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountType").value("fixed_amount"))
                .andExpect(jsonPath("$.discountValue").value(5000)).andExpect(jsonPath("$.percentage").isEmpty());
        assertEquals(15000, price(2001)); assertEquals(35000, price(2002)); assertEquals(30000, price(2011));
        sales.refreshAll(); assertEquals(15000, price(2001));
        addToCart();
        mvc.perform(post("/api/checkout/validate").header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(15000));
        at("2026-11-09T00:00:00"); sales.refreshAll();
        assertEquals(20000, price(2001)); assertEquals(40000, price(2002));
    }

    @Test void fixedSaleCapsAtZeroAndCanSwitchToPercentageWithoutCompounding() throws Exception {
        mvc.perform(put("/api/admin/product-sales/200").header("Authorization", admin).contentType("application/json").content(fixedPayload(50000)))
                .andExpect(status().isOk());
        assertEquals(0, price(2001)); assertEquals(0, price(2002));
        activeSale(2);
        assertEquals(19600, price(2001)); assertEquals(39200, price(2002));
        mvc.perform(get("/api/admin/product-sales/200").header("Authorization", admin))
                .andExpect(jsonPath("$.discountType").value("percentage")).andExpect(jsonPath("$.discountValue").value(2));
    }

    @Test void fixedSaleRejectsInvalidAmountsTypesAndContradictoryLegacyPercentage() throws Exception {
        for (String invalid : new String[]{fixedPayload(0), fixedPayload(-1),
                fixedPayload(5000).replace("5000", "1.5"), fixedPayload(5000).replace("5000", "10000000000"),
                fixedPayload(5000).replace("fixed_amount", "invalid"),
                fixedPayload(5000).replace("\"enabled\":true", "\"percentage\":2,\"enabled\":true"),
                fixedPayload(5000).replace("\"discountValue\":5000,", "")}) {
            mvc.perform(put("/api/admin/product-sales/200").header("Authorization", admin).contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(20000, price(2001));
    }

    @Test void searchFindsByIdProductNameOrBrandAndCountsVariantsWithoutDuplicates() throws Exception {
        for (String query : new String[]{"200", "#200", "LAPTOP"}) {
            mvc.perform(get("/api/admin/product-sales/products").header("Authorization", admin).param("search", query))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].productId").value(200)).andExpect(jsonPath("$[0].brand").value("Test"))
                    .andExpect(jsonPath("$[0].variantCount").value(2));
        }
        mvc.perform(get("/api/admin/product-sales/products").header("Authorization", admin).param("search", "test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].productId").value(201)).andExpect(jsonPath("$[1].variantCount").value(1));
        mvc.perform(get("/api/admin/product-sales/products").header("Authorization", admin).param("search", "không"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].productId").value(201));
    }

    @Test void searchRejectsUnauthorizedOrLongQueriesAndTreatsWildcardInputAsLiteralText() throws Exception {
        for (String actor : new String[]{customer, staff}) {
            mvc.perform(get("/api/admin/product-sales/products").header("Authorization", actor).param("search", "Test"))
                    .andExpect(status().isForbidden());
        }
        for (String query : new String[]{"", "   ", "%", "_", "' OR 1=1--", "999999999999999999999"}) {
            mvc.perform(get("/api/admin/product-sales/products").header("Authorization", admin).param("search", query))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
        mvc.perform(get("/api/admin/product-sales/products").header("Authorization", admin).param("search", "a".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    @Test void productCodeTargetsAllItsVariantsAndLeavesOtherProductsAndOriginalPricesIntact() throws Exception {
        activeSale(2);
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].price").value(19600))
                .andExpect(jsonPath("$[0].originalPrice").value(20000))
                .andExpect(jsonPath("$[1].price").value(39200))
                .andExpect(jsonPath("$[2].price").value(30000));
        assertEquals(20000, jdbc.queryForObject("SELECT original_price FROM product_variants WHERE id=2001", Integer.class));
        mvc.perform(get("/api/admin/product-sales").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].productId").value(200))
                .andExpect(jsonPath("$[0].variants.length()").value(2));
    }

    @Test void saleStartsInclusivelyEndsExclusivelyAndNeverCompounds() throws Exception {
        mvc.perform(put("/api/admin/product-sales/200").header("Authorization", admin).contentType("application/json")
                        .content(payload(2, true, "2026-11-08T11:00", "2026-11-08T12:00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        assertEquals(20000, price(2001));
        at("2026-11-08T11:00:00");
        for (int i=0; i<3; i++) sales.refreshAll();
        assertEquals(19600, price(2001));
        assertEquals(39200, price(2002));
        at("2026-11-08T12:00:00"); sales.refreshAll();
        assertEquals(20000, price(2001)); assertEquals(40000, price(2002));
    }

    @Test void changingPercentageDisablingAndDeletingRestoreSavedRegularPrices() throws Exception {
        activeSale(2); activeSale(5);
        assertEquals(19000, price(2001));
        mvc.perform(put("/api/admin/product-sales/200").header("Authorization", admin).contentType("application/json")
                        .content(payload(5, false, "2026-11-08T00:00", "2026-11-09T00:00")))
                .andExpect(status().isOk());
        assertEquals(20000, price(2001));
        activeSale(2);
        mvc.perform(delete("/api/admin/product-sales/200").header("Authorization", admin)).andExpect(status().isNoContent());
        assertEquals(20000, price(2001));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM product_sale_prices", Integer.class));
    }

    @Test void cartCheckoutAndSavedOrderUseSalePriceWithoutAVoucherAndOrderKeepsItsSnapshot() throws Exception {
        activeSale(2); addToCart();
        mvc.perform(get("/api/orders/cart").header("Authorization", customer))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(19600));
        mvc.perform(post("/api/checkout/validate").header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(19600));
        mvc.perform(post("/api/checkout/details/preview").header("Authorization", customer).contentType("application/json")
                        .content("{\"pickupName\":\"Khách\",\"pickupPhone\":\"0900000128\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(19600))
                .andExpect(jsonPath("$.discountAmount").value(0)).andExpect(jsonPath("$.finalAmount").value(19600));
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/api/orders").header("Authorization", customer).header("Idempotency-Key", key)
                        .contentType("application/json").content("""
                                {"pickupName":"Khách","pickupPhone":"0900000128","method":"store",
                                 "expectedTotal":19600,"expectedDiscount":0}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.items[0].unitPrice").value(19600));
        at("2026-11-09T00:00:00"); sales.refreshAll();
        assertEquals(20000, price(2001));
        String code = "TZ-" + key.replace("-", "").toUpperCase();
        mvc.perform(get("/api/orders/" + code).header("Authorization", customer))
                .andExpect(jsonPath("$.items[0].unitPrice").value(19600)).andExpect(jsonPath("$.finalAmount").value(19600));
    }

    @Test void checkoutRefreshesBoundaryPriceEvenIfSchedulerHasNotRun() throws Exception {
        mvc.perform(put("/api/admin/product-sales/200").header("Authorization", admin).contentType("application/json")
                        .content(payload(2, true, "2026-11-08T11:00", "2026-11-08T12:00")))
                .andExpect(status().isOk());
        addToCart(); at("2026-11-08T11:00:00");
        mvc.perform(post("/api/checkout/validate").header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(19600));
        at("2026-11-08T12:00:00");
        mvc.perform(post("/api/checkout/validate").header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(20000));
    }

    @Test void staleExpectedPriceDoesNotCreateAnOrderOrDeductStock() throws Exception {
        activeSale(2); addToCart();
        mvc.perform(post("/api/orders").header("Authorization", customer).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json").content("""
                                {"pickupName":"Khách","pickupPhone":"0900000128","method":"store",
                                 "expectedTotal":20000,"expectedDiscount":0}
                                """))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class));
        assertEquals(20, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=2001", Integer.class));
    }

    @Test void newVariantUsesProductSaleAndMissingOriginalPriceIsRestoredAfterSale() throws Exception {
        activeSale(2);
        jdbc.update("INSERT INTO product_variants VALUES (2003,200,'LAP-C','32GB',50000,NULL,20,NULL)");
        sales.refreshAll();
        assertEquals(49000, price(2003));
        assertEquals(50000, jdbc.queryForObject("SELECT original_price FROM product_variants WHERE id=2003", Integer.class));
        at("2026-11-09T00:00:00"); promotions.syncProductPrices(List.of(2003));
        assertEquals(50000, price(2003));
        assertNull(jdbc.queryForObject("SELECT original_price FROM product_variants WHERE id=2003", java.math.BigDecimal.class));
    }

    @Test void saleNeverRaisesAnExistingLowerRegularPrice() throws Exception {
        jdbc.update("UPDATE product_variants SET price=19000 WHERE id=2001");
        activeSale(2);
        assertEquals(19000, price(2001));
        assertEquals(39200, price(2002));
        at("2026-11-09T00:00:00"); sales.refreshAll();
        assertEquals(19000, price(2001));
    }

    @Test void manualRegularPriceChangesDuringSaleAreNotLostAtEnd() throws Exception {
        activeSale(2);
        jdbc.update("UPDATE product_variants SET price=18000 WHERE id=2001");
        sales.refreshAll();
        assertEquals(18000, price(2001));
        jdbc.update("UPDATE product_variants SET price=19600 WHERE id=2001");
        sales.refreshAll();
        assertEquals(19600, price(2001));
        at("2026-11-09T00:00:00"); sales.refreshAll();
        assertEquals(19600, price(2001));
    }

    @Test void onlyAdminCanManageSalesAndInvalidCodesPercentagesOrDatesAreRejected() throws Exception {
        String valid = payload(2, true, "2026-11-08T00:00", "2026-11-09T00:00");
        for (String actor : new String[]{customer, staff}) {
            mvc.perform(put("/api/admin/product-sales/200").header("Authorization", actor).contentType("application/json").content(valid))
                    .andExpect(status().isForbidden());
        }
        for (String invalid : new String[]{valid.replace("\"percentage\":2", "\"percentage\":0"),
                valid.replace("\"percentage\":2", "\"percentage\":101"),
                valid.replace("\"percentage\":2", "\"percentage\":1.234"),
                payload(2, true, "2026-11-09T00:00", "2026-11-08T00:00")}) {
            mvc.perform(put("/api/admin/product-sales/200").header("Authorization", admin).contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/admin/product-sales/999").header("Authorization", admin).contentType("application/json").content(valid))
                .andExpect(status().isNotFound());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM product_sales", Integer.class));
    }
}
