package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.ScoreContext;

import java.util.List;

/** Chấm điểm theo hồ sơ (DD 6.2, 6.3). Thuần bộ nhớ, không gọi DB. */
public interface PartScorer {

    /** Điểm của cả bộ máy U ∈ [0, 1] (có thể âm nhẹ do penalty cân đối). */
    double utility(List<BuildLine> lines, ScoreContext ctx);

    /** Điểm vai trò s(role) của build. */
    double roleScore(String role, List<BuildLine> lines, ScoreContext ctx);

    /** Điểm của một ứng viên đơn lẻ (số lượng 1), dùng để sắp pool. */
    double partScore(String role, ComponentCandidateDTO part, ScoreContext ctx);
}
