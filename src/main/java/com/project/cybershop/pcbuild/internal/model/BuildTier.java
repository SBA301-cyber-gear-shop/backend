package com.project.cybershop.pcbuild.internal.model;

import java.math.BigDecimal;

/** Mức ngân sách (DD 5.5): mỗi mức gồm cận dưới, không gồm cận trên. */
public enum BuildTier {
    T1(0L), T2(10_000_000L), T3(20_000_000L), T4(40_000_000L), T5(80_000_000L), T6(150_000_000L), T7(300_000_000L);

    private final long lowerBound;

    BuildTier(long lowerBound) {
        this.lowerBound = lowerBound;
    }

    public long lowerBound() {
        return lowerBound;
    }

    /** Cận trên (không gồm); null với T7. */
    public Long upperBound() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1].lowerBound : null;
    }

    public static BuildTier of(BigDecimal budget) {
        BuildTier result = T1;
        for (BuildTier tier : values()) {
            if (budget.compareTo(BigDecimal.valueOf(tier.lowerBound)) >= 0) {
                result = tier;
            }
        }
        return result;
    }
}
