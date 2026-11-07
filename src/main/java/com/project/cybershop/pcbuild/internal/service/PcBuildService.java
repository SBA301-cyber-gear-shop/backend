package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.AutoBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ReplacementRequest;
import com.project.cybershop.pcbuild.api.dto.request.SaveBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;
import com.project.cybershop.pcbuild.api.dto.response.ReplacementOptionResponse;
import com.project.cybershop.pcbuild.api.dto.response.SaveBuildResponse;

import java.util.List;

/** Điều phối bước 3 (kiểm lại) → 4 → 5 → 6 → 8, và các thao tác của bước 9: kiểm lại, đổi món, lưu. */
public interface PcBuildService {

    /** Tự build + lời giải thích AI (bước 8 chạy đúng một lần ở đây). */
    BuildResultResponse autoBuild(AutoBuildRequest request, String clientKey);

    /** Bước 9 → 6 (+ 7): kiểm lại sau mỗi lần khách đổi món; không gọi AI, message luôn null. */
    BuildResultResponse validate(ValidateBuildRequest request);

    /** Bước 9: món tương thích cho dropdown của một loại linh kiện. */
    List<ReplacementOptionResponse> replacements(ReplacementRequest request);

    /** Bước 9: lưu pc_customs + pc_custom_items; OVER_BUDGET phải có xác nhận của khách (popup bước 7). */
    SaveBuildResponse save(SaveBuildRequest request, Integer userId);
}
