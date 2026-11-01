package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.MetricKind;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Chỉ số cấp bộ máy (DD 5.2): phép cộng theo số lượng trên các dòng của build. */
public final class BuildMetrics {

    private static final int CAPACITY_GB = 9;
    private static final int VRAM_GB = 12;
    private static final int CORE_COUNT = 22;

    private BuildMetrics() {
    }

    public static Map<MetricKind, Integer> of(List<BuildLine> lines, CompatibilitySnapshot snapshot) {
        Map<MetricKind, Integer> values = new EnumMap<>(MetricKind.class);
        for (MetricKind kind : MetricKind.values()) {
            values.put(kind, 0);
        }
        for (BuildLine line : lines) {
            if (line == null || line.part() == null) continue;
            ParsedSpecs specs = snapshot.parsed(line.part());
            int quantity = line.quantity();
            String role = line.role() != null ? line.role() : "";
            if (role.equals("ram")) {
                values.merge(MetricKind.TOTAL_RAM_GB, specs.ints().getOrDefault(CAPACITY_GB, 0) * quantity, Integer::sum);
            } else if (role.equals("ssd")) {
                values.merge(MetricKind.TOTAL_STORAGE_GB, specs.ints().getOrDefault(CAPACITY_GB, 0) * quantity, Integer::sum);
            } else if (role.equals("gpu")) {
                values.merge(MetricKind.TOTAL_VRAM_GB, specs.ints().getOrDefault(VRAM_GB, 0) * quantity, Integer::sum);
                values.merge(MetricKind.GPU_COUNT, quantity, Integer::sum);
            } else if (role.equals("cpu")) {
                values.merge(MetricKind.CPU_COUNT, quantity, Integer::sum);
                values.merge(MetricKind.TOTAL_CPU_CORES, specs.ints().getOrDefault(CORE_COUNT, 0) * quantity, Integer::sum);
            }
        }
        return values;
    }
}
