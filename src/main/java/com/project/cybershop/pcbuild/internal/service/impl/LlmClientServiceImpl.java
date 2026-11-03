package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import com.project.cybershop.pcbuild.internal.support.AiCircuitBreaker;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClientServiceImpl implements LlmClientService {

    private final ObjectMapper objectMapper;
    private final AiCircuitBreaker circuitBreaker;
    private final Clock clock;

    private HttpClient httpClient;

    // Không final để test trỏ sang server cục bộ
    private String geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/models/";
    private String openAiUrl = "https://api.openai.com/v1/chat/completions";

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.provider}")
    private String provider;

    @Value("${app.ai.model}")
    private String modelName;

    @Value("${app.ai.timeout-ms}")
    private long timeoutMs;

    @Value("${app.ai.profile-timeout-ms:6000}")
    private long profileTimeoutMs;

    @Value("${app.ai.max-retries:1}")
    private int maxRetries;

    @Value("${app.ai.min-call-ms:2000}")
    private long minCallMs;

    @Value("${app.ai.profile-thinking-level:low}")
    private String profileThinkingLevel;

    @Value("${app.ai.explain-thinking-level:minimal}")
    private String explainThinkingLevel;

    @PostConstruct
    void init() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    private RestClient clientWithTimeout(long readTimeoutMs) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public RequirementProfileDto extractProfile(String systemPrompt, String userJson, Instant deadline) {
        return callJson("profile", systemPrompt, userJson, profileSchema(), profileThinkingLevel, profileTimeoutMs, deadline,
                RequirementProfileDto.class);
    }

    @Override
    public ExplanationDraft explainBuild(String systemPrompt, String userJson, Instant deadline) {
        return callJson("explain", systemPrompt, userJson, explanationSchema(), explainThinkingLevel, timeoutMs, deadline,
                ExplanationDraft.class);
    }

    /**
     * Gọi nhà cung cấp, thử lại tối đa max-retries lần khi timeout, lỗi kết nối, 5xx, 429 hoặc JSON hỏng, và chỉ khi
     * còn ít nhất min-call-ms tới deadline (DD 8.5.1). Timeout mỗi lần = min(timeout của bước, thời gian còn lại).
     */
    private <T> T callJson(String step, String systemPrompt, String userJson, Map<String, Object> schema, String thinkingLevel,
                           long stepTimeoutMs, Instant deadline, Class<T> type) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Chưa cấu hình app.ai.api-key, không gọi được AI bước {}", step);
            throw new AiUnavailableException(AiUnavailableException.Detail.PROVIDER_ERROR);
        }
        AiUnavailableException.Detail lastError = AiUnavailableException.Detail.PROVIDER_ERROR;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            long remainingMs = Duration.between(clock.instant(), deadline).toMillis();
            if (remainingMs < minCallMs) {
                log.warn("AI bước {}: còn {} ms tới deadline, không gọi thêm", step, remainingMs);
                throw new AiUnavailableException(AiUnavailableException.Detail.DEADLINE_EXCEEDED);
            }
            circuitBreaker.beforeCall();
            String text;
            try {
                text = "gemini".equalsIgnoreCase(provider)
                        ? postGemini(systemPrompt, userJson, schema, thinkingLevel, Math.min(stepTimeoutMs, remainingMs))
                        : postOpenAi(systemPrompt, userJson, Math.min(stepTimeoutMs, remainingMs));
                circuitBreaker.onSuccess();
            } catch (ResourceAccessException e) {
                lastError = isTimeout(e) ? AiUnavailableException.Detail.TIMEOUT : AiUnavailableException.Detail.PROVIDER_ERROR;
                circuitBreaker.onProviderFailure(lastError.name());
                log.warn("AI bước {} lần {} lỗi: provider={}, model={}, loại={}", step, attempt + 1, provider, modelName, lastError);
                continue;
            } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
                lastError = AiUnavailableException.Detail.PROVIDER_ERROR;
                circuitBreaker.onProviderFailure("HTTP " + e.getStatusCode().value());
                log.warn("AI bước {} lần {} lỗi: provider={}, model={}, HTTP {}", step, attempt + 1, provider, modelName, e.getStatusCode().value());
                continue;
            } catch (HttpClientErrorException e) {
                // Lỗi request của ta (400…): không thử lại, không tính vào ngắt mạch
                circuitBreaker.onNeutralOutcome();
                log.warn("AI bước {} bị từ chối: provider={}, model={}, HTTP {}", step, provider, modelName, e.getStatusCode().value());
                throw new AiUnavailableException(AiUnavailableException.Detail.PROVIDER_ERROR);
            }
            try {
                return objectMapper.readValue(stripCodeFence(text), type);
            } catch (Exception e) {
                lastError = AiUnavailableException.Detail.INVALID_RESPONSE;
                log.warn("AI bước {} lần {} trả JSON không đọc được (không log nội dung)", step, attempt + 1);
            }
        }
        throw new AiUnavailableException(lastError);
    }

    private String postGemini(String systemPrompt, String userJson, Map<String, Object> schema, String thinkingLevel,
                              long readTimeoutMs) throws ResourceAccessException {
        // Key chỉ đi qua header x-goog-api-key, không nằm trong URL. Không gửi temperature/topP/topK (DD 8.2)
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", schema);
        generationConfig.put("thinkingConfig", Map.of("thinkingLevel", thinkingLevel));
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userJson)))),
                "generationConfig", generationConfig);
        String response = clientWithTimeout(readTimeoutMs).post()
                .uri(geminiBaseUrl + modelName + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        return geminiText(response);
    }

    private String postOpenAi(String systemPrompt, String userJson, long readTimeoutMs) {
        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", List.of(Map.of("role", "system", "content", systemPrompt), Map.of("role", "user", "content", userJson)),
                "response_format", Map.of("type", "json_object"));
        String response = clientWithTimeout(readTimeoutMs).post()
                .uri(openAiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(response).path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "";
        }
    }

    // Lấy phần text đầu tiên không phải "thought" của candidate đầu tiên
    private String geminiText(String response) {
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

    private static boolean isTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof java.net.SocketTimeoutException) return true;
        }
        return false;
    }

    private static String stripCodeFence(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }
        return trimmed;
    }

    // ---------- Schema (DD 8.3, 8.4) ----------

    private static Map<String, Object> type(String type) {
        return Map.of("type", type);
    }

    private static Map<String, Object> nullable(String type) {
        return Map.of("type", type, "nullable", true);
    }

    private static Map<String, Object> array(Map<String, Object> items) {
        return Map.of("type", "ARRAY", "items", items);
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        return schema;
    }

    static Map<String, Object> profileSchema() {
        // Bắt buộc đủ 8 khóa: khóa không bắt buộc thì Gemini bỏ gpu, psu, case, cooler, và thiếu khóa nghĩa là trọng số 0
        List<String> roles = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");
        Map<String, Object> roleWeights = new LinkedHashMap<>();
        for (String role : roles) {
            roleWeights.put(role, type("NUMBER"));
        }
        Map<String, Object> constraint = new LinkedHashMap<>();
        constraint.put("role", type("STRING"));
        constraint.put("spec", type("STRING"));
        constraint.put("op", Map.of("type", "STRING", "enum", List.of("GTE", "LTE", "EQ", "IN", "NOT_IN", "CONTAINS")));
        constraint.put("value", nullable("NUMBER"));
        constraint.put("values", array(type("STRING")));
        constraint.put("priority", type("INTEGER"));
        constraint.put("reason", type("STRING"));
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("metric", type("STRING"));
        metric.put("op", Map.of("type", "STRING", "enum", List.of("GTE", "LTE", "EQ")));
        metric.put("value", type("NUMBER"));
        metric.put("priority", type("INTEGER"));
        metric.put("reason", type("STRING"));
        Map<String, Object> preference = new LinkedHashMap<>();
        preference.put("role", nullable("STRING"));
        preference.put("spec", nullable("STRING"));
        preference.put("metric", nullable("STRING"));
        preference.put("direction", Map.of("type", "STRING", "enum", List.of("MAX", "MIN", "PREFER")));
        preference.put("values", array(type("STRING")));
        preference.put("weight", type("NUMBER"));
        preference.put("cap", nullable("NUMBER"));
        preference.put("reason", type("STRING"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("summary", type("STRING"));
        properties.put("archetype", Map.of("type", "STRING", "enum", List.of("GAMING", "CREATIVE", "AI_DATA", "OFFICE", "DEV", "GENERAL")));
        properties.put("useCases", array(type("STRING")));
        properties.put("roleWeights", object(roleWeights, roles));
        properties.put("requiredRoles", array(object(Map.of("role", type("STRING"), "priority", type("INTEGER"), "reason", type("STRING")),
                List.of("role", "priority"))));
        properties.put("excludedRoles", array(object(Map.of("role", type("STRING"), "reason", type("STRING")), List.of("role"))));
        properties.put("constraints", array(object(constraint, List.of("role", "spec", "op", "priority", "reason"))));
        properties.put("metrics", array(object(metric, List.of("metric", "op", "value", "priority", "reason"))));
        properties.put("preferences", array(object(preference, List.of("direction", "weight"))));
        properties.put("balancePairs", array(object(Map.of("a", type("STRING"), "b", type("STRING"), "maxGap", type("NUMBER")), List.of())));
        properties.put("brands", object(Map.of("prefer", array(type("STRING")), "avoid", array(type("STRING"))), List.of()));
        properties.put("unmetNeeds", array(type("STRING")));
        properties.put("objective", Map.of("type", "STRING", "enum", List.of("MAX_PRICE"), "nullable", true));
        properties.put("confidence", type("NUMBER"));
        properties.put("clarifyingQuestion", nullable("STRING"));
        return object(properties, List.of("summary", "archetype", "roleWeights", "constraints", "metrics", "preferences", "confidence"));
    }

    static Map<String, Object> explanationSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("buildName", type("STRING"));
        properties.put("aiReasoning", type("STRING"));
        properties.put("performanceHighlight", type("STRING"));
        properties.put("partReasons", array(object(Map.of("variantId", type("INTEGER"), "reason", type("STRING")),
                List.of("variantId", "reason"))));
        return object(properties, List.of("buildName", "aiReasoning", "performanceHighlight", "partReasons"));
    }
}
