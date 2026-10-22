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
}
