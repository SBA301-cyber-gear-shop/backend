package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.ReplacementQueryRequest;
import com.project.cybershop.pcbuild.api.dto.request.SavePcBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildDetailResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildSummaryResponse;
import com.project.cybershop.pcbuild.api.dto.response.ValidateBuildResponse;

import java.util.List;

public interface PcCustomService {
    Integer saveNewBuild(SavePcBuildRequest request, Integer userId);
    List<SavedPcBuildSummaryResponse> getMySavedBuilds(Integer userId);
    SavedPcBuildDetailResponse getSavedBuildDetail(Integer buildId, Integer userId);
    void deleteSavedBuild(Integer buildId, Integer userId);

    /** POST /validate: kiểm toàn bộ build bằng CompatibilityEngine mode USER (DD 9.2). */
    ValidateBuildResponse validateBuild(ValidateBuildRequest request);

    /** POST /compatible-replacements: thay từng ứng viên vào build rồi chạy engine (DD 9.3). */
    List<CompatibleReplacementResponse> getCompatibleReplacements(ReplacementQueryRequest request);
}
