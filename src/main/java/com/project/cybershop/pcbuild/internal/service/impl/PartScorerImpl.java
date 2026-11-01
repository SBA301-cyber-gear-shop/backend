package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.profile.BalancePairDto;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.MetricKind;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.ScoreContext;
import com.project.cybershop.pcbuild.internal.service.PartScorer;
import com.project.cybershop.pcbuild.internal.support.BuildMetrics;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PartScorerImpl implements PartScorer {

    private static final List<String> ROLES = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");
    private static final int PRICE_STATUS = 66;
    private static final int LIFECYCLE = 63;
    private static final Set<String> ESTIMATED_PRICE = Set.of("ESTIMATE", "BUILD_ONLY");
    private static final BigDecimal LEGACY_PENALTY_FROM = BigDecimal.valueOf(20_000_000L);
    private static final double BALANCE_PENALTY = 0.15;

    @Override
    public double utility(List<BuildLine> lines, ScoreContext ctx) {
        Map<MetricKind, Integer> metrics = BuildMetrics.of(lines, ctx.snapshot());
        Map<String, Double> scores = new HashMap<>();
        double utility = 0;
        for (String role : ROLES) {
            double weight = ctx.profile().weight(role);
            double score = roleScore(role, lines, metrics, ctx);
            scores.put(role, score);
            utility += weight * score;
        }
        for (BalancePairDto pair : ctx.profile().profile().getBalancePairs()) {
            double gap = Math.abs(scores.getOrDefault(pair.getA(), 0.0) - scores.getOrDefault(pair.getB(), 0.0));
            utility -= BALANCE_PENALTY * Math.max(0, gap - (pair.getMaxGap() != null ? pair.getMaxGap() : 0.2));
        }
        return utility;
    }

    @Override
    public double roleScore(String role, List<BuildLine> lines, ScoreContext ctx) {
        return roleScore(role, lines, BuildMetrics.of(lines, ctx.snapshot()), ctx);
    }

    @Override
    public double partScore(String role, ComponentCandidateDTO part, ScoreContext ctx) {
        List<BuildLine> single = List.of(new BuildLine(role, part, 1));
        return roleScore(role, single, BuildMetrics.of(single, ctx.snapshot()), ctx);
    }

    private double roleScore(String role, List<BuildLine> lines, Map<MetricKind, Integer> metrics, ScoreContext ctx) {
        List<BuildLine> roleLines = lines.stream().filter(line -> line != null && role.equals(line.role())).toList();
        if (roleLines.isEmpty()) {
            // Không có GPU thì điểm GPU = 0; CPU kèm tản nên không có tản rời vẫn được 0,2
            return "cooler".equals(role) ? 0.2 : 0.0;
        }
        double weighted = 0;
        double totalWeight = 0;
        for (ScoreContext.ResolvedPreference preference : ctx.preferences(role)) {
            double normalized = preference.metric() != null
                    ? metricScore(preference, metrics.getOrDefault(preference.metric(), 0), ctx)
                    : lineAverage(preference, roleLines, role, ctx);
            weighted += preference.weight() * normalized;
            totalWeight += preference.weight();
        }
        double score = totalWeight > 0 ? weighted / totalWeight : 0;

        List<String> preferredBrands = ctx.profile().profile().getBrands().getPrefer();
        if (!preferredBrands.isEmpty() && roleLines.stream().allMatch(line -> containsIgnoreCase(preferredBrands, line.part().getBrandName()))) {
            score = Math.min(1, score + 0.05);
        }
        if (roleLines.stream().anyMatch(line -> ESTIMATED_PRICE.contains(upper(raw(line, PRICE_STATUS, ctx))))) {
            score -= 0.02;
        }
        if (ctx.budget() != null && ctx.budget().compareTo(LEGACY_PENALTY_FROM) >= 0
                && roleLines.stream().anyMatch(line -> "LEGACY".equals(upper(raw(line, LIFECYCLE, ctx))))) {
            score -= 0.03;
        }
        return score;
    }

    // Trung bình theo quantity trên các dòng của role (nhiều SSD/GPU)
    private double lineAverage(ScoreContext.ResolvedPreference preference, List<BuildLine> roleLines, String role, ScoreContext ctx) {
        double sum = 0;
        int quantity = 0;
        for (BuildLine line : roleLines) {
            sum += partValue(preference, line.part(), role, ctx) * line.quantity();
            quantity += line.quantity();
        }
        return quantity > 0 ? sum / quantity : 0;
    }

    private double partValue(ScoreContext.ResolvedPreference preference, ComponentCandidateDTO part, String role, ScoreContext ctx) {
        int specId = preference.specId();
        if ("PREFER".equals(preference.direction())) {
            ParsedSpecs specs = ctx.snapshot().parsed(part);
            Set<String> codes = specs.lists().getOrDefault(specId, Set.of());
            return codes.stream().anyMatch(preference.values()::contains) ? 1 : 0;
        }
        Integer value = ctx.snapshot().effectiveInt(part, specId);
        if (value == null) return 0;
        int[] range = ctx.pool() != null ? ctx.pool().range(role, specId) : null;
        double low = range != null ? range[0] : value;
        double high = Math.min(preference.cap() != null ? preference.cap() : Double.POSITIVE_INFINITY,
                range != null ? range[1] : value);
        double normalized = high == low ? 1 : clamp((Math.min(value, high) - low) / (high - low));
        return "MIN".equals(preference.direction()) ? 1 - normalized : normalized;
    }

    // Metric: lo = 0, hi = min(cap, metrics.max của từ điển)
    private double metricScore(ScoreContext.ResolvedPreference preference, int value, ScoreContext ctx) {
        double high = preference.cap() != null ? preference.cap() : Double.POSITIVE_INFINITY;
        MetadataDictionaryResponse dictionary = ctx.snapshot().dictionary();
        if (dictionary != null) {
            MetadataDictionaryResponse.MetricEntry entry = dictionary.getMetrics().get(preference.metric().name());
            if (entry != null && entry.getMax() != null) high = Math.min(high, entry.getMax());
        }
        double normalized = high <= 0 || Double.isInfinite(high) ? (value > 0 ? 1 : 0) : clamp(Math.min(value, high) / high);
        return "MIN".equals(preference.direction()) ? 1 - normalized : normalized;
    }

    private static String raw(BuildLine line, int specId, ScoreContext ctx) {
        return ctx.snapshot().parsed(line.part()).raw().get(specId);
    }

    private static boolean containsIgnoreCase(List<String> values, String value) {
        return value != null && values.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(value));
    }

    private static String upper(String value) {
        return value != null ? value.toUpperCase(Locale.ROOT) : "";
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
