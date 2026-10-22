package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BudgetHeuristicService {

    record PriceRange(double min, double max) {}

    Map<String, PriceRange> calculateTargetRanges(BigDecimal budget, String purpose);

    List<ComponentCandidateDTO> filterCandidates(List<ComponentCandidateDTO> all, BigDecimal budget, String purpose);
}
