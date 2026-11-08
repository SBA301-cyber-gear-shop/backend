package com.project.cybershop.payment.internal.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public final class VnPayUtil {

    private VnPayUtil() {}

    public static String hmacSHA512(String key, String data) {
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512Hmac.init(secretKey);
            byte[] hash = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi tạo mã băm HMAC-SHA512 cho VNPay", e);
        }
    }

    public static String buildPaymentUrl(String vnpUrl, Map<String, String> params, String secretKey) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder signDataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                signDataBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                signDataBuilder.append("=");
                signDataBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                signDataBuilder.append("&");
            }
        }
        if (signDataBuilder.length() > 0) {
            signDataBuilder.deleteCharAt(signDataBuilder.length() - 1);
        }

        String signData = signDataBuilder.toString();
        String secureHash = hmacSHA512(secretKey, signData);
        sortedParams.put("vnp_SecureHash", secureHash);

        StringBuilder urlBuilder = new StringBuilder(vnpUrl);
        urlBuilder.append("?");
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                urlBuilder.append("=");
                urlBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                urlBuilder.append("&");
            }
        }
        if (urlBuilder.charAt(urlBuilder.length() - 1) == '&' || urlBuilder.charAt(urlBuilder.length() - 1) == '?') {
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }
        return urlBuilder.toString();
    }

    public static boolean verifySignature(Map<String, String> fields, String secretKey) {
        String vnpSecureHash = fields.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isBlank()) {
            return false;
        }

        Map<String, String> sortedFields = new TreeMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equalsIgnoreCase(key)
                    && !"vnp_SecureHashType".equalsIgnoreCase(key)
                    && value != null && !value.isBlank()) {
                sortedFields.put(key, value);
            }
        }

        StringBuilder signDataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedFields.entrySet()) {
            signDataBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            signDataBuilder.append("=");
            signDataBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            signDataBuilder.append("&");
        }
        if (signDataBuilder.length() > 0) {
            signDataBuilder.deleteCharAt(signDataBuilder.length() - 1);
        }

        String calculatedHash = hmacSHA512(secretKey, signDataBuilder.toString());
        return calculatedHash.equalsIgnoreCase(vnpSecureHash);
    }
}
