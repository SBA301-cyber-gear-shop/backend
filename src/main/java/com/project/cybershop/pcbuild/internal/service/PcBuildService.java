package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.request.AutoBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.BuildResultResponse;

/** Điều phối bước 3 (kiểm lại) → 4 → 5 → 6 và kiểm lại bộ PC khi khách đổi món. */
public interface PcBuildService {

    BuildResultResponse autoBuild(AutoBuildRequest request);

    BuildResultResponse validate(ValidateBuildRequest request);
}
