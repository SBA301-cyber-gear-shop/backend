package com.project.cybershop;

import com.project.cybershop.shared.config.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class CustomerVoucherApiTests {
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate jdbc;
 @Autowired JwtUtil jwt;
 @Autowired Clock clock;
 String admin, customer, other, staff;
 int userId, otherId;
 @BeforeEach void seed() {
  admin="Bearer "+jwt.generateToken("admin@techzone.demo","ADMIN");
  customer="Bearer "+jwt.generateToken("customer@techzone.demo","CUSTOMER");
  other="Bearer "+jwt.generateToken("wallet-other@example.com","CUSTOMER");
  staff="Bearer "+jwt.generateToken("staff@techzone.demo","STAFF");
  userId=jdbc.queryForObject("SELECT id FROM users WHERE email='customer@techzone.demo'",Integer.class);
  int role=jdbc.queryForObject("SELECT role_id FROM users WHERE id=?",Integer.class,userId);
  jdbc.update("INSERT INTO users (role_id,full_name,email,password_hash,is_active) VALUES (?,'Khách khác','wallet-other@example.com','test',true)",role);
  otherId=jdbc.queryForObject("SELECT id FROM users WHERE email='wallet-other@example.com'",Integer.class);
  promo(71,"ONLYME",-1,1,"ASSIGNED");
  promo(72,"EXPIRED",-2,-1,"ASSIGNED");
  promo(73,"FUTURE",1,2,"ASSIGNED");
  promo(74,"PUBLIC",-1,1,"PUBLIC");
 }
 void promo(int id,String code,int start,int end,String audience) {
  jdbc.update("INSERT INTO promotions (id,code,name,discount_type,discount_value,usage_limit,is_active,start_date,end_date,audience) VALUES (?,?,?,'fixed_amount',100000,10,true,?,?,?)",id,code,"Ưu đãi "+code,LocalDateTime.now(clock).plusDays(start),LocalDateTime.now(clock).plusDays(end),audience);
 }
 void grant(int id,int user) throws Exception {
  mvc.perform(post("/api/admin/promotions/"+id+"/recipients").header("Authorization",admin).contentType("application/json").content("{\"userIds\":["+user+"]}"))
   .andExpect(status().isNoContent());
 }
 @Test void walletOnlyShowsOwnGrantsAndDoesNotDistributePublicVouchersAutomatically() throws Exception {
  mvc.perform(get("/api/me/vouchers").header("Authorization",customer)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  grant(71,userId);grant(73,userId);
  jdbc.update("INSERT INTO promotion_assignments (promotion_id,user_id) VALUES (72,?)",userId);
  mvc.perform(get("/api/me/vouchers").header("Authorization",customer))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
   .andExpect(jsonPath("$[?(@.code=='ONLYME')].status").value("AVAILABLE"))
   .andExpect(jsonPath("$[?(@.code=='EXPIRED')].status").value("EXPIRED"))
   .andExpect(jsonPath("$[?(@.code=='FUTURE')].status").value("UPCOMING"));
  mvc.perform(get("/api/me/vouchers").header("Authorization",other)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
 }
 @Test void grantIsIdempotentAndCanRevokeUnusedVoucher() throws Exception {
  grant(71,userId);grant(71,userId);
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM promotion_assignments",Integer.class));
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM promotion_usages",Integer.class));
  mvc.perform(delete("/api/admin/promotions/71/recipients/"+userId).header("Authorization",admin)).andExpect(status().isNoContent());
  mvc.perform(get("/api/me/vouchers").header("Authorization",customer)).andExpect(jsonPath("$.length()").value(0));
 }
 void cart() throws Exception {
  jdbc.update("INSERT INTO brands VALUES (71,'Brand')");
  jdbc.update("INSERT INTO categories VALUES (71,'RAM','ram')");
  jdbc.update("INSERT INTO products VALUES (71,71,71,'RAM','active','Description',36)");
  jdbc.update("INSERT INTO product_variants VALUES (701,71,'WALLET-RAM','16GB',1000000,1000000,5,NULL)");
  mvc.perform(post("/api/orders/cart/items").header("Authorization",customer).contentType("application/json").content("{\"productVariantId\":701,\"quantity\":1}"))
   .andExpect(status().isOk());
 }
 String checkout() {return "{\"pickupName\":\"Khách nhận\",\"pickupPhone\":\"0900000128\",\"promotionCode\":\"ONLYME\",\"method\":\"store\",\"expectedTotal\":1000000,\"expectedDiscount\":100000}";}
 @Test void knowingCodeCannotBypassAssignmentInPreviewOrOrderCreation() throws Exception {
  cart();
  mvc.perform(post("/api/checkout/details/preview").header("Authorization",customer).contentType("application/json").content(checkout())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Voucher này chưa được cấp cho tài khoản của bạn."));
  mvc.perform(post("/api/orders").header("Authorization",customer).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(checkout())).andExpect(status().isBadRequest());
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM orders",Integer.class));
  assertEquals(5,jdbc.queryForObject("SELECT stock_quantity FROM product_variants WHERE id=701",Integer.class));
  grant(71,userId);
  mvc.perform(post("/api/checkout/details/preview").header("Authorization",customer).contentType("application/json").content(checkout()))
   .andExpect(status().isOk()).andExpect(jsonPath("$.finalAmount").value(900000));
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM promotion_usages",Integer.class));
 }
 @Test void reservedUsedAndCanceledOrdersDoNotRestoreCustomerEligibility() throws Exception {
  cart();grant(71,userId);
  String key=UUID.randomUUID().toString();
  String code="TZ-"+key.replace("-","").toUpperCase();
  mvc.perform(post("/api/orders").header("Authorization",customer).header("Idempotency-Key",key).contentType("application/json").content(checkout())).andExpect(status().isCreated());
  mvc.perform(get("/api/me/vouchers").header("Authorization",customer)).andExpect(jsonPath("$[0].status").value("RESERVED"));
  mvc.perform(delete("/api/admin/promotions/71/recipients/"+userId).header("Authorization",admin)).andExpect(status().isBadRequest());
  jdbc.update("UPDATE promotion_usages SET status='USED'");
  mvc.perform(get("/api/me/vouchers").header("Authorization",customer)).andExpect(jsonPath("$[0].status").value("USED"));
  jdbc.update("UPDATE promotion_usages SET status='RESERVED'");
  mvc.perform(post("/api/orders/"+code+"/cancel").header("Authorization",customer)).andExpect(status().isOk());
  mvc.perform(get("/api/me/vouchers").header("Authorization",customer)).andExpect(jsonPath("$[0].status").value("REVOKED"));
  grant(71,userId);
  mvc.perform(post("/api/checkout/details/preview").header("Authorization",customer).contentType("application/json").content(checkout())).andExpect(status().isBadRequest());
 }
 @Test void walletAndRecipientsEnforceRolesAndSearchEscapesWildcards() throws Exception {
  mvc.perform(get("/api/me/vouchers")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/me/vouchers").header("Authorization",staff)).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/promotions/71/recipients").header("Authorization",customer)).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/promotions/71/recipients").header("Authorization",customer).contentType("application/json").content("{\"userIds\":["+otherId+"]}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/promotions/71/recipients").header("Authorization",admin).param("assignedOnly","false").param("search",String.valueOf(otherId)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].email").value("wallet-other@example.com"));
  mvc.perform(get("/api/admin/promotions/71/recipients").header("Authorization",admin).param("assignedOnly","false").param("search","%"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
 }
 @Test void privateVouchersNeverAppearInPublicBannersAndNewApiVouchersDefaultPrivate() throws Exception {
  jdbc.update("INSERT INTO banners (promotion_id,title,image_url,is_active) VALUES (71,'Private','/private.png',true),(74,'Public','/public.png',true)");
  mvc.perform(get("/api/promotions")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].code").value("PUBLIC"));
  String body="""
   {"code":"NEWPRIVATE","name":"New private voucher","type":"percentage","value":10,"limit":10,"start":"%s","end":"%s","active":true,"bannerOrder":0,"bannerActive":false}
   """.formatted(LocalDateTime.now(clock).minusDays(1),LocalDateTime.now(clock).plusDays(1));
  mvc.perform(post("/api/admin/promotions").header("Authorization",admin).contentType("application/json").content(body))
   .andExpect(status().isOk()).andExpect(jsonPath("$.audience").value("ASSIGNED"));
 }
 @Test void grantRejectsWrongRolesInactiveUsersAndExpiredVouchersAtomically() throws Exception {
  int staffId=jdbc.queryForObject("SELECT id FROM users WHERE email='staff@techzone.demo'",Integer.class);
  mvc.perform(post("/api/admin/promotions/71/recipients").header("Authorization",admin).contentType("application/json").content("{\"userIds\":["+userId+","+staffId+"]}"))
   .andExpect(status().isBadRequest());
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM promotion_assignments",Integer.class));
  mvc.perform(post("/api/admin/promotions/72/recipients").header("Authorization",admin).contentType("application/json").content("{\"userIds\":["+userId+"]}"))
   .andExpect(status().isBadRequest());
  jdbc.update("UPDATE users SET is_active=false WHERE id=?",otherId);
  mvc.perform(post("/api/admin/promotions/71/recipients").header("Authorization",admin).contentType("application/json").content("{\"userIds\":["+otherId+"]}"))
   .andExpect(status().isBadRequest());
 }
}
