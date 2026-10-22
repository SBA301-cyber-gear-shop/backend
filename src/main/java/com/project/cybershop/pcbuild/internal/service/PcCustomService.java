package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;

import java.util.List;

public interface PcCustomService {
    void saveBuildForUser(Integer buildId, Integer userId);
    List<CompatibleReplacementResponse> getCompatibleReplacements(String role, Integer currentCpuId, Integer currentMbId);
}
