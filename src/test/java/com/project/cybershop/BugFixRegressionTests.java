package com.project.cybershop;

import com.project.cybershop.shared.config.JwtUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BugFixRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @Autowired EntityManager em;
    private String customer;

    @BeforeEach
    void seed() {
        customer = "Bearer " + jwt.generateToken("customer@techzone.demo", "CUSTOMER");
        jdbc.update("INSERT INTO brands VALUES (10, 'Kingston')");
        jdbc.update("INSERT INTO categories VALUES (10, 'Bộ nhớ RAM', 'ram')");
        jdbc.update("INSERT INTO products VALUES (10, 10, 10, 'Kingston RAM', 'ACTIVE', 'RAM', 36)");
        jdbc.update("INSERT INTO product_variants VALUES (101, 10, 'RAM16', '16GB', 1200000, 1300000, 5, 'a.png')");
        jdbc.update("INSERT INTO product_variants VALUES (102, 10, 'RAM32', '32GB', 2200000, 2300000, 5, 'b.png')");
        jdbc.update("INSERT INTO product_variants VALUES (103, 10, 'RAM64', '64GB', 3200000, 3300000, 5, 'c.png')");
    }

    @Test
    void c4_customerAndStaffCannotReadAdminApis() throws Exception {
        String staff = "Bearer " + jwt.generateToken("staff@techzone.demo", "STAFF");
        for (String path : List.of("/api/admin/products", "/api/admin/products/10", "/api/admin/products/10/variants",
                "/api/admin/brands", "/api/admin/brands/10", "/api/admin/categories", "/api/admin/categories/10",
                "/api/admin/specifications", "/api/admin/specifications/10", "/api/admin/specifications/10/options",
                "/api/admin/promotions")) {
            mvc.perform(get(path).header("Authorization", customer))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này."));
            mvc.perform(get(path).header("Authorization", staff)).andExpect(status().isForbidden());
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void c4_adminStillPassesAuthorization() throws Exception {
        String admin = "Bearer " + jwt.generateToken("admin@techzone.demo", "ADMIN");
        mvc.perform(get("/api/admin/promotions").header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void t7_clientMistakesAreNot500AndDoNotLeakInternals() throws Exception {
        mvc.perform(get("/api/khong-ton-tai"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy đường dẫn yêu cầu."));
        mvc.perform(delete("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Phương thức này không được hỗ trợ."));
        mvc.perform(post("/api/auth/login").contentType("text/plain").content("x"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/orders").header("Authorization", customer).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Yêu cầu không hợp lệ."));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void t8_validationMessagesAreVietnamese() throws Exception {
        mvc.perform(post("/api/orders").header("Authorization", customer)
                        .header("Idempotency-Key", "6f1c1b0e-8c34-4a55-9a7a-0d1f7a2b9c11")
                        .contentType("application/json").content("{\"pickupName\":\"\",\"method\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.pickupName").value("Vui lòng nhập tên người nhận"))
                .andExpect(jsonPath("$.validationErrors.expectedTotal").value("Thiếu tổng tiền dự kiến"));
    }

    @Test
    void t10_profileUpdatePersistsAndValidates() throws Exception {
        mvc.perform(put("/api/auth/me").contentType("application/json")
                        .content("{\"fullName\":\"Tên\",\"phone\":\"0911222333\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/auth/me").header("Authorization", customer).contentType("application/json")
                        .content("{\"fullName\":\"Tên\",\"phone\":\"12345\"}"))
                .andExpect(status().isBadRequest());
        // Số của tài khoản Admin
        mvc.perform(put("/api/auth/me").header("Authorization", customer).contentType("application/json")
                        .content("{\"fullName\":\"Tên\",\"phone\":\"0900000131\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Số điện thoại '0900000131' đã được sử dụng"));
        mvc.perform(put("/api/auth/me").header("Authorization", customer).contentType("application/json")
                        .content("{\"fullName\":\" Tên Mới \",\"phone\":\"0911222333\",\"address\":\" Hà Nội \",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Tên Mới"))
                .andExpect(jsonPath("$.data.address").value("Hà Nội"))
                .andExpect(jsonPath("$.data.role").value("Customer"));
        em.flush(); // cả test nằm trong một transaction nên JDBC chưa thấy thay đổi nếu chưa flush
        assertEquals("0911222333", jdbc.queryForObject(
                "SELECT phone FROM users WHERE email='customer@techzone.demo'", String.class));
    }

    @Test
    void cartKeepsInsertionOrderAfterUpdatingALine() throws Exception {
        for (int variant : new int[]{103, 101, 102}) {
            mvc.perform(post("/api/orders/cart/items").header("Authorization", customer)
                            .contentType("application/json")
                            .content("{\"productVariantId\":" + variant + ",\"quantity\":1}"))
                    .andExpect(status().isOk());
        }
        Integer first = jdbc.queryForObject("SELECT id FROM cart_items WHERE product_variant_id=103", Integer.class);
        mvc.perform(put("/api/orders/cart/items/" + first).header("Authorization", customer)
                        .contentType("application/json").content("{\"quantity\":2}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/orders/cart").header("Authorization", customer))
                .andExpect(jsonPath("$.data.items[0].productVariantId").value(103))
                .andExpect(jsonPath("$.data.items[1].productVariantId").value(101))
                .andExpect(jsonPath("$.data.items[2].productVariantId").value(102));
    }
}
