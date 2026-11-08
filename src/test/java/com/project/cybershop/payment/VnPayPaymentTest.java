package com.project.cybershop.payment;

import com.project.cybershop.payment.internal.config.VnPayConfig;
import com.project.cybershop.payment.internal.service.impl.PaymentServiceImpl;
import com.project.cybershop.payment.internal.util.VnPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

class VnPayPaymentTest {

    private final String testSecretKey = "WVWWFTHJALERWKHEOBBIHVXHVRTUYJRL";
    private final String testTmnCode = "3RBH6UL5";
    private final String testPayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private final String testReturnUrl = "http://localhost:5173/thanh-toan/ket-qua";

    private VnPayConfig vnPayConfig;
    private JdbcTemplate jdbcTemplate;
    private Clock clock;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        vnPayConfig = new VnPayConfig();
        ReflectionTestUtils.setField(vnPayConfig, "tmnCode", testTmnCode);
        ReflectionTestUtils.setField(vnPayConfig, "hashSecret", testSecretKey);
        ReflectionTestUtils.setField(vnPayConfig, "payUrl", testPayUrl);
        ReflectionTestUtils.setField(vnPayConfig, "returnUrl", testReturnUrl);

        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        clock = Clock.fixed(Instant.parse("2026-11-05T10:15:30Z"), ZoneId.of("Asia/Bangkok"));
        paymentService = new PaymentServiceImpl(jdbcTemplate, clock, vnPayConfig);
    }

    @Test
    void testHmacSHA512Generation() {
        String data = "vnp_Amount=10000000&vnp_Command=pay&vnp_TmnCode=3RBH6UL5";
        String hash = VnPayUtil.hmacSHA512(testSecretKey, data);
        assertNotNull(hash);
        assertEquals(128, hash.length(), "HMAC-SHA512 hex string should be 128 characters");
    }

    @Test
    void testBuildPaymentUrlAndVerifySignature() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", testTmnCode);
        params.put("vnp_Amount", "1500000000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", "TZ-20261105-TEST1");
        params.put("vnp_OrderInfo", "Thanh toan don hang TZ-20261105-TEST1 tai CyberShop");
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", testReturnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", "20261105161530");

        String payUrl = VnPayUtil.buildPaymentUrl(testPayUrl, params, testSecretKey);
        assertNotNull(payUrl);
        assertTrue(payUrl.startsWith(testPayUrl + "?"));
        assertTrue(payUrl.contains("vnp_SecureHash="));

        // Parse query params back
        URI uri = URI.create(payUrl);
        String query = uri.getRawQuery();
        Map<String, String> extractedParams = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String val = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            extractedParams.put(key, val);
        }

        assertTrue(VnPayUtil.verifySignature(extractedParams, testSecretKey), "Signature verification should succeed");
    }

    @Test
    void testTamperedSignatureFails() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TZ-20261105-TEST2");
        params.put("vnp_Amount", "1000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");

        String payUrl = VnPayUtil.buildPaymentUrl(testPayUrl, params, testSecretKey);
        URI uri = URI.create(payUrl);
        Map<String, String> extractedParams = new HashMap<>();
        for (String pair : uri.getRawQuery().split("&")) {
            String[] parts = pair.split("=");
            extractedParams.put(parts[0], parts.length > 1 ? parts[1] : "");
        }

        // Tamper amount
        extractedParams.put("vnp_Amount", "9999999");
        assertFalse(VnPayUtil.verifySignature(extractedParams, testSecretKey), "Tampered parameter must fail signature verification");
    }

    @Test
    void testCreateVnPayPaymentService() {
        Integer orderId = 101;
        String orderCode = "TZ-20261105-9999";
        BigDecimal amount = BigDecimal.valueOf(2500000);

        String url = paymentService.createVnPayPayment(orderId, orderCode, amount, "127.0.0.1");

        assertNotNull(url);
        assertTrue(url.contains("vnp_TxnRef=TZ-20261105-9999"));
        assertTrue(url.contains("vnp_Amount=250000000")); // 2.500.000 * 100
        verify(jdbcTemplate).update(anyString(), eq(orderId), eq(amount), any());
    }
}
