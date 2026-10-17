package com.project.cybershop.catalog.api;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;

import java.util.List;
import java.util.Map;

public interface CatalogPublicApi {
    List<ComponentCandidateDTO> getActiveCandidatesForBuild();
    Map<Integer, ComponentCandidateDTO> getCandidateMapByIds(List<Integer> variantIds);
}
