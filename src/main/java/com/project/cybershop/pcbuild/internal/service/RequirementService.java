package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.RequirementAnalyzeRequest;
import com.project.cybershop.pcbuild.api.dto.response.RequirementAnalyzeResponse;
import com.project.cybershop.pcbuild.api.dto.response.RequirementOptionsResponse;

/** Phần A của AI Build PC (bước 1–3): hiểu yêu cầu của khách và kiểm tra trước khi chia ngân sách. */
public interface RequirementService {

    /** Các danh sách giá trị hợp lệ đọc từ metadata (mục đích, cỡ main, hãng, cận trên ngân sách). */
    RequirementOptionsResponse options();

    /**
     * Bước 2–3: AI đọc yêu cầu (khi cần) rồi code kiểm tra và quyết định trạng thái.
     *
     * @param clientKey khóa giới hạn tần suất của người gọi (user hoặc IP)
     */
    RequirementAnalyzeResponse analyze(RequirementAnalyzeRequest request, String clientKey);
}
