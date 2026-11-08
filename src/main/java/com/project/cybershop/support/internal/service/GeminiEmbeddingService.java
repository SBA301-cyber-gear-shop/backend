package com.project.cybershop.support.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiEmbeddingService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    public GeminiEmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Sinh vector 768 chiều cho truy vấn tìm kiếm (Query Vector)
     * Sử dụng taskType: RETRIEVAL_QUERY để tối ưu hóa độ chính xác so khớp.
     */
    public String embedText(String text) {
        if (text == null || text.isBlank()) {
            return generateZeroVector(768);
        }

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + apiKey;
                Map<String, Object> body = Map.of(
                        "model", "models/gemini-embedding-001",
                        "content", Map.of("parts", List.of(Map.of("text", text))),
                        "taskType", "RETRIEVAL_QUERY",
                        "outputDimensionality", 768
                );

                String response = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(response);
                JsonNode valuesNode = root.path("embedding").path("values");
                if (valuesNode.isArray() && valuesNode.size() > 0) {
                    return formatValuesToVector(valuesNode);
                }
            } catch (Exception e) {
                log.warn("Lỗi khi gọi Gemini embedding cho truy vấn (dùng vector fallback): {}", e.getMessage());
            }
        }

        return generateHashVector(text, 768);
    }

    /**
     * Sinh vector 768 chiều cho một tài liệu lưu trữ vào cơ sở dữ liệu (Document Vector)
     * Sử dụng taskType: RETRIEVAL_DOCUMENT.
     */
    public String embedDocument(String title, String content) {
        String fullText = (title != null && !title.isBlank() ? title + "\n" : "") + (content != null ? content : "");
        if (fullText.isBlank()) {
            return generateZeroVector(768);
        }

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + apiKey;
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", "models/gemini-embedding-001");
                body.put("content", Map.of("parts", List.of(Map.of("text", fullText))));
                body.put("taskType", "RETRIEVAL_DOCUMENT");
                if (title != null && !title.isBlank()) {
                    body.put("title", title);
                }
                body.put("outputDimensionality", 768);

                String response = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(response);
                JsonNode valuesNode = root.path("embedding").path("values");
                if (valuesNode.isArray() && valuesNode.size() > 0) {
                    return formatValuesToVector(valuesNode);
                }
            } catch (Exception e) {
                log.warn("Lỗi khi gọi Gemini embedding cho tài liệu (dùng vector fallback): {}", e.getMessage());
            }
        }

        return generateHashVector(fullText, 768);
    }

    /**
     * Nhúng hàng loạt tài liệu sản phẩm theo lô (Batch Embedding)
     * Mỗi request xử lý tới 80 tài liệu, cho phép nhúng 255 sản phẩm chỉ trong ~4 HTTP requests
     * Tránh hoàn toàn lỗi 429 Too Many Requests của Google API.
     */
    public List<String> batchEmbedDocuments(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<String> results = new ArrayList<>(texts.size());
        final int BATCH_SIZE = 80;

        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            List<String> chunk = texts.subList(i, end);

            boolean success = false;
            if (apiKey != null && !apiKey.isBlank()) {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:batchEmbedContents?key=" + apiKey;
                List<Map<String, Object>> requestsList = new ArrayList<>();
                for (String t : chunk) {
                    requestsList.add(Map.of(
                            "model", "models/gemini-embedding-001",
                            "content", Map.of("parts", List.of(Map.of("text", t != null ? t : ""))),
                            "taskType", "RETRIEVAL_DOCUMENT",
                            "outputDimensionality", 768
                    ));
                }
                Map<String, Object> body = Map.of("requests", requestsList);

                try {
                    String response = restClient.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .retrieve()
                            .body(String.class);

                    JsonNode root = objectMapper.readTree(response);
                    JsonNode embeddingsNode = root.path("embeddings");
                    if (embeddingsNode.isArray() && embeddingsNode.size() == chunk.size()) {
                        for (int j = 0; j < embeddingsNode.size(); j++) {
                            JsonNode vals = embeddingsNode.get(j).path("values");
                            results.add(formatValuesToVector(vals));
                        }
                        success = true;
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("429")) {
                        log.warn("Gemini Embedding gặp 429 Rate Limit. Đang chờ 10s để thử lại chunk {} - {}...", i, end - 1);
                        try {
                            Thread.sleep(10000);
                            String retryResponse = restClient.post()
                                    .uri(url)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(body)
                                    .retrieve()
                                    .body(String.class);

                            JsonNode root = objectMapper.readTree(retryResponse);
                            JsonNode embeddingsNode = root.path("embeddings");
                            if (embeddingsNode.isArray() && embeddingsNode.size() == chunk.size()) {
                                for (int j = 0; j < embeddingsNode.size(); j++) {
                                    JsonNode vals = embeddingsNode.get(j).path("values");
                                    results.add(formatValuesToVector(vals));
                                }
                                success = true;
                            }
                        } catch (Exception retryEx) {
                            log.warn("Thử lại thất bại sau 429: {}. Dùng hash fallback cho lô này.", retryEx.getMessage());
                        }
                    } else {
                        log.warn("Lỗi khi gọi batchEmbedContents (chunk {} - {}): {}. Dùng hash fallback cho lô này.", i, end - 1, e.getMessage());
                    }
                }
            }

            if (!success) {
                // Fallback nếu không có API key hoặc batch call bị lỗi
                for (String t : chunk) {
                    results.add(generateHashVector(t != null ? t : "", 768));
                }
            }

            // Nghỉ 1.5s giữa các batch để tránh vượt quota 100 requests/phút của Free Tier
            if (i + BATCH_SIZE < texts.size()) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return results;
    }

    private String formatValuesToVector(JsonNode valuesNode) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < valuesNode.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(valuesNode.get(i).asDouble());
        }
        sb.append("]");
        return sb.toString();
    }

    private String generateZeroVector(int dims) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dims; i++) {
            if (i > 0) sb.append(",");
            sb.append("0.0");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Sinh HashVector dự phòng có bảo vệ chống Integer Overflow (BUG-03)
     * Sử dụng phép toán bit w.hashCode() & 0x7fffffff và ép kiểu long khi nhân 31L
     */
    private String generateHashVector(String text, int dims) {
        float[] vec = new float[dims];
        String[] words = text.toLowerCase().split("\\s+");
        for (String w : words) {
            int h = w.hashCode() & 0x7fffffff;
            int idx = h % dims;
            vec[idx] += 1.0f;
            int idx2 = (int) (((long) h * 31L) % dims);
            vec[idx2] += 0.5f;
        }

        // L2 Normalize
        float norm = 0f;
        for (float v : vec) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm < 1e-6) norm = 1f;

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dims; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(java.util.Locale.US, "%.5f", vec[i] / norm));
        }
        sb.append("]");
        return sb.toString();
    }
}
