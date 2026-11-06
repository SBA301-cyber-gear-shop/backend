package com.project.cybershop.pcbuild.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** JSON AI trả về ở bước 2, chưa kiểm tra. Mọi giá trị đều phải qua bước 3 trước khi dùng. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RequirementDraft(String purpose, String level, String formFactor, String brandPreference) {
}
