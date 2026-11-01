package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.support.PerfIndexEstimator;

import java.util.List;
import java.util.Map;

/**
 * Dựng từ điển metadata (DD 5.2) từ dữ liệu đã nạp vào snapshot: suy kiểu, đơn vị, min/max, thương hiệu, metric
 * và dictionaryVersion. Không truy vấn DB; CandidateRepositoryFacade gọi khi dựng snapshot.
 */
public interface MetadataDictionaryService {
    MetadataDictionaryResponse build(List<ComponentCandidateDTO> candidates,
                                     Map<Integer, ParsedSpecs> parsedByVariantId,
                                     List<CategorySpecDto> specificationCatalog,
                                     PerfIndexEstimator perfIndexEstimator);
}
