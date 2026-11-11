package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.RequirementAnalyzeRequest;
import com.project.cybershop.pcbuild.api.dto.response.PcBuildConfigResponse;
import com.project.cybershop.pcbuild.api.dto.response.RequirementAnalyzeResponse;

/** Dịch vụ điều phối phân tích nhu cầu và kiểm tra ngân sách (Phase 1). */
public interface RequirementService {

    /**
     * Phân tích nhu cầu tự nhiên của khách, kiểm tra ngân sách và trả về trạng thái nghiệp vụ.
     */
    RequirementAnalyzeResponse analyze(RequirementAnalyzeRequest request, String clientKey);

    /**
     * Lấy cấu hình giới hạn ngân sách ban đầu (mức tối đa và tối thiểu) tính từ Database.
     */
    PcBuildConfigResponse getConfig();
}

