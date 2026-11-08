package com.project.cybershop;

import com.project.cybershop.shared.config.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class ReviewMediaApiTests {
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate jdbc;
 @Autowired JwtUtil jwt;
 String token;
 byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a6kUAAAAASUVORK5CYII=");
 @BeforeEach void seed(){
  int user=jdbc.queryForObject("SELECT id FROM users WHERE email='customer@techzone.demo'",Integer.class);
  token="Bearer "+jwt.generateToken("customer@techzone.demo","CUSTOMER");
  jdbc.update("INSERT INTO brands VALUES (10,'Brand')");
  jdbc.update("INSERT INTO categories VALUES (10,'RAM','ram')");
  jdbc.update("INSERT INTO products VALUES (10,10,10,'RAM','active','RAM',36)");
  jdbc.update("INSERT INTO product_variants VALUES (101,10,'RAM16','16GB',1200000,1300000,3,'ram.png')");
  jdbc.update("INSERT INTO orders (id,user_id,order_code,total_amount,final_amount,status) VALUES (901,?,'REVIEW-MEDIA',1200000,1200000,'completed')",user);
  jdbc.update("INSERT INTO order_items (id,order_id,product_variant_id,warranty_period_months,quantity,unit_price,subtotal) VALUES (902,901,101,36,1,1200000,1200000)");
 }
 MockMultipartHttpServletRequestBuilder upload(String keep){
  var request=multipart("/api/order-items/902/review");
  request.with(req->{req.setMethod("PUT");return req;});
  request.file(new MockMultipartFile("payload","payload.json","application/json",("{\"review\":{\"rating\":4,\"comment\":\"Good product\"},\"retainedMediaIds\":"+keep+"}").getBytes(StandardCharsets.UTF_8)));
  request.header("Authorization",token);
  return request;
 }
 MockMultipartFile image(){return new MockMultipartFile("files","a.png","image/png",png);}
 MockMultipartFile video(){return new MockMultipartFile("files","v.mp4","video/mp4",new byte[]{0,0,0,12,102,116,121,112,105,115,111,109});}
 @Test void savesThreeImagesOneVideoAndReadsEditorAndPublicContent() throws Exception {
  mvc.perform(upload("[]").file(image()).file(image()).file(image()).file(video())).andExpect(status().isNoContent());
  mvc.perform(get("/api/order-items/902/review").header("Authorization",token)).andExpect(status().isOk())
   .andExpect(jsonPath("$.reviewed").value(true)).andExpect(jsonPath("$.rating").value(4))
   .andExpect(jsonPath("$.comment").value("Good product")).andExpect(jsonPath("$.media.length()").value(4));
  int review=jdbc.queryForObject("SELECT id FROM reviews WHERE order_item_id=902",Integer.class);
  int file=jdbc.queryForObject("SELECT min(id) FROM review_media",Integer.class);
  mvc.perform(get("/api/reviews/"+review+"/media")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
  mvc.perform(get("/api/review-media/"+file)).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(png));
  int videoId=jdbc.queryForObject("SELECT max(id) FROM review_media",Integer.class);
  mvc.perform(get("/api/review-media/"+videoId).header("Range","bytes=0-3")).andExpect(status().isPartialContent())
   .andExpect(content().contentType("video/mp4")).andExpect(header().string("Content-Range","bytes 0-3/12"));
  mvc.perform(get("/api/order-items/902/review")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/order-items/902/review").header("Authorization","Bearer "+jwt.generateToken("staff@techzone.demo","STAFF"))).andExpect(status().isNotFound());
 }
 @Test void rejectsExcessImagesOrVideosWithoutSavingReview() throws Exception {
  mvc.perform(upload("[]").file(image()).file(image()).file(image()).file(image())).andExpect(status().isBadRequest());
  mvc.perform(upload("[]").file(video()).file(video())).andExpect(status().isBadRequest());
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM reviews",Integer.class));
 }
 @Test void rejectsSpoofedAndOversizedFiles() throws Exception {
  mvc.perform(upload("[]").file(new MockMultipartFile("files","a.png","image/png",new byte[]{1,2,3}))).andExpect(status().isBadRequest());
  mvc.perform(upload("[]").file(new MockMultipartFile("files","large.png","image/png",new byte[5*1024*1024+1]))).andExpect(status().isBadRequest());
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM reviews",Integer.class));
 }
 @Test void rejectsPendingAndOtherUsersAndUnauthenticatedUploads() throws Exception {
  var unauthenticated=upload("[]");
  unauthenticated.with(req->{req.removeHeader("Authorization");return req;});
  mvc.perform(unauthenticated).andExpect(status().isUnauthorized());
  String foreign="Bearer "+jwt.generateToken("staff@techzone.demo","STAFF");
  mvc.perform(get("/api/order-items/902/review").header("Authorization",foreign)).andExpect(status().isNotFound());
  jdbc.update("UPDATE orders SET status='ready_for_pickup' WHERE id=901");
  mvc.perform(upload("[]").file(image())).andExpect(status().isBadRequest());
 }
 @Test void repeatReviewsCannotChangeTextOrAttachments() throws Exception {
  mvc.perform(upload("[]").file(image()).file(image())).andExpect(status().isNoContent());
  int keep=jdbc.queryForObject("SELECT min(id) FROM review_media",Integer.class);
  int removed=jdbc.queryForObject("SELECT max(id) FROM review_media",Integer.class);
  mvc.perform(upload("[999999]").file(image())).andExpect(status().isBadRequest());
  assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM review_media",Integer.class));
  mvc.perform(upload("["+keep+"]").file(video())).andExpect(status().isBadRequest());
  assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM review_media",Integer.class));
  mvc.perform(get("/api/review-media/"+removed)).andExpect(status().isOk());
  mvc.perform(put("/api/order-items/902/review").header("Authorization",token).contentType("application/json").content("{\"rating\":1,\"comment\":\"Changed\"}")).andExpect(status().isBadRequest());
  assertEquals("Good product",jdbc.queryForObject("SELECT comment FROM reviews WHERE order_item_id=902",String.class));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM reviews",Integer.class));
 }
}
