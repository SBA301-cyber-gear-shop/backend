package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.internal.model.BuildContext;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildPlan;

import java.util.List;

/** Bước 5: chọn lần lượt từng linh kiện theo step_order, không dùng AI. */
public interface BuildSolver {

    /**
     * @return các dòng theo step_order, tổng luôn ≤ budget và qua mọi luật
     * @throws com.project.cybershop.pcbuild.internal.support.PcBuildException 422 BUILD_NOT_FOUND khi kho không ráp được
     */
    List<BuildLine> solve(long budget, BuildPlan plan, BuildContext context);
}
