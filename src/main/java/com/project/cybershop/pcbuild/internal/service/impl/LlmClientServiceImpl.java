package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gọi Gemini generateContent với responseSchema. Key chỉ đi qua header x-goog-api-key, không nằm trong URL. */
@Slf4j
@Service
public class LlmClientServiceImpl implements LlmClientService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String modelName;
    private final int maxRetries;
    private final String thinkingLevel;

    public LlmClientServiceImpl(ObjectMapper objectMapper,
                                @Value("${app.ai.api-key:}") String apiKey,
                                @Value("${app.ai.model}") String modelName,
                                @Value("${app.ai.profile-timeout-ms:6000}") long timeoutMs,
                                @Value("${app.ai.max-retries:1}") int maxRetries,
                                @Value("${app.ai.profile-thinking-level:low}") String thinkingLevel) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.maxRetries = maxRetries;
        this.thinkingLevel = thinkingLevel;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public <T> T generateJson(String step, String systemPrompt, String userJson, Map<String, Object> responseSchema, Class<T> type) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Chưa cấu hình app.ai.api-key (biến môi trường AI_API_KEY), không gọi được AI bước {}", step);
            throw new AiUnavailableException("chưa cấu hình API key");
        }
        String lastError = "lỗi nhà cung cấp";
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            String text;
            try {
                text = postGemini(systemPrompt, userJson, responseSchema);
            } catch (ResourceAccessException e) {
                lastError = "hết giờ hoặc lỗi kết nối";
                log.warn("AI bước {} lần {}: {} (model {})", step, attempt, lastError, modelName);
                continue;
            } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
                lastError = "HTTP " + e.getStatusCode().value();
                log.warn("AI bước {} lần {}: {} (model {})", step, attempt, lastError, modelName);
                continue;
            } catch (HttpClientErrorException e) {
                // Lỗi request của ta (400, 401, 403…): thử lại cũng vô ích
                log.warn("AI bước {} bị từ chối: HTTP {} (model {})", step, e.getStatusCode().value(), modelName);
                throw new AiUnavailableException("yêu cầu bị từ chối (HTTP " + e.getStatusCode().value() + ")");
            }
            try {
                return objectMapper.readValue(stripCodeFence(text), type);
            } catch (Exception e) {
                lastError = "JSON trả về không đọc được";
                log.warn("AI bước {} lần {}: {}", step, attempt, lastError);
            }
        }
        throw new AiUnavailableException(lastError);
    }

    private String postGemini(String systemPrompt, String userJson, Map<String, Object> responseSchema) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema);
        generationConfig.put("thinkingConfig", Map.of("thinkingLevel", thinkingLevel));
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userJson)))),
                "generationConfig", generationConfig);
        String response = restClient.post()
                .uri(GEMINI_BASE_URL + modelName + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        return firstText(response);
    }

    // Phần text đầu tiên không phải "thought" của candidate đầu tiên
    private String firstText(String response) {
        try {
            for (JsonNode part : objectMapper.readTree(response).path("candidates").path(0).path("content").path("parts")) {
                if (part.hasNonNull("text") && !part.path("thought").asBoolean(false)) {
                    return part.path("text").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Không đọc được phản hồi Gemini");
        }
        return "";
    }

    private static String stripCodeFence(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }
        return trimmed;
    }
}
