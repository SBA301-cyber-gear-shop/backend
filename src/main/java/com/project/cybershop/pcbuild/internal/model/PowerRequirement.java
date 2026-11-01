package com.project.cybershop.pcbuild.internal.model;

/** Kết quả PsuSizer (DD 6.5): tải ước tính và công suất nguồn tối thiểu an toàn (W). */
public record PowerRequirement(int loadW, int requiredW) {
}
