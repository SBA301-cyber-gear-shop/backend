package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.model.BuildCheck;
import com.project.cybershop.pcbuild.internal.model.BuildContext;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.SlotMeta;
import com.project.cybershop.pcbuild.internal.model.RuleViolation;

import java.util.List;

/**
 * Bước 6: rule engine. Hàm thuần, không gọi DB hay AI; bước 5 dùng chung để lọc nên hai bước luôn khớp nhau.
 */
public interface CompatibilityEngine {

    /** Lỗi theo luật DB và công suất nguồn (R7); chỉ xét cặp mà cả hai loại đều có mặt. */
    List<RuleViolation> checkRules(List<BuildLine> lines, List<CompatibilityRule> rules);

    /** Số lượng tối đa thật của part = min(max_qty của slot, giới hạn từ thông số các món khác). */
    int maxQuantity(SlotMeta slot, ComponentCandidateDTO part, List<BuildLine> others, List<CompatibilityRule> rules);

    /** true khi part (thuộc required_if_category_id của slot) làm slot đó thành bắt buộc, ví dụ CPU không có iGPU. */
    boolean triggersRequiredSlot(SlotMeta slot, ComponentCandidateDTO part);

    /** Kiểm cả bộ: khuôn + luật + công suất nguồn + tiền. */
    BuildCheck checkBuild(long budget, List<BuildLine> lines, BuildContext context);
}
