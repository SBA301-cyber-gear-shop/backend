package com.project.cybershop.pcbuild.internal.support;

import java.util.Set;

/**
 * Id danh mục và id thông số mà bước 5–6 đọc trực tiếp (trùng docs/Seed_data_AIBuildPC.sql, không đánh số lại).
 */
public final class BuildIds {

    public static final int CATEGORY_CPU = 1;
    public static final int CATEGORY_MAINBOARD = 2;
    public static final int CATEGORY_RAM = 3;
    public static final int CATEGORY_GPU = 4;
    public static final int CATEGORY_PSU = 6;
    public static final int CATEGORY_COOLER = 8;

    public static final int SPEC_TDP_POWER = 2;
    public static final int SPEC_CAPACITY_GB = 9;
    public static final int SPEC_WATTAGE = 14;
    public static final int SPEC_HEIGHT_MM = 19;
    public static final int SPEC_COOLER_TYPE = 20;
    public static final int SPEC_CPU_SOCKET_COUNT = 31;
    public static final int SPEC_PCIE_X16_SLOTS = 32;
    public static final int SPEC_MODULES_COUNT = 34;
    public static final int SPEC_M2_SLOTS_USED = 40;
    public static final int SPEC_MAX_GPU_QTY = 54;
    public static final int SPEC_RADIATOR_SIZE_MM = 60;

    /** Thông số tính cho MỘT cái: vế trái của luật nhân thêm số lượng (docx 3.3: "R3, R4 nhân thêm số lượng"). */
    public static final Set<Integer> ADDITIVE_SPEC_IDS = Set.of(SPEC_CAPACITY_GB, SPEC_MODULES_COUNT, SPEC_M2_SLOTS_USED);

    /** Số lượng tự build lấy cho RAM (kênh đôi); các loại khác lấy 1 (docx bước 5). */
    public static final int RAM_AUTO_QUANTITY = 2;

    public static final String COOLER_AIR = "AIR";
    public static final String COOLER_LIQUID = "LIQUID";
    public static final String VALUE_UNKNOWN = "UNKNOWN";

    private BuildIds() {
    }
}
