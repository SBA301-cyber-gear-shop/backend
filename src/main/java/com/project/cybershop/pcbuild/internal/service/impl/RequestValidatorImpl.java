package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.internal.service.RequestValidator;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RequestValidatorImpl implements RequestValidator {

    private static final Set<Integer> LEGACY_RAM_OPTIONS = Set.of(8, 16, 32, 64, 96, 128, 256, 512);
    private static final int MAX_PROFILE_BYTES = 32 * 1024;

    private final ObjectMapper objectMapper;
    private final BigDecimal minBudget;
    private final BigDecimal maxBudget;

    public RequestValidatorImpl(ObjectMapper objectMapper,
                                @Value("${app.pcbuild.min-budget:8000000}") BigDecimal minBudget,
                                @Value("${app.pcbuild.max-budget:9999999999}") BigDecimal maxBudget) {
        this.objectMapper = objectMapper;
        this.minBudget = minBudget;
        this.maxBudget = maxBudget;
    }

    @Override
    public void validate(AiBuildRequest request, BigDecimal floorPrice) {
        BigDecimal budget = request.getBudget();
        BigDecimal threshold = floorPrice != null && floorPrice.compareTo(minBudget) > 0 ? floorPrice : minBudget;
        if (budget == null || budget.compareTo(threshold) < 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("minBudget", minBudget);
            data.put("floorPrice", floorPrice);
            throw new BuildRuleException("BUDGET_BELOW_FLOOR", HttpStatus.BAD_REQUEST,
                    "Ngân sách tối thiểu để ráp một bộ máy mới là " + vnd(threshold), data);
        }
        if (budget.compareTo(maxBudget) > 0) {
            throw new BuildRuleException("BUDGET_TOO_HIGH", HttpStatus.BAD_REQUEST,
                    "Ngân sách tối đa là " + vnd(maxBudget), Map.of("maxBudget", maxBudget));
        }
        BigDecimal fullSetBudget = request.getFullSetBudget();
        if (fullSetBudget != null && fullSetBudget.compareTo(budget) < 0) {
            throw new BuildRuleException("INVALID_REQUEST", HttpStatus.BAD_REQUEST,
                    "Ngân sách trọn bộ không được nhỏ hơn ngân sách dành cho thùng máy", null);
        }
        if (fullSetBudget != null && fullSetBudget.compareTo(maxBudget) > 0) {
            throw new BuildRuleException("BUDGET_TOO_HIGH", HttpStatus.BAD_REQUEST,
                    "Ngân sách tối đa là " + vnd(maxBudget), Map.of("maxBudget", maxBudget));
        }
        if (request.getProfile() == null) {
            String purpose = request.getPurpose() != null ? request.getPurpose().trim() : "";
            if (purpose.length() < 2 || purpose.length() > 300) {
                throw new BuildRuleException("NEEDS_REQUIRED", HttpStatus.BAD_REQUEST,
                        "Vui lòng mô tả nhu cầu sử dụng chính (2–300 ký tự)", null);
            }
        } else if (sizeOf(request) > MAX_PROFILE_BYTES) {
            throw new BuildRuleException("INVALID_PROFILE", HttpStatus.BAD_REQUEST, "Hồ sơ yêu cầu quá lớn (tối đa 32 KB)", null);
        }
        if (request.getUserPrompt() != null && request.getUserPrompt().trim().length() > 500) {
            throw new BuildRuleException("INVALID_REQUEST", HttpStatus.BAD_REQUEST, "Mong muốn thêm tối đa 500 ký tự", null);
        }
        if (request.getRamGb() != null && !LEGACY_RAM_OPTIONS.contains(request.getRamGb())) {
            throw new BuildRuleException("INVALID_REQUEST", HttpStatus.BAD_REQUEST,
                    "Dung lượng RAM chỉ nhận 8, 16, 32, 64, 96, 128, 256 hoặc 512 GB", null);
        }
    }

    @Override
    public BigDecimal minBudget() {
        return minBudget;
    }

    @Override
    public BigDecimal maxBudget() {
        return maxBudget;
    }

    private int sizeOf(AiBuildRequest request) {
        try {
            return objectMapper.writeValueAsString(request.getProfile()).getBytes(StandardCharsets.UTF_8).length;
        } catch (JsonProcessingException e) {
            throw new BuildRuleException("INVALID_PROFILE", HttpStatus.BAD_REQUEST, "Hồ sơ yêu cầu không đúng cấu trúc", null);
        }
    }

    private static String vnd(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + " ₫";
    }
}
