package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.SolverResult;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.BuildExplainer;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildExplainerImpl implements BuildExplainer {

    static final String PROMPT_EXPLAIN_V3 = """
            BẠN LÀ KỸ SƯ TƯ VẤN PC CỦA CYBERSHOP.
            Hệ thống đã hiểu nhu cầu của khách thành "profile" và đã chọn sẵn MỘT bộ máy.
            Bộ máy ĐÃ được kiểm tra tương thích, giá là giá thật và tổng giá KHÔNG vượt ngân sách.
            NHIỆM VỤ: Viết buildName (≤ 60 ký tự), aiReasoning (3–6 câu), performanceHighlight (1–2 câu),
            và lý do ngắn cho TỪNG variantId trong bộ máy, liên hệ với các "reason" trong profile.
            QUY TẮC:
            - KHÔNG đề xuất thay, thêm hay bớt linh kiện. KHÔNG nhắc tới linh kiện, variantId, giá hoặc thông số không có trong dữ liệu được cấp.
            - KHÔNG hứa hẹn số FPS cụ thể. Chỉ mô tả mức trải nghiệm (ví dụ "chơi tốt ở 2K thiết lập cao").
            - PHẢI nhắc lại trung thực: tổng giá so với ngân sách và số tiền còn dư (leftover), các mục đã hạ (relaxedItems),
              các nhu cầu chưa đáp ứng (unmetNeeds), reasonCode (ví dụ nhu cầu đã bão hòa nên còn dư tiền), và warning về giá tham khảo.
            - Trả về DUY NHẤT JSON theo schema.
            """;

    private static final int MAX_BUILD_NAME = 60;

    private final LlmClientService llmClientService;
    private final ObjectMapper objectMapper;

    @Override
    public Explanation explain(ValidatedProfile profile, SolverResult result, BigDecimal budget, String purpose, String userPrompt,
                               CompatibilitySnapshot snapshot, Instant deadline) {
        LlmClientService.ExplanationDraft draft = llmClientService.explainBuild(PROMPT_EXPLAIN_V3,
                context(profile, result, budget, purpose, userPrompt, snapshot), deadline);
        if (draft == null || isBlank(draft.getBuildName()) || isBlank(draft.getAiReasoning())) {
            throw new AiUnavailableException(AiUnavailableException.Detail.INVALID_RESPONSE);
        }

        Set<Integer> buildVariantIds = result.lines().stream().map(line -> line.part().getVariantId()).collect(Collectors.toSet());
        Map<Integer, String> reasons = new LinkedHashMap<>();
        boolean partial = false;
        for (LlmClientService.ExplanationDraft.PartReason partReason : draft.getPartReasons()) {
            // Tuyệt đối không dùng variantId do AI đưa ra để đổi linh kiện; id lạ chỉ bị bỏ (DD 8.5)
            if (partReason.getVariantId() == null || !buildVariantIds.contains(partReason.getVariantId())) {
                partial = true;
                continue;
            }
            reasons.putIfAbsent(partReason.getVariantId(), partReason.getReason());
        }
        if (partial) {
            log.warn("Giải thích có lý do cho variantId không thuộc bộ máy, đã bỏ (aiStatus = PARTIAL)");
        }
        String name = draft.getBuildName().trim();
        return new Explanation(name.length() > MAX_BUILD_NAME ? name.substring(0, MAX_BUILD_NAME) : name,
                draft.getAiReasoning().trim(),
                draft.getPerformanceHighlight() != null ? draft.getPerformanceHighlight().trim() : "",
                reasons, partial ? "PARTIAL" : "OK");
    }

    // Context rút gọn (sửa B07): chỉ hồ sơ, văn bản khách, và bộ máy với keySpecs
    private String context(ValidatedProfile profile, SolverResult result, BigDecimal budget, String purpose, String userPrompt,
                           CompatibilitySnapshot snapshot) {
        Map<String, Object> profileView = new LinkedHashMap<>();
        profileView.put("summary", profile.profile().getSummary());
        profileView.put("constraints", profile.constraints().stream().map(BuildExplainerImpl::constraintView).toList());
        profileView.put("metrics", profile.metrics().stream().map(BuildExplainerImpl::metricView).toList());
        profileView.put("unmetNeeds", profile.profile().getUnmetNeeds());

        List<Map<String, Object>> parts = new ArrayList<>();
        for (BuildLine line : result.lines()) {
            Map<String, Object> part = new LinkedHashMap<>();
            part.put("role", line.role());
            part.put("variantId", line.part().getVariantId());
            part.put("name", line.part().getProductName());
            part.put("qty", line.quantity());
            part.put("price", line.part().getPrice());
            part.put("keySpecs", keySpecs(line, profile, snapshot));
            parts.add(part);
        }
        List<String> warnings = result.compatibility().checks().stream()
                .filter(check -> "warning".equals(check.getStatus()))
                .map(CompatibilityCheckResponse::getDetail)
                .toList();

        Map<String, Object> build = new LinkedHashMap<>();
        build.put("total", result.total());
        build.put("fitPercent", result.total().multiply(BigDecimal.valueOf(100)).divide(budget, 1, RoundingMode.HALF_UP));
        build.put("leftover", result.leftover());
        build.put("parts", parts);
        build.put("warnings", warnings);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("profile", profileView);
        content.put("userText", Map.of("needs", purpose != null ? purpose : "", "extraWishes", userPrompt != null ? userPrompt : ""));
        content.put("budget", budget);
        content.put("build", build);
        content.put("reasonCode", result.reasonCode());
        content.put("relaxedItems", result.warnings().stream().filter(warning -> warning.startsWith("REQUIREMENT_RELAXED")).toList());
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không dựng được nội dung gửi AI", e);
        }
    }

    // keySpecs = thông số is_primary của danh mục + thông số xuất hiện trong constraints / preferences của hồ sơ (DD 8.4)
    private static Map<String, String> keySpecs(BuildLine line, ValidatedProfile profile, CompatibilitySnapshot snapshot) {
        Set<String> names = new LinkedHashSet<>();
        MetadataDictionaryResponse dictionary = snapshot.dictionary();
        if (dictionary != null && dictionary.getRoles().get(line.role()) != null
                && dictionary.getRoles().get(line.role()).getPrimarySpecs() != null) {
            names.addAll(dictionary.getRoles().get(line.role()).getPrimarySpecs());
        }
        profile.constraints().stream().filter(c -> line.role().equals(c.getRole())).map(ProfileConstraintDto::getSpec).forEach(names::add);
        profile.profile().getPreferences().stream().filter(p -> line.role().equals(p.getRole()))
                .map(ProfilePreferenceDto::getSpec).forEach(names::add);
        Map<String, String> specs = new LinkedHashMap<>();
        for (String name : names) {
            String value = line.part().getSpec(name);
            if (value != null) specs.put(name, value);
        }
        return specs;
    }

    private static Map<String, Object> constraintView(ProfileConstraintDto constraint) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("reason", constraint.getReason());
        view.put("spec", constraint.getSpec());
        view.put("op", constraint.getOp());
        view.put("value", constraint.getValue() != null ? constraint.getValue() : constraint.getValues());
        return view;
    }

    private static Map<String, Object> metricView(ProfileMetricDto metric) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("reason", metric.getReason());
        view.put("metric", metric.getMetric());
        view.put("op", metric.getOp());
        view.put("value", metric.getValue());
        return view;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
