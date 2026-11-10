package com.project.cybershop.pcbuild.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PcBuildConfigResponse {
    private Long maxBuildPrice;
    private Long lowestMinBudget;
}
