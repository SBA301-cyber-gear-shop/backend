package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.internal.model.BuildMetadata;
import com.project.cybershop.pcbuild.internal.model.BuildPlan;

/** Bước 4: chia ngân sách theo budget_profiles của mục đích. */
public interface BudgetAllocationService {

    BuildPlan plan(String purpose, long budget, BuildMetadata metadata);
}
