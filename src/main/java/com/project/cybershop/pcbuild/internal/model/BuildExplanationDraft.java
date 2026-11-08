package com.project.cybershop.pcbuild.internal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** JSON AI trả ở bước 8 theo responseSchema {"message": "..."}; code kiểm lại trước khi dùng. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildExplanationDraft {
    private String message;
}
