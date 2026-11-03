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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;
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

    static final String PROMPT_EXPLAIN_V4 = """
            BẠN LÀ KỸ SƯ TƯ VẤN PC CỦA CYBERSHOP.
            Hệ thống đã hiểu nhu cầu của khách thành "profile" và đã chọn sẵn MỘT bộ máy.
            Bộ máy ĐÃ được kiểm tra tương thích, giá là giá thật và tổng giá KHÔNG vượt ngân sách.
            NHIỆM VỤ: Viết buildName (≤ 60 ký tự), aiReasoning (3–6 câu), performanceHighlight (1–2 câu),
            và lý do ngắn cho TỪNG variantId trong bộ máy, liên hệ với các "reason" trong profile.
            QUY TẮC:
            - KHÔNG đề xuất thay, thêm hay bớt linh kiện. KHÔNG nhắc tới linh kiện, variantId, giá hoặc thông số không có trong dữ liệu được cấp.
            - KHÔNG hứa hẹn số FPS cụ thể. Chỉ mô tả mức trải nghiệm (ví dụ "chơi tốt ở 2K thiết lập cao").
            - PHẢI nhắc lại trung thực: tổng giá so với ngân sách và số tiền còn dư (leftover), các mục đã hạ (relaxedItems),
              các nhu cầu chưa đáp ứng (unmetNeeds) và warning về giá tham khảo.
            - Lý do còn dư tiền CHỈ được nói đúng như "reasonMeaning". Không có "reasonMeaning" thì không tự đoán lý do.
            - Viết tiếng Việt CÓ DẤU đầy đủ. Số tiền viết đúng như trong "moneyText" (dạng 1.234.567 ₫).
            - Nếu có "fullSet": khách đã nhập ngân sách TRỌN BỘ (fullSet.totalText) gồm thùng máy và màn hình, bàn phím, chuột;
              "budget" chỉ là phần dành cho thùng máy. Khi nhắc ngân sách phải nói rõ cả hai con số, KHÔNG gọi "budget" là ngân sách của khách.
            - Trả về DUY NHẤT JSON theo schema.
            """;

    // Chỉ gửi khi khách thật sự chọn MAX_PRICE: để trong prompt chung thì AI áp nhầm cho máy "mạnh nhất" (B1)
    static final String MAX_PRICE_RULE = """
            - profile.objective là MAX_PRICE: nói rõ bộ máy được chọn theo tổng giá cao nhất đúng theo yêu cầu của khách,
              không phải cấu hình tối ưu cho nhu cầu. KHÔNG khen linh kiện là lựa chọn phù hợp nhất hay đáng tiền nhất.
              KHÔNG viết chữ "MAX_PRICE" hay "objective" trong câu trả lời. Tiền dư là do đã hết linh kiện đắt hơn ráp được,
              KHÔNG nói nhu cầu đã bão hòa.
            """;

    static final String DIACRITICS_RETRY = """
            - LẦN TRƯỚC bạn viết tiếng Việt KHÔNG DẤU. Viết lại toàn bộ bằng tiếng Việt CÓ DẤU đầy đủ.
            """;

    private static final Map<String, String> REASON_MEANINGS = Map.of(
            "NEEDS_SATURATED", "Nhu cầu trong hồ sơ đã được đáp ứng đủ; linh kiện đắt hơn không cải thiện thêm cho nhu cầu này nên còn dư tiền. "
                    + "KHÔNG nói cửa hàng đã hết linh kiện đắt hơn.",
            "CATALOG_CEILING", "Đã dùng linh kiện mạnh nhất cửa hàng đang có cho nhu cầu này nên còn dư tiền.",
            "REQUIREMENT_RELAXED", "Ngân sách chưa đủ cho một số yêu cầu nên hệ thống đã hạ các mục trong relaxedItems.",
            "MAX_PRICE_REACHED", "Đây là bộ máy đắt nhất ráp được trong kho với ngân sách này.");

    // Số tiền AI hay viết lộn xộn: "11,968,000đ", "15899000 dong", "VNĐ" (B8)
    private static final Pattern MONEY = Pattern.compile(
            "(\\d{1,3}(?:[.,]\\d{3})+|\\d{4,})\\s*(?:₫|VNĐ|VND|vnđ|vnd|đồng|dong|đ)(?![\\p{L}\\p{N}])");

    private static final int MAX_BUILD_NAME = 60;

    private final LlmClientService llmClientService;
    private final ObjectMapper objectMapper;

    @Override
    public Explanation explain(ValidatedProfile profile, SolverResult result, BigDecimal budget, String purpose, String userPrompt,
                               CompatibilitySnapshot snapshot, Instant deadline, BigDecimal fullSetBudget) {
        String prompt = "MAX_PRICE".equals(profile.profile().getObjective()) ? PROMPT_EXPLAIN_V4 + MAX_PRICE_RULE : PROMPT_EXPLAIN_V4;
        String context = context(profile, result, budget, purpose, userPrompt, snapshot, fullSetBudget);
        LlmClientService.ExplanationDraft draft = llmClientService.explainBuild(prompt, context, deadline);
        if (draft == null || isBlank(draft.getBuildName()) || isBlank(draft.getAiReasoning())) {
            throw new AiUnavailableException(AiUnavailableException.Detail.INVALID_RESPONSE);
        }
        if (lacksDiacritics(draft.getAiReasoning())) {
            draft = rewriteWithDiacritics(prompt, context, deadline, draft);
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
            reasons.putIfAbsent(partReason.getVariantId(), normalizeMoney(partReason.getReason()));
        }
        if (partial) {
            log.warn("Giải thích có lý do cho variantId không thuộc bộ máy, đã bỏ (aiStatus = PARTIAL)");
        }
        String name = draft.getBuildName().trim();
        return new Explanation(name.length() > MAX_BUILD_NAME ? name.substring(0, MAX_BUILD_NAME) : name,
                normalizeMoney(draft.getAiReasoning().trim()),
                draft.getPerformanceHighlight() != null ? normalizeMoney(draft.getPerformanceHighlight().trim()) : "",
                reasons, partial ? "PARTIAL" : "OK");
    }

    // Prompt đã yêu cầu có dấu nhưng model thỉnh thoảng vẫn viết không dấu (B8): gọi lại một lần, lỗi thì giữ bản đầu
    private LlmClientService.ExplanationDraft rewriteWithDiacritics(String prompt, String context, Instant deadline,
                                                                    LlmClientService.ExplanationDraft first) {
        try {
            LlmClientService.ExplanationDraft second = llmClientService.explainBuild(prompt + DIACRITICS_RETRY, context, deadline);
            if (second != null && !isBlank(second.getBuildName()) && !isBlank(second.getAiReasoning())
                    && !lacksDiacritics(second.getAiReasoning())) {
                return second;
            }
        } catch (AiUnavailableException e) {
            log.warn("Không viết lại được lời giải thích không dấu: {}", e.getMessage());
        }
        return first;
    }

    /** Văn bản tiếng Việt bình thường có khoảng 25–30% chữ mang dấu; dưới 5% coi như viết không dấu. */
    static boolean lacksDiacritics(String text) {
        long letters = text.codePoints().filter(Character::isLetter).count();
        if (letters < 40) return false;
        long marked = text.codePoints().filter(cp -> Character.isLetter(cp) && cp > 127).count();
        return marked * 20 < letters;
    }

    /** "11,968,000đ", "15899000 dong", "3.690.000 VNĐ" → "11.968.000 ₫"; số không kèm đơn vị tiền giữ nguyên. */
    static String normalizeMoney(String text) {
        if (text == null) return null;
        return MONEY.matcher(text).replaceAll(match -> vnd(new BigDecimal(match.group(1).replaceAll("[.,]", ""))));
    }

    static String vnd(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + " ₫";
    }

    // Context rút gọn (sửa B07): chỉ hồ sơ, văn bản khách, và bộ máy với keySpecs
    private String context(ValidatedProfile profile, SolverResult result, BigDecimal budget, String purpose, String userPrompt,
                           CompatibilitySnapshot snapshot, BigDecimal fullSetBudget) {
        Map<String, Object> profileView = new LinkedHashMap<>();
        profileView.put("summary", profile.profile().getSummary());
        profileView.put("constraints", profile.constraints().stream().map(BuildExplainerImpl::constraintView).toList());
        profileView.put("metrics", profile.metrics().stream().map(BuildExplainerImpl::metricView).toList());
        profileView.put("unmetNeeds", profile.profile().getUnmetNeeds());
        profileView.put("objective", profile.profile().getObjective());

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
        if (result.reasonCode() != null && REASON_MEANINGS.containsKey(result.reasonCode())) {
            content.put("reasonMeaning", REASON_MEANINGS.get(result.reasonCode()));
        }
        Map<String, String> moneyText = new LinkedHashMap<>();
        moneyText.put("budget", vnd(budget));
        moneyText.put("total", vnd(result.total()));
        moneyText.put("leftover", vnd(result.leftover()));
        content.put("moneyText", moneyText);
        if (fullSetBudget != null) {
            Map<String, Object> fullSet = new LinkedHashMap<>();
            fullSet.put("fullSetBudget", fullSetBudget);
            fullSet.put("totalText", vnd(fullSetBudget));
            fullSet.put("pcBudgetText", vnd(budget));
            fullSet.put("peripheralBudgetText", vnd(fullSetBudget.subtract(budget)));
            content.put("fullSet", fullSet);
        }
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
