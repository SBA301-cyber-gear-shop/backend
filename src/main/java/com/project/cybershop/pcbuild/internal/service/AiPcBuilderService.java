package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.AiBuildResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildOptionsResponse;

public interface AiPcBuilderService {

    /** Luồng /ai-suggest (DD 3.3): hồ sơ (AI hoặc khách sửa) → solver → AI giải thích. */
    AiBuildResponse processAiBuild(AiBuildRequest request, Integer currentUserId);

    /** GET /options (DD 9.6): giới hạn ngân sách, từ điển, câu gợi ý; đọc từ snapshot, 0 câu SQL khi cache ấm. */
    BuildOptionsResponse options();
}
