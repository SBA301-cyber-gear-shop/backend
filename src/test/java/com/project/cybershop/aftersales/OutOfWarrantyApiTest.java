package com.project.cybershop.aftersales;

import com.project.cybershop.shared.config.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aftersales;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:purchase-schema.sql,classpath:aftersales-test-schema.sql"
})
@AutoConfigureMockMvc
@Transactional
class OutOfWarrantyApiTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    private String staff, technician, customer, otherCustomer;
    private int cardId;

    @BeforeEach
    void setUp() {
        staff = "Bearer " + jwt.generateToken("staff@techzone.demo", "STAFF");
        technician = "Bearer " + jwt.generateToken("technician@techzone.demo", "TECHNICIAN");
        customer = "Bearer " + jwt.generateToken("customer@techzone.demo", "CUSTOMER");
        otherCustomer = "Bearer " + jwt.generateToken("other@example.test", "CUSTOMER");
        Integer userId = jdbc.queryForObject("SELECT id FROM users WHERE email='customer@techzone.demo'", Integer.class);
        jdbc.update("INSERT INTO users(role_id,full_name,email,password_hash,is_active) SELECT role_id,'Other','other@example.test','test',true FROM users WHERE id=?", userId);
        jdbc.update("INSERT INTO brands VALUES (20,'Test')");
        jdbc.update("INSERT INTO categories VALUES (20,'Nguon','psu')");
        jdbc.update("INSERT INTO products VALUES (20,20,20,'Nguon','active','',12)");
        jdbc.update("INSERT INTO product_variants(id,product_id,serial_code,variant_name,price,original_price,stock_quantity,image_url) VALUES (201,20,'SKU-201','650W',1000000,1000000,3,NULL)");
        jdbc.update("INSERT INTO orders(user_id,order_code,total_amount,discount_amount,final_amount,status,pickup_name,pickup_phone) VALUES (?,'TEST-ORDER',1000000,0,1000000,'completed','Khach','0900000000')", userId);
        Integer orderId = jdbc.queryForObject("SELECT id FROM orders WHERE order_code='TEST-ORDER'", Integer.class);
        jdbc.update("INSERT INTO order_items(order_id,product_variant_id,warranty_period_months,product_name,quantity,unit_price,subtotal) VALUES (?,201,12,'Nguon',1,1000000,1000000)", orderId);
        Integer itemId = jdbc.queryForObject("SELECT id FROM order_items WHERE order_id=?", Integer.class, orderId);
        jdbc.update("INSERT INTO warranty_cards(order_item_id,serial_number,start_date,end_date,status) VALUES (?,'EXPIRED-PSU',?,?,'active')",
                itemId, LocalDate.now().minusYears(2).atStartOfDay(), LocalDate.now().minusDays(1).atStartOfDay());
        cardId = jdbc.queryForObject("SELECT id FROM warranty_cards WHERE serial_number='EXPIRED-PSU'", Integer.class);
    }

    @Test
    void staffTechnicianAndCustomerCanFollowExpiredDeviceWithoutRepairingIt() throws Exception {
        String intake = """
                {"warrantyCardIds":[],"outOfWarrantyCardIds":[%d],"issueDescription":"Máy không chạy",
                 "conditionNote":"Nguyên vẹn","accessoriesNote":"Nguồn","contactName":"Khách",
                 "contactPhone":"0900000000","expectedReturnDate":"%s"}
                """.formatted(cardId, LocalDate.now().plusDays(2));
        mvc.perform(post("/api/service-intakes").header("Authorization", staff)
                        .contentType("application/json").content(intake))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requests[0].serialNumber").value("EXPIRED-PSU"))
                .andExpect(jsonPath("$.data.requests[0].underWarrantyAtIntake").value(false));
        int id = jdbc.queryForObject("SELECT id FROM service_requests WHERE warranty_card_id=?", Integer.class, cardId);
        mvc.perform(get("/api/service-requests").header("Authorization", technician))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].underWarrantyAtIntake").value(false));
        mvc.perform(get("/api/service-requests/" + id).header("Authorization", customer))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/service-requests/" + id + "/transitions").header("Authorization", technician)
                        .contentType("application/json").content("{\"toStatus\":\"inspecting\",\"internalNote\":\"Kiểm tra\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/service-requests/" + id + "/transitions").header("Authorization", technician)
                        .contentType("application/json").content("{\"toStatus\":\"repairing\",\"internalNote\":\"Sửa\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/service-requests/" + id + "/transitions").header("Authorization", technician)
                        .contentType("application/json").content("{\"toStatus\":\"ready_for_return\",\"resolution\":\"rejected\",\"internalNote\":\"Nguồn hỏng\",\"publicNote\":\"Nguồn đã hết bảo hành và bị hỏng\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.publicNote").value("Nguồn đã hết bảo hành và bị hỏng"));
        mvc.perform(get("/api/service-requests/" + id).header("Authorization", staff))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.publicNote").value("Nguồn đã hết bảo hành và bị hỏng"));
        mvc.perform(get("/api/my/service-intakes").header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].components[0].underWarrantyAtIntake").value(false))
                .andExpect(jsonPath("$.data[0].components[0].publicNote").value("Nguồn đã hết bảo hành và bị hỏng"));
        mvc.perform(get("/api/my/service-intakes").header("Authorization", otherCustomer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM service_requests WHERE warranty_card_id=?", Integer.class, cardId));
    }

    @Test
    void technicianCanAddAnExpiredDeviceToAnExistingIntake() throws Exception {
        Integer itemId = jdbc.queryForObject("SELECT order_item_id FROM warranty_cards WHERE id=?", Integer.class, cardId);
        jdbc.update("INSERT INTO warranty_cards(order_item_id,serial_number,start_date,end_date,status) VALUES (?,'VALID-PSU',?,?,'active')",
                itemId, LocalDate.now().minusMonths(1).atStartOfDay(), LocalDate.now().plusMonths(1).atStartOfDay());
        Integer validId = jdbc.queryForObject("SELECT id FROM warranty_cards WHERE serial_number='VALID-PSU'", Integer.class);
        String intake = """
                {"warrantyCardIds":[%d],"outOfWarrantyCardIds":[],"issueDescription":"Máy không chạy",
                 "conditionNote":"Nguyên vẹn","accessoriesNote":"Có nguồn nhận kèm","contactName":"Khách",
                 "contactPhone":"0900000000","expectedReturnDate":"%s"}
                """.formatted(validId, LocalDate.now().plusDays(2));
        mvc.perform(post("/api/service-intakes").header("Authorization", staff)
                        .contentType("application/json").content(intake)).andExpect(status().isCreated());
        int intakeId = jdbc.queryForObject("SELECT id FROM service_intakes", Integer.class);
        String body = "{\"warrantyCardId\":" + cardId + ",\"internalNote\":\"Nguồn đã nhận kèm\"}";
        mvc.perform(post("/api/service-intakes/" + intakeId + "/requests").header("Authorization", staff)
                        .contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/service-intakes/" + intakeId + "/requests").header("Authorization", technician)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.requests.length()").value(2))
                .andExpect(jsonPath("$.data.requests[1].serialNumber").value("EXPIRED-PSU"))
                .andExpect(jsonPath("$.data.requests[1].underWarrantyAtIntake").value(false))
                .andExpect(jsonPath("$.data.requests[1].status").value("inspecting"));
        mvc.perform(post("/api/service-intakes/" + intakeId + "/requests").header("Authorization", technician)
                        .contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void noFaultOnlyChangesExplicitlySelectedCurrentComponents() throws Exception {
        Integer itemId = jdbc.queryForObject("SELECT order_item_id FROM warranty_cards WHERE id=?", Integer.class, cardId);
        for (String serial : new String[]{"VALID-A", "VALID-B", "VALID-C"}) {
            jdbc.update("INSERT INTO warranty_cards(order_item_id,serial_number,start_date,end_date,status) VALUES (?,?,?,?,'active')",
                    itemId, serial, LocalDate.now().minusMonths(1).atStartOfDay(), LocalDate.now().plusMonths(1).atStartOfDay());
        }
        var ids = jdbc.queryForList("SELECT id FROM warranty_cards WHERE serial_number LIKE 'VALID-%' ORDER BY serial_number", Integer.class);
        String intake = """
                {"warrantyCardIds":[%d,%d,%d],"outOfWarrantyCardIds":[%d],"issueDescription":"Máy lỗi",
                 "conditionNote":"Nguyên vẹn","accessoriesNote":"Không có","contactName":"Khách",
                 "contactPhone":"0900000000","expectedReturnDate":"%s"}
                """.formatted(ids.get(0), ids.get(1), ids.get(2), cardId, LocalDate.now().plusDays(2));
        mvc.perform(post("/api/service-intakes").header("Authorization", staff)
                .contentType("application/json").content(intake)).andExpect(status().isCreated());
        int intakeId = jdbc.queryForObject("SELECT id FROM service_intakes", Integer.class);
        var requests = jdbc.queryForList("SELECT id FROM service_requests WHERE intake_id=? ORDER BY id", Integer.class, intakeId);
        for (int index : new int[]{1, 2}) {
            mvc.perform(post("/api/service-requests/" + requests.get(index) + "/transitions").header("Authorization", technician)
                    .contentType("application/json").content("{\"toStatus\":\"inspecting\",\"internalNote\":\"Kiểm tra\"}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/service-requests/" + requests.get(2) + "/transitions").header("Authorization", technician)
                .contentType("application/json").content("{\"toStatus\":\"repairing\",\"internalNote\":\"Đang sửa\"}"))
                .andExpect(status().isOk());
        String url = "/api/service-intakes/" + intakeId + "/no-fault-rest";
        mvc.perform(post(url).header("Authorization", technician).contentType("application/json")
                .content("{\"internalNote\":\"Kiểm tra\"}")).andExpect(status().isBadRequest());
        mvc.perform(post(url).header("Authorization", technician).contentType("application/json")
                .content("{\"components\":[],\"internalNote\":\"Kiểm tra\"}")).andExpect(status().isBadRequest());
        Long version = jdbc.queryForObject("SELECT version FROM service_requests WHERE id=?", Long.class, requests.get(0));
        Long expiredVersion = jdbc.queryForObject("SELECT version FROM service_requests WHERE id=?", Long.class, requests.get(3));
        mvc.perform(get("/api/service-intakes/" + intakeId).header("Authorization", technician))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.requests[0].version").value(version));
        String component = "{\"id\":" + requests.get(0) + ",\"version\":" + version + "}";
        String invalid = "{\"components\":[" + component + ",{\"id\":999999,\"version\":0}],\"internalNote\":\"Kiểm tra\"}";
        mvc.perform(post(url).header("Authorization", technician).contentType("application/json").content(invalid))
                .andExpect(status().isBadRequest());
        assertEquals("received", jdbc.queryForObject("SELECT status FROM service_requests WHERE id=?", String.class, requests.get(0)));
        mvc.perform(post(url).header("Authorization", technician).contentType("application/json")
                .content("{\"components\":[{\"id\":" + requests.get(0) + ",\"version\":" + (version + 1) + "}],\"internalNote\":\"Kiểm tra\"}"))
                .andExpect(status().isBadRequest());
        String body = "{\"components\":[" + component + ",{\"id\":" + requests.get(3) + ",\"version\":" + expiredVersion + "}],\"internalNote\":\"Đã kiểm tra\"}";
        mvc.perform(post(url).header("Authorization", staff).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post(url).header("Authorization", technician).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.canReturn").value(false));
        assertEquals("ready_for_return", jdbc.queryForObject("SELECT status FROM service_requests WHERE id=?", String.class, requests.get(0)));
        assertEquals("inspecting", jdbc.queryForObject("SELECT status FROM service_requests WHERE id=?", String.class, requests.get(1)));
        assertEquals("repairing", jdbc.queryForObject("SELECT status FROM service_requests WHERE id=?", String.class, requests.get(2)));
        assertEquals("ready_for_return", jdbc.queryForObject("SELECT status FROM service_requests WHERE id=?", String.class, requests.get(3)));
    }
}
