package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.SavePcBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildDetailResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildSummaryResponse;

import java.util.List;

public interface PcCustomService {
    Integer saveNewBuild(SavePcBuildRequest request, Integer userId);
    void saveBuildForUser(Integer buildId, Integer userId);
    List<SavedPcBuildSummaryResponse> getMySavedBuilds(Integer userId);
    SavedPcBuildDetailResponse getSavedBuildDetail(Integer buildId, Integer userId);
    void deleteSavedBuild(Integer buildId, Integer userId);
    List<CompatibleReplacementResponse> getCompatibleReplacements(String role, Integer currentCpuId, Integer currentMbId);
}

