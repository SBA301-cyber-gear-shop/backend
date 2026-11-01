package com.project.cybershop.pcbuild.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Từ điển metadata (DD 5.2, 9.6): những gì cửa hàng mô tả được, dựng hoàn toàn từ dữ liệu.
 * Gửi cho AI khi trích hồ sơ, trả cho FE ở /options, và là chuẩn để ProfileValidator kiểm hồ sơ.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataDictionaryResponse {
    private String dictionaryVersion;
    @Builder.Default
    private Map<String, RoleEntry> roles = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, MetricEntry> metrics = new LinkedHashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoleEntry {
        @JsonIgnore
        private Integer categoryId;
        // Thông số is_primary của danh mục: dùng làm keySpecs khi nhờ AI giải thích bộ máy (DD 8.4)
        @JsonIgnore
        private List<String> primarySpecs;
        private String label;
        private List<BigDecimal> priceRange;
        private List<String> brands;
        @Builder.Default
        private Map<String, SpecEntry> specs = new LinkedHashMap<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpecEntry {
        @JsonIgnore
        private Integer specId;
        private String type;                // ENUM, LIST, INT
        private String unit;
        private Integer min;
        private Integer max;
        private Map<String, String> values; // ENUM/LIST: code → nhãn hiển thị
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetricEntry {
        private String label;
        private String unit;
        private Integer min;
        private Integer max;
    }
}
