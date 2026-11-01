package com.project.cybershop.pcbuild.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Response GET /api/v1/pc-build/options (DD 9.6): giới hạn ngân sách, từ điển và câu gợi ý cho FE. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildOptionsResponse {
    private BigDecimal minBudget;
    private BigDecimal floorPrice;
    private BigDecimal maxBudget;
    private String dictionaryVersion;
    private MetadataDictionaryResponse dictionary;
    private List<String> examples;
    private List<Tier> tiers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tier {
        private String code;
        private Long min;
        private Long max;   // null với T7
    }
}
