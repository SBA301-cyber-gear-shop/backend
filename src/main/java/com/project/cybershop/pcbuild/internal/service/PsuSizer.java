package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.PowerRequirement;
import com.project.cybershop.pcbuild.internal.model.ScoreContext;

import java.util.List;

/** Tính và chọn nguồn (DD 6.5). Một công thức duy nhất, dùng chung cho engine (J4) và solver. */
public interface PsuSizer {

    /** Công suất cần; null khi thiếu TDP/Rec_PSU_W hoặc build chưa có CPU lẫn GPU. */
    PowerRequirement requiredW(List<BuildLine> lines, CompatibilitySnapshot snapshot);

    /**
     * Thay dòng nguồn bằng nguồn rẻ nhất đủ công suất; trong các nguồn đắt hơn tối đa 10% thì lấy nguồn điểm cao nhất.
     * Trả null khi không nguồn nào trong pool đủ công suất (J4 fail).
     */
    List<BuildLine> fix(List<BuildLine> lines, ScoreContext ctx);
}
