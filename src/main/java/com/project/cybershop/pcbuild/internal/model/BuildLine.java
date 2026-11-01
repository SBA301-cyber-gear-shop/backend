package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;

/** Một dòng của bộ máy: vai trò khách/solver đặt, biến thể và số lượng. */
public record BuildLine(String role, ComponentCandidateDTO part, int quantity) {
}
