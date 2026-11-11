package com.project.cybershop;

import com.project.cybershop.order.api.dto.request.CollectPaymentRequest;
import com.project.cybershop.order.api.dto.request.HandoverRequest;
import com.project.cybershop.order.api.dto.response.OrderResponse;
import com.project.cybershop.order.internal.service.OrderService;
import com.project.cybershop.order.api.dto.request.PlaceOrderRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:pickup-race;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
class PickupConcurrencyTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired OrderService orders;
    @Autowired com.project.cybershop.order.internal.service.CartService carts;
    @Autowired com.project.cybershop.review.internal.service.ReviewService reviews;
    int first,second;

    @BeforeEach void seed() {
        clean();
        first=jdbc.queryForObject("SELECT id FROM users WHERE email='customer@techzone.demo'",Integer.class);
        if (jdbc.queryForObject("SELECT count(*) FROM users WHERE email='parallel@example.com'",Integer.class)==0)
            jdbc.update("INSERT INTO users (role_id,full_name,email,password_hash,is_active) SELECT id,'Parallel','parallel@example.com','unused',TRUE FROM roles WHERE upper(name)='CUSTOMER'");
        second=jdbc.queryForObject("SELECT id FROM users WHERE email='parallel@example.com'",Integer.class);
        jdbc.update("INSERT INTO brands VALUES (30,'Test')");
        jdbc.update("INSERT INTO categories VALUES (30,'RAM','ram')");
        jdbc.update("INSERT INTO products VALUES (30,30,30,'RAM A','active','',36)");
        jdbc.update("INSERT INTO products VALUES (31,30,30,'RAM B','active','',36)");
        jdbc.update("INSERT INTO product_variants VALUES (301,30,'RACE-A','16GB',1000000,1000000,1,NULL)");
        jdbc.update("INSERT INTO product_variants VALUES (302,31,'RACE-B','16GB',1000000,1000000,1,NULL)");
        for(int user : new int[]{first,second}) {
            jdbc.update("INSERT INTO carts (user_id) VALUES (?)",user);
            int cart=jdbc.queryForObject("SELECT id FROM carts WHERE user_id=?",Integer.class,user);
            jdbc.update("INSERT INTO cart_items (cart_id,product_variant_id,quantity) VALUES (?,301,1)",cart);
        }
    }

    @AfterEach void clean() {
        for(String table : new String[]{"reviews","warranty_cards","promotion_usages","payments","order_items","orders","cart_items","carts","payment_methods","banners","promotions","specification_values","category_specifications","product_variants","products","brands","categories"})
            jdbc.update("DELETE FROM "+table);
    }

    PlaceOrderRequest request(String promo) {
        return new PlaceOrderRequest("Khách","0900000128","",promo,"store",null,new BigDecimal("1000000"),new BigDecimal(promo.isEmpty()?"0":"100000"));
    }

    int race(String promo) throws Exception {
        var barrier=new CyclicBarrier(2);
        // ExecutorService chỉ là AutoCloseable từ Java 19; dự án biên dịch theo Java 17.
        var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->attempt(barrier,"customer@techzone.demo",promo));
            var b=pool.submit(()->attempt(barrier,"parallel@example.com",promo));
            return (a.get(20,TimeUnit.SECONDS)?1:0)+(b.get(20,TimeUnit.SECONDS)?1:0);
        } finally { pool.shutdownNow(); }
    }

    boolean attempt(CyclicBarrier barrier,String email,String promo) throws Exception {
        barrier.await(10,TimeUnit.SECONDS);
        try { orders.place(email,UUID.randomUUID().toString(),request(promo)); return true; }
        catch(com.project.cybershop.shared.exception.BusinessException expected) { return false; }
    }

    @Test void twoCustomersCannotBuyTheLastUnitTwice() throws Exception {
        assertEquals(1,race(""));
        assertEquals(0,jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=301",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM orders",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM cart_items",Integer.class));
    }

    @Test
    void concurrentRetriesWithTheSameKeyCreateOneOrderAndDeductStockOnce() throws Exception {
        String key = UUID.randomUUID().toString();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            Callable<OrderResponse> placeOrder = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return orders.place("customer@techzone.demo", key, request(""));
            };

            Future<OrderResponse> firstAttempt = pool.submit(placeOrder);
            Future<OrderResponse> retry = pool.submit(placeOrder);

            assertEquals(firstAttempt.get(20, TimeUnit.SECONDS).id(), retry.get(20, TimeUnit.SECONDS).id());
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM order_items", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM payments", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT stock_quantity FROM product_variants WHERE id=301", Integer.class
        ));
    }

    @Test void differentProductsStillCompeteForOneVoucherReservation() throws Exception {
        jdbc.update("UPDATE cart_items SET product_variant_id=302 WHERE cart_id=(SELECT id FROM carts WHERE user_id=?)",second);
        jdbc.update("INSERT INTO promotions (code,name,discount_type,discount_value,usage_limit,is_active,start_date,end_date) VALUES ('LAST','Last','fixed_amount',100000,1,TRUE,CURRENT_TIMESTAMP-INTERVAL '1' DAY,CURRENT_TIMESTAMP+INTERVAL '1' DAY)");
        assertEquals(1,race("LAST"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM promotion_usages WHERE status='RESERVED'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM orders",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT sum(stock_quantity) FROM product_variants",Integer.class));
    }

    @Test void paymentInsertFailureRollsBackOrderStockVoucherAndCart() {
        jdbc.update("INSERT INTO promotions (code,name,discount_type,discount_value,usage_limit,is_active,start_date,end_date) VALUES ('LAST','Last','fixed_amount',100000,1,TRUE,CURRENT_TIMESTAMP-INTERVAL '1' DAY,CURRENT_TIMESTAMP+INTERVAL '1' DAY)");
        jdbc.execute("ALTER TABLE payments ADD CONSTRAINT force_payment_failure CHECK (amount<0)");
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->orders.place("customer@techzone.demo",UUID.randomUUID().toString(),request("LAST")));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM orders",Integer.class));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM promotion_usages",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=301",Integer.class));
            assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM cart_items",Integer.class));
        } finally { jdbc.execute("ALTER TABLE payments DROP CONSTRAINT force_payment_failure"); }
    }

    @Test void duplicateSerialLateInHandoverRollsBackAllNewCards() {
        jdbc.update("UPDATE product_variants SET stock_quantity=2 WHERE id=301");
        jdbc.update("UPDATE cart_items SET quantity=2 WHERE cart_id=(SELECT id FROM carts WHERE user_id=?)",first);
        var request=new PlaceOrderRequest("Khách","0900000128","","","store",null,new BigDecimal("2000000"),BigDecimal.ZERO);
        var order=orders.place("customer@techzone.demo",UUID.randomUUID().toString(),request);
        for(String status : new String[]{"confirmed","preparing","ready_for_pickup"}) orders.advance("staff@techzone.demo",order.orderCode(),status);
        orders.collect("staff@techzone.demo",order.orderCode(),new CollectPaymentRequest(new BigDecimal("2000000"),null));
        int itemId=order.items().get(0).id();
        jdbc.update("INSERT INTO warranty_cards (order_item_id,serial_number,start_date,end_date,status) VALUES (?,'ALREADY-USED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'active')",itemId);
        var devices=java.util.List.of(new HandoverRequest.Device(itemId,"FIRST-NEW"),
                new HandoverRequest.Device(itemId,"ALREADY-USED"));
        assertThrows(com.project.cybershop.shared.exception.BusinessException.class,()->orders.handover("staff@techzone.demo",order.orderCode(),
                new HandoverRequest("0900000128",true,devices)));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM warranty_cards",Integer.class));
        assertEquals("ready_for_pickup",orders.myOrder("customer@techzone.demo",order.orderCode()).status());
    }

    @Test void simultaneousReviewSavesKeepOneReviewPerPurchasedLine() throws Exception {
        var order = orders.place("customer@techzone.demo", UUID.randomUUID().toString(), request(""));
        for (String status : new String[]{"confirmed", "preparing", "ready_for_pickup"}) {
            orders.advance("staff@techzone.demo", order.orderCode(), status);
        }
        orders.collect("staff@techzone.demo", order.orderCode(),
                new CollectPaymentRequest(new BigDecimal("1000000"), null));
        int itemId = order.items().get(0).id();
        orders.handover("staff@techzone.demo", order.orderCode(),
                new HandoverRequest("0900000128", true,
                        java.util.List.of(new HandoverRequest.Device(itemId, "REVIEW-RACE"))));

        int accepted = runConcurrent(
                () -> reviews.save("customer@techzone.demo", itemId,
                        new com.project.cybershop.review.api.dto.request.ReviewRequest(3, "Nhận xét đầu")),
                () -> reviews.save("customer@techzone.demo", itemId,
                        new com.project.cybershop.review.api.dto.request.ReviewRequest(5, "Nhận xét sau")));
        assertEquals(1, accepted);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM reviews WHERE order_item_id=?", Integer.class, itemId));
        assertEquals(1, reviews.forVariant(301).size());
    }
    @Test
    void invalidBatchItemRollsBackEarlierCartChanges() {
        jdbc.update("UPDATE product_variants SET stock_quantity=5 WHERE id=301");
        assertThrows(com.project.cybershop.shared.exception.ResourceNotFoundException.class,
                () -> carts.addBatchToCart(first, java.util.List.of(
                        new com.project.cybershop.order.api.dto.request.AddToCartRequest(301, 1),
                        new com.project.cybershop.order.api.dto.request.AddToCartRequest(999, 1))));
        assertEquals(1, jdbc.queryForObject("""
                SELECT quantity FROM cart_items WHERE cart_id=(SELECT id FROM carts WHERE user_id=?)
                """, Integer.class, first));
    }

    @Test
    void concurrentCartAddsMergeIntoOneLineWithoutLostQuantities() throws Exception {
        jdbc.update("UPDATE product_variants SET stock_quantity=5 WHERE id=301");
        var addition = new com.project.cybershop.order.api.dto.request.AddToCartRequest(301, 1);
        assertEquals(2, runConcurrent(
                () -> carts.addToCart(first, addition), () -> carts.addToCart(first, addition)));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM cart_items WHERE cart_id=(SELECT id FROM carts WHERE user_id=?)
                """, Integer.class, first));
        assertEquals(3, jdbc.queryForObject("""
                SELECT quantity FROM cart_items WHERE cart_id=(SELECT id FROM carts WHERE user_id=?)
                """, Integer.class, first));
        assertEquals(5, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=301", Integer.class));
    }

    @Test
    void distinctConcurrentCheckoutKeysCannotPurchaseTheSameCartTwice() throws Exception {
        assertEquals(1, runConcurrent(
                () -> orders.place("customer@techzone.demo", UUID.randomUUID().toString(), request("")),
                () -> orders.place("customer@techzone.demo", UUID.randomUUID().toString(), request(""))));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM payments", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=301", Integer.class));
    }

    @Test
    void simultaneousCollectionsRecordOnePaymentAndKeepTheFirstReference() throws Exception {
        var order = orders.place("customer@techzone.demo", UUID.randomUUID().toString(), request(""));
        assertEquals(2, runConcurrent(
                () -> orders.collect("staff@techzone.demo", order.orderCode(),
                        new CollectPaymentRequest(new BigDecimal("1000000"), "RECEIPT-A")),
                () -> orders.collect("staff@techzone.demo", order.orderCode(),
                        new CollectPaymentRequest(new BigDecimal("1000000"), "RECEIPT-B"))));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM payments", Integer.class));
        assertEquals("SUCCESS", jdbc.queryForObject("SELECT status FROM payments", String.class));
        String reference = jdbc.queryForObject("SELECT transaction_reference FROM payments", String.class);
        assertTrue(java.util.Set.of("RECEIPT-A", "RECEIPT-B").contains(reference));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM order_cash_collections", Integer.class));
        assertEquals(reference, orders.cashCollection("staff@techzone.demo", order.orderCode()).orElseThrow().reference());
    }

    @Test
    void collectionRacingCancellationCannotLeaveAPaidCanceledOrder() throws Exception {
        var order = orders.place("customer@techzone.demo", UUID.randomUUID().toString(), request(""));
        assertEquals(1, runConcurrent(
                () -> orders.collect("staff@techzone.demo", order.orderCode(),
                        new CollectPaymentRequest(new BigDecimal("1000000"), "RACE-COLLECT")),
                () -> orders.cancel("customer@techzone.demo", order.orderCode(), false, "Khách đổi nhu cầu")));
        String status = jdbc.queryForObject("SELECT status FROM orders", String.class);
        boolean canceled = "canceled".equals(status);
        assertTrue(canceled || "pending".equals(status));
        assertEquals(canceled ? "CANCELED" : "SUCCESS", jdbc.queryForObject("SELECT status FROM payments", String.class));
        assertEquals(canceled ? 1 : 0, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=301", Integer.class));
    }

    @Test
    void concurrentHandoversIssueOnlyOneWarrantyPerDevice() throws Exception {
        var order = orders.place("customer@techzone.demo", UUID.randomUUID().toString(), request(""));
        for (String status : new String[]{"confirmed", "preparing", "ready_for_pickup"}) {
            orders.advance("staff@techzone.demo", order.orderCode(), status);
        }
        orders.collect("staff@techzone.demo", order.orderCode(),
                new CollectPaymentRequest(new BigDecimal("1000000"), null));
        var handover = new HandoverRequest("+84900000128", true,
                java.util.List.of(new HandoverRequest.Device(order.items().get(0).id(), "HANDOVER-RACE")));
        assertEquals(1, runConcurrent(
                () -> orders.handover("staff@techzone.demo", order.orderCode(), handover),
                () -> orders.handover("staff@techzone.demo", order.orderCode(), handover)));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM warranty_cards", Integer.class));
        assertEquals("completed", jdbc.queryForObject("SELECT status FROM orders", String.class));
    }

    @Test
    void missingPaymentBlocksCollectionAndCancellationEvenForAFreeOrder() {
        jdbc.update("UPDATE product_variants SET price=0 WHERE id=301");
        var freeRequest = new PlaceOrderRequest("Khách", "0900000128", "", "", "store", null,
                BigDecimal.ZERO, BigDecimal.ZERO);
        var order = orders.place("customer@techzone.demo", UUID.randomUUID().toString(), freeRequest);
        jdbc.update("DELETE FROM payments WHERE order_id=?", order.id());
        assertThrows(com.project.cybershop.shared.exception.BusinessException.class,
                () -> orders.collect("staff@techzone.demo", order.orderCode(), new CollectPaymentRequest(BigDecimal.ZERO, null)));
        assertThrows(com.project.cybershop.shared.exception.BusinessException.class,
                () -> orders.cancel("customer@techzone.demo", order.orderCode(), false, "Khách đổi nhu cầu"));
        assertEquals("pending", jdbc.queryForObject("SELECT status FROM orders", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=301", Integer.class));
    }

    private int runConcurrent(Runnable firstAction, Runnable secondAction) throws Exception {
        var barrier = new CyclicBarrier(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var firstResult = pool.submit(() -> runAfterBarrier(barrier, firstAction));
            var secondResult = pool.submit(() -> runAfterBarrier(barrier, secondAction));
            return (firstResult.get(20, TimeUnit.SECONDS) ? 1 : 0)
                    + (secondResult.get(20, TimeUnit.SECONDS) ? 1 : 0);
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean runAfterBarrier(CyclicBarrier barrier, Runnable action) throws Exception {
        barrier.await(10, TimeUnit.SECONDS);
        try {
            action.run();
            return true;
        } catch (com.project.cybershop.shared.exception.BusinessException rejected) {
            return false;
        }
    }
}
