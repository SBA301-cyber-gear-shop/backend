package com.project.cybershop.pcbuild.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** JSON AI trả về ở bước 2, chưa kiểm tra. Mọi giá trị đều phải qua bước 3 trước khi dùng. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RequirementDraft(Long budget, String purpose, String level, String formFactor, String brandPreference,
                               List<String> missingInfo) {
}
