package com.project.cybershop;

import com.project.cybershop.shared.config.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PromotionManagementApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @Autowired Clock clock;
    String admin, customer;

    @BeforeEach void setup() {
        admin="Bearer "+jwt.generateToken("admin@techzone.demo","ADMIN");
        customer="Bearer "+jwt.generateToken("customer@techzone.demo","CUSTOMER");
    }

    String payload(String code,int limit,boolean active) {
        return """
                {"code":"%s","name":"Ưu đãi nhận tại cửa hàng","type":"percentage","value":10,"limit":%d,
                 "start":"%s","end":"%s","active":%s,"bannerTitle":"Ưu đãi","bannerImage":"/banner.png",
                 "bannerLink":"/san-pham","bannerOrder":0,"bannerActive":true}
                """.formatted(code,limit,LocalDateTime.now(clock).minusDays(1),LocalDateTime.now(clock).plusDays(1),active);
    }

    int create(String code) throws Exception {
        mvc.perform(post("/api/admin/promotions").header("Authorization",admin).contentType("application/json").content(payload(code,10,true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code.trim().toUpperCase()));
        return jdbc.queryForObject("SELECT id FROM promotions WHERE code=?",Integer.class,code.trim().toUpperCase());
    }

    @Test void adminCreatesNormalizesUpdatesAndDeactivatesPublicPromotion() throws Exception {
        int id=create(" save10 ");
        mvc.perform(get("/api/promotions")).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("SAVE10"));
        mvc.perform(post("/api/admin/promotions").header("Authorization",admin).contentType("application/json").content(payload("save10",10,true)))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/promotions/"+id).header("Authorization",admin).contentType("application/json").content(payload("SAVE10",10,false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/promotions")).andExpect(jsonPath("$").isEmpty());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM banners WHERE promotion_id=?",Integer.class,id));
    }

    @Test void quotaIncludesReservationsAndUsedCodeCannotBeRenamed() throws Exception {
        int id=create("RESERVED");
        int user=jdbc.queryForObject("SELECT id FROM users WHERE email='customer@techzone.demo'",Integer.class);
        int other=jdbc.queryForObject("SELECT id FROM users WHERE email='staff@techzone.demo'",Integer.class);
        jdbc.update("INSERT INTO orders (id,user_id,order_code,total_amount,final_amount,status) VALUES (401,?,'P401',1000,900,'pending')",user);
        jdbc.update("INSERT INTO orders (id,user_id,order_code,total_amount,final_amount,status) VALUES (402,?,'P402',1000,900,'pending')",other);
        jdbc.update("INSERT INTO promotion_usages (promotion_id,user_id,order_id,status,discount_amount) VALUES (?,?,401,'RESERVED',100)",id,user);
        jdbc.update("INSERT INTO promotion_usages (promotion_id,user_id,order_id,status,discount_amount) VALUES (?,?,402,'USED',100)",id,other);
        mvc.perform(put("/api/admin/promotions/"+id).header("Authorization",admin).contentType("application/json").content(payload("RESERVED",1,true)))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/promotions/"+id).header("Authorization",admin).contentType("application/json").content(payload("RENAMED",10,true)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/promotions").header("Authorization",admin))
                .andExpect(jsonPath("$[0].reserved").value(1)).andExpect(jsonPath("$[0].used").value(1));
    }

    @Test void customerAndSpoofedRoleCannotManagePromotionsAndInvalidValuesAreRejected() throws Exception {
        mvc.perform(get("/api/admin/promotions").header("Authorization",customer)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/promotions").header("Authorization","Bearer "+jwt.generateToken("customer@techzone.demo","ADMIN"))
                .contentType("application/json").content(payload("NOPE",10,true))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/promotions").header("Authorization",admin).contentType("application/json")
                .content(payload("BADVALUE",10,true).replace("\"value\":10","\"value\":101")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/promotions").header("Authorization",admin).contentType("application/json")
                .content(payload("BADLINK",10,true).replace("/san-pham","//evil.example")))
                .andExpect(status().isBadRequest());
    }

    int historyData() throws Exception {
        int promotionId = create("HISTORY");
        String[] emails = {"customer@techzone.demo", "staff@techzone.demo", "admin@techzone.demo"};
        String[] statuses = {"USED", "RESERVED", "REVOKED"};
        String[] codes = {"TZ-HISTORY-NEW", "TZ-HISTORY-RESERVED", "TZ-HISTORY-REVOKED"};
        for (int i=0; i<3; i++) {
            int user = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Integer.class, emails[i]);
            jdbc.update("INSERT INTO orders (id,user_id,order_code,total_amount,final_amount,status,created_at) VALUES (?,?,?,1000000,900000,?,?)",
                    501+i, user, codes[i], i == 0 ? "completed" : i == 1 ? "pending" : "canceled", LocalDateTime.of(2026, 11, 8-i, 10, 0));
            jdbc.update("INSERT INTO promotion_usages (promotion_id,user_id,order_id,status,discount_amount) VALUES (?,?,?,?,?)",
                    promotionId, user, 501+i, statuses[i], 100000-i*10000);
        }
        return promotionId;
    }

    @Test void adminCanSeeVoucherCustomersOrdersAmountsAndStatusesWithStablePagination() throws Exception {
        int id = historyData();
        int other = create("OTHER-HISTORY");
        int user = jdbc.queryForObject("SELECT id FROM users WHERE email='customer@techzone.demo'", Integer.class);
        jdbc.update("INSERT INTO orders (id,user_id,order_code,total_amount,final_amount,status,created_at) VALUES (504,?,'TZ-OTHER-HISTORY',1000,900,'pending',CURRENT_TIMESTAMP)", user);
        jdbc.update("INSERT INTO promotion_usages (promotion_id,user_id,order_id,status,discount_amount) VALUES (?,?,504,'RESERVED',100)", other, user);
        mvc.perform(get("/api/admin/promotions/"+id+"/usages").header("Authorization", admin).param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("HISTORY"))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].customerName").isNotEmpty())
                .andExpect(jsonPath("$.items[0].customerEmail").value("customer@techzone.demo"))
                .andExpect(jsonPath("$.items[0].orderCode").value("TZ-HISTORY-NEW"))
                .andExpect(jsonPath("$.items[0].discountAmount").value(100000))
                .andExpect(jsonPath("$.items[0].orderCreatedAt").value("2026-11-08T10:00:00"))
                .andExpect(jsonPath("$.items[0].status").value("USED"))
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.items[1].status").value("RESERVED"));
        mvc.perform(get("/api/admin/promotions/"+id+"/usages").header("Authorization", admin).param("size", "2").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].orderCode").value("TZ-HISTORY-REVOKED"))
                .andExpect(jsonPath("$.items[0].status").value("REVOKED"));
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM promotion_usages", Integer.class));
    }

    @Test void historyFiltersStatusAndSearchesCustomerOrOrderWithoutWildcardExpansion() throws Exception {
        int id = historyData();
        for (String state : new String[]{"USED", "RESERVED", "REVOKED"}) {
            mvc.perform(get("/api/admin/promotions/"+id+"/usages").header("Authorization", admin).param("status", state))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.items[0].status").value(state));
        }
        mvc.perform(get("/api/admin/promotions/"+id+"/usages").header("Authorization", admin).param("search", "CUSTOMER@TECHZONE.DEMO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].customerEmail").value("customer@techzone.demo"));
        mvc.perform(get("/api/admin/promotions/"+id+"/usages").header("Authorization", admin).param("search", "hIsToRy-ReVoKeD"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].orderCode").value("TZ-HISTORY-REVOKED"));
        for (String term : new String[]{"%", "_", "' OR 1=1--", "no-such-customer"}) {
            mvc.perform(get("/api/admin/promotions/"+id+"/usages").header("Authorization", admin).param("search", term))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty()).andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Test void usageHistoryProtectsCustomerDataValidatesInputsAndSupportsEmptyVouchers() throws Exception {
        int id = create("EMPTY-HISTORY");
        String path = "/api/admin/promotions/"+id+"/usages";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        for (String actor : new String[]{customer,
                "Bearer "+jwt.generateToken("staff@techzone.demo", "STAFF"),
                "Bearer "+jwt.generateToken("customer@techzone.demo", "ADMIN")}) {
            mvc.perform(get(path).header("Authorization", actor)).andExpect(status().isForbidden());
        }
        mvc.perform(get(path).header("Authorization", admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty()).andExpect(jsonPath("$.totalElements").value(0));
        for (String[] invalid : new String[][]{{"page","-1"},{"size","0"},{"size","101"},{"page","2147483647"},{"status","BOGUS"},{"search","a".repeat(101)}}) {
            mvc.perform(get(path).header("Authorization", admin).param(invalid[0], invalid[1]))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/admin/promotions/999999/usages").header("Authorization", admin)).andExpect(status().isNotFound());
    }
}
