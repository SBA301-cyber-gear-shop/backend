package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.BuildTier;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Trích hồ sơ yêu cầu bằng AI (DD 5.6, 8.3): cache 24 giờ theo câu nhập, gộp các lời gọi trùng khóa đang chạy.
 * AI lỗi thì ném AiUnavailableException, không thay bằng hồ sơ mẫu hay từ khóa [ĐÃ CHỐT].
 */
public interface ProfileExtractor {

    String PROMPT_VERSION = "PROFILE_V1";

    /** @param status OK = request này gọi AI; CACHED = dùng lại hồ sơ đã có hoặc chờ lời gọi trùng khóa của request khác */
    record Extraction(RequirementProfileDto draft, String status) {
    }

    Extraction extract(String purpose, String userPrompt, BigDecimal budget, BuildTier tier,
                       CompatibilitySnapshot snapshot, Instant deadline);
}
