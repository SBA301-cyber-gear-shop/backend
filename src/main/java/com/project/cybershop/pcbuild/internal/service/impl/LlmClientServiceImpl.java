package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import com.project.cybershop.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClientServiceImpl implements LlmClientService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().build();

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.provider}")
    private String provider;

    @Value("${app.ai.model}")
    private String modelName;

    @Override
    public AiDraftProposal generateBuildProposal(String systemPrompt, String userContextJson) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Chưa cấu hình app.ai.api-key!");
            throw new BusinessException("Chưa cấu hình API Key cho dịch vụ AI. Vui lòng liên hệ quản trị viên.");
        }

        try {
            if ("gemini".equalsIgnoreCase(provider)) {
                return callGeminiApi(systemPrompt, userContextJson);
            } else {
                return callOpenAiApi(systemPrompt, userContextJson);
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi API AI bên ngoài: {}", e.getMessage(), e);
            throw new BusinessException("Dịch vụ AI hiện không khả dụng. Vui lòng thử lại sau!");
        }
    }

    private AiDraftProposal callGeminiApi(String systemPrompt, String userContextJson) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key="
                + apiKey;

        Map<String, Object> partSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "role", Map.of("type", "STRING"),
                        "variantId", Map.of("type", "INTEGER"),
                        "quantity", Map.of("type", "INTEGER"),
                        "reason", Map.of("type", "STRING")
                ),
                "required", List.of("role", "variantId", "quantity", "reason")
        );

        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "buildName", Map.of("type", "STRING"),
                        "aiReasoning", Map.of("type", "STRING"),
                        "performanceHighlight", Map.of("type", "STRING"),
                        "selectedParts", Map.of(
                                "type", "ARRAY",
                                "items", partSchema
                        )
                ),
                "required", List.of("buildName", "aiReasoning", "performanceHighlight", "selectedParts")
        );

        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userContextJson)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema,
                        "temperature", 0.2));

        String response = restClient.post()
                .uri(url)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String textJson = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText().trim();

        if (textJson.startsWith("```")) {
            textJson = textJson.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
        }

        return objectMapper.readValue(textJson, AiDraftProposal.class);
    }

    private AiDraftProposal callOpenAiApi(String systemPrompt, String userContextJson) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContextJson)),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.2);

        String response = restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        return objectMapper.readValue(content, AiDraftProposal.class);
    }

    private AiDraftProposal createMockAiProposal() {
        AiDraftProposal proposal = new AiDraftProposal();
        proposal.setBuildName("Bạch Hổ Gaming DDR5 - i5 13400F & RTX 4060");
        proposal.setAiReasoning(
                "Cấu hình được tối ưu hoàn hảo cho trải nghiệm gaming và sáng tạo nội dung. Bộ vi xử lý Intel i5 thế hệ 13 phối hợp cùng card RTX 4060 8GB DLSS 3 chiến mượt mà mọi tựa game AAA. Vỏ case màu trắng Montech 4 quạt ARGB và tản nhiệt tháp đảm bảo luồng khí mát mẻ, êm ái.");
        proposal.setPerformanceHighlight(
                "Chiến Black Myth Wukong 75+ FPS High Settings, CS2 240+ FPS, Render Premiere 4K mượt mà.");
        proposal.setSelectedParts(List.of(
                createPart("cpu", 101, 1, "CPU 10 nhân đa nhiệm xuất sắc"),
                createPart("mainboard", 102, 1, "Bo mạch chủ DDR5 có Wi-Fi tích hợp"),
                createPart("ram", 103, 2, "RAM DDR5 32GB (2x16GB) đa nhiệm thả ga"),
                createPart("gpu", 104, 1, "Card RTX 4060 8GB mát mẻ tiết kiệm điện"),
                createPart("ssd", 105, 1, "SSD NVMe 1TB tốc độ đọc 3500MB/s"),
                createPart("psu", 106, 1, "Nguồn 650W chuẩn Bronze dư tải an toàn"),
                createPart("case", 107, 1, "Vỏ case trắng đối lưu không khí cực tốt"),
                createPart("cooler", 108, 1, "Tản nhiệt khí màu trắng đồng bộ tone máy")));
        return proposal;
    }

    private AiDraftProposal.PartSelection createPart(String role, int variantId, int qty, String reason) {
        AiDraftProposal.PartSelection part = new AiDraftProposal.PartSelection();
        part.setRole(role);
        part.setVariantId(variantId);
        part.setQuantity(qty);
        part.setReason(reason);
        return part;
    }
}
