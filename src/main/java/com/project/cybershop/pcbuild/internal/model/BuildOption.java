package com.project.cybershop.pcbuild.internal.model;

import java.util.List;

/**
 * Một phương án bước của solver (DD 6.4.2): bộ máy sau khi áp một bước nâng cấp hoặc hoán đổi.
 *
 * @param lines   bộ máy sau bước, đã qua engine (không có check fail) và tổng ≤ B
 * @param utility điểm U của bộ máy theo hồ sơ
 * @param ratio   ΔU / Δgiá so với bộ hiện tại; pha 2 chọn phương án có ratio lớn nhất
 */
public record BuildOption(List<BuildLine> lines, double utility, double ratio) {
}
