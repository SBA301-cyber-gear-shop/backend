package com.project.cybershop.aiconfig.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.aiconfig.api.AiConfigPublicApi;
import com.project.cybershop.aiconfig.api.dto.AiConfigDto;
import com.project.cybershop.aiconfig.internal.dto.*;
import com.project.cybershop.aiconfig.internal.entity.AiConfigEntity;
import com.project.cybershop.aiconfig.internal.repository.AiConfigRepository;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigPublicApi {

    private final AiConfigRepository aiConfigRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.api-key:}")
    private String defaultApiKey;

    private final Map<String, AiConfigDto> activeConfigCache = new ConcurrentHashMap<>();

    // =========================================================================
    // IMPLEMENTATION CỦA AiConfigPublicApi (DÀNH CHO PCBUILD, SUPPORT GỌI ĐỘNG)
    // =========================================================================

    @Override
    public Optional<AiConfigDto> findActiveConfig(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            return Optional.empty();
        }
        AiConfigDto cached = activeConfigCache.get(configKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<AiConfigEntity> entityOpt = aiConfigRepository.findByConfigKeyAndIsActiveTrue(configKey);
        if (entityOpt.isPresent()) {
            AiConfigDto dto = toDto(entityOpt.get());
            activeConfigCache.put(configKey, dto);
            return Optional.of(dto);
        }
        return Optional.empty();
    }

    @Override
    public AiConfigDto getActiveConfig(String configKey) {
        return findActiveConfig(configKey).orElse(null);
    }

    @Override
    public String getActivePrompt(String configKey, String defaultPrompt) {
        AiConfigDto config = getActiveConfig(configKey);
        if (config != null && config.getSystemPrompt() != null && !config.getSystemPrompt().isBlank()) {
            return config.getSystemPrompt();
        }
        return defaultPrompt;
    }

    // =========================================================================
    // CÁC HÀM CRUD & QUẢN TRỊ DÀNH CHO ADMIN
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AiConfigDto> getAllConfigs() {
        return aiConfigRepository.findAllByOrderByConfigKeyAscIdAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiConfigDto getConfigById(Integer id) {
        AiConfigEntity entity = aiConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình AI với ID: " + id));
        return toDto(entity);
    }

    @Transactional
    public AiConfigDto createConfig(AiConfigCreateRequest request) {
        boolean hasActive = aiConfigRepository.countByConfigKeyAndIsActiveTrue(request.getConfigKey()) > 0;
        boolean willBeActive = Boolean.TRUE.equals(request.getIsActive()) || !hasActive;

        AiConfigEntity entity = AiConfigEntity.builder()
                .configKey(request.getConfigKey().trim())
                .configName(request.getConfigName().trim())
                .provider(request.getProvider().trim())
                .modelName(request.getModelName().trim())
                .apiKey(request.getApiKey().trim())
                .systemPrompt(request.getSystemPrompt() != null ? request.getSystemPrompt().trim() : null)
                .isActive(willBeActive)
                .build();

        AiConfigEntity saved = aiConfigRepository.save(entity);

        if (willBeActive) {
            aiConfigRepository.deactivateOthersInKey(saved.getConfigKey(), saved.getId());
        }

        evictCache(saved.getConfigKey());
        return toDto(saved);
    }

    @Transactional
    public AiConfigDto updateConfig(Integer id, AiConfigUpdateRequest request) {
        AiConfigEntity entity = aiConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình AI với ID: " + id));

        String oldKey = entity.getConfigKey();
        boolean currentlyActive = Boolean.TRUE.equals(entity.getIsActive());
        boolean requestedActive = Boolean.TRUE.equals(request.getIsActive());

        // Quy tắc: Nếu đang active mà cố tình tắt trực tiếp mà không có thằng thay thế
        if (currentlyActive && !requestedActive) {
            throw new BusinessException(
                    "Không thể tắt cấu hình đang hoạt động của khóa '" + entity.getConfigKey() +
                            "'. Hệ thống bắt buộc phải có 1 cấu hình hoạt động. Vui lòng kích hoạt một cấu hình thay thế khác hoặc tạo mới và kích hoạt cấu hình đó!");
        }

        entity.setConfigKey(request.getConfigKey().trim());
        entity.setConfigName(request.getConfigName().trim());
        entity.setProvider(request.getProvider().trim());
        entity.setModelName(request.getModelName().trim());
        entity.setApiKey(request.getApiKey().trim());
        entity.setSystemPrompt(request.getSystemPrompt() != null ? request.getSystemPrompt().trim() : null);

        if (requestedActive && !currentlyActive) {
            entity.setIsActive(true);
            aiConfigRepository.deactivateOthersInKey(entity.getConfigKey(), entity.getId());
        }

        AiConfigEntity updated = aiConfigRepository.save(entity);
        evictCache(oldKey);
        evictCache(updated.getConfigKey());
        return toDto(updated);
    }

    @Transactional
    public AiConfigDto toggleActive(Integer id, Boolean targetStatus) {
        AiConfigEntity entity = aiConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình AI với ID: " + id));

        boolean newStatus = targetStatus != null ? targetStatus : !Boolean.TRUE.equals(entity.getIsActive());

        if (!newStatus && Boolean.TRUE.equals(entity.getIsActive())) {
            // Đang active mà cố tình tắt đi trực tiếp
            throw new BusinessException(
                    "Không thể tắt cấu hình đang hoạt động của khóa '" + entity.getConfigKey() +
                            "'. Hệ thống bắt buộc phải có 1 cấu hình hoạt động. Vui lòng kích hoạt một cấu hình thay thế khác hoặc tạo mới và kích hoạt cấu hình đó!");
        }

        if (newStatus) {
            entity.setIsActive(true);
            aiConfigRepository.deactivateOthersInKey(entity.getConfigKey(), entity.getId());
        }

        AiConfigEntity updated = aiConfigRepository.save(entity);
        evictCache(updated.getConfigKey());
        return toDto(updated);
    }

    @Transactional
    public void deleteConfig(Integer id) {
        AiConfigEntity entity = aiConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình AI với ID: " + id));

        if (Boolean.TRUE.equals(entity.getIsActive())) {
            throw new BusinessException(
                    "Không thể xóa cấu hình đang hoạt động ('" + entity.getConfigName() +
                            "'). Vui lòng kích hoạt một cấu hình khác thay thế trước khi xóa!");
        }

        aiConfigRepository.delete(entity);
        evictCache(entity.getConfigKey());
    }

    public AiTestPromptResponse testPrompt(AiTestPromptRequest request) {
        long startTime = System.currentTimeMillis();
        String effectiveKey = request.getApiKey() != null && !request.getApiKey().isBlank() && !request.getApiKey().startsWith("AI_API_KEY_PLACEHOLDER")
                ? request.getApiKey().trim()
                : defaultApiKey;

        String model = request.getModelName() != null && !request.getModelName().isBlank()
                ? request.getModelName().trim()
                : "gemini-2.5-flash";

        // Thử gọi trực tiếp Gemini API nếu có key thực
        if (effectiveKey != null && !effectiveKey.isBlank() && !"GEMINI".equalsIgnoreCase(request.getProvider()) || "GEMINI".equalsIgnoreCase(request.getProvider())) {
            try {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(Duration.ofSeconds(6));
                factory.setReadTimeout(Duration.ofSeconds(15));
                RestClient restClient = RestClient.builder().requestFactory(factory).build();

                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + effectiveKey;

                Map<String, Object> body = Map.of(
                        "systemInstruction", Map.of("parts", List.of(Map.of("text", request.getSystemPrompt() != null ? request.getSystemPrompt() : ""))),
                        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", request.getUserMessage())))),
                        "generationConfig", Map.of("temperature", 0.3)
                );

                String response = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                long latency = System.currentTimeMillis() - startTime;
                JsonNode root = objectMapper.readTree(response);
                String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText().trim();

                int promptTokens = Math.max(10, (request.getUserMessage().length() + (request.getSystemPrompt() != null ? request.getSystemPrompt().length() : 0)) / 4);
                int completionTokens = Math.max(5, text.length() / 4);

                return AiTestPromptResponse.builder()
                        .output(text)
                        .latencyMs(latency)
                        .promptTokens(promptTokens)
                        .completionTokens(completionTokens)
                        .simulated(false)
                        .build();

            } catch (Exception e) {
                log.warn("Gọi Sandbox Test AI thất bại: {}. Sử dụng phản hồi mô phỏng dự phòng.", e.getMessage());
            }
        }

        // Mô phỏng nếu không gọi được mạng
        long latency = System.currentTimeMillis() - startTime + 420;
        String simulatedOutput = "Dạ, CyberShop đã tiếp nhận yêu cầu với cấu hình '" + (request.getModelName() != null ? request.getModelName() : "AI") +
                "'. Phản hồi mẫu cho câu hỏi: \"" + request.getUserMessage() + "\". Hệ thống hoạt động bình thường.";

        return AiTestPromptResponse.builder()
                .output(simulatedOutput)
                .latencyMs(latency)
                .promptTokens(request.getUserMessage().length() / 4 + 40)
                .completionTokens(simulatedOutput.length() / 4)
                .simulated(true)
                .build();
    }

    private void evictCache(String configKey) {
        if (configKey != null) {
            activeConfigCache.remove(configKey);
        }
    }

    private AiConfigDto toDto(AiConfigEntity entity) {
        return AiConfigDto.builder()
                .id(entity.getId())
                .configKey(entity.getConfigKey())
                .configName(entity.getConfigName())
                .provider(entity.getProvider())
                .modelName(entity.getModelName())
                .apiKey(entity.getApiKey())
                .systemPrompt(entity.getSystemPrompt())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

