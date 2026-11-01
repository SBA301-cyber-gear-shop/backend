package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.PowerRequirement;
import com.project.cybershop.pcbuild.internal.model.ScoreContext;
import com.project.cybershop.pcbuild.internal.service.PartScorer;
import com.project.cybershop.pcbuild.internal.service.PsuSizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PsuSizerImpl implements PsuSizer {

    private static final int TDP_POWER = 2;
    private static final int WATTAGE = 14;
    private static final int REC_PSU_W = 50;
    private static final int BASE_LOAD_W = 80;                                  // bo mạch, RAM, SSD, quạt
    private static final BigDecimal HEADROOM_ONE_GPU = new BigDecimal("1.30");  // [QĐTK] giữ như code cũ
    private static final BigDecimal HEADROOM_MULTI_GPU = new BigDecimal("1.35");
    private static final BigDecimal PREMIUM_TOLERANCE = new BigDecimal("1.10");

    private final PartScorer partScorer;

    @Autowired
    public PsuSizerImpl(PartScorer partScorer) {
        this.partScorer = partScorer;
    }

    /** Dùng khi chỉ cần requiredW (engine dựng tay trong test). */
    public PsuSizerImpl() {
        this(new PartScorerImpl());
    }

    @Override
    public PowerRequirement requiredW(List<BuildLine> lines, CompatibilitySnapshot snapshot) {
        int load = BASE_LOAD_W;
        int gpuCount = 0;
        int maxRecommended = 0;
        int maxGpuTdp = 0;
        boolean hasCpuOrGpu = false;
        for (BuildLine line : lines) {
            if (line == null || line.part() == null) continue;
            ParsedSpecs specs = snapshot.parsed(line.part());
            if ("cpu".equals(line.role())) {
                Integer tdp = specs.ints().get(TDP_POWER);
                if (tdp == null) return null;
                load += tdp * line.quantity();
                hasCpuOrGpu = true;
            } else if ("gpu".equals(line.role())) {
                Integer tdp = specs.ints().get(TDP_POWER);
                Integer recommended = specs.ints().get(REC_PSU_W);
                if (tdp == null || recommended == null) return null;
                load += tdp * line.quantity();
                gpuCount += line.quantity();
                maxRecommended = Math.max(maxRecommended, recommended);
                maxGpuTdp = Math.max(maxGpuTdp, tdp);
                hasCpuOrGpu = true;
            }
        }
        if (!hasCpuOrGpu) return null;
        BigDecimal headroom = gpuCount >= 2 ? HEADROOM_MULTI_GPU : HEADROOM_ONE_GPU;
        int byLoad = BigDecimal.valueOf(load).multiply(headroom).setScale(0, RoundingMode.CEILING).intValueExact();
        int byGpuRecommendation = gpuCount == 0 ? 0 : maxRecommended + (gpuCount - 1) * maxGpuTdp;
        return new PowerRequirement(load, Math.max(byLoad, byGpuRecommendation));
    }

    @Override
    public List<BuildLine> fix(List<BuildLine> lines, ScoreContext ctx) {
        PowerRequirement need = requiredW(lines, ctx.snapshot());
        if (need == null) return null;

        List<ComponentCandidateDTO> enough = ctx.pool().of("psu").stream()
                .filter(psu -> ctx.snapshot().parsed(psu).ints().getOrDefault(WATTAGE, 0) >= need.requiredW())
                .toList();
        if (enough.isEmpty()) return null;
        BigDecimal cheapest = enough.stream().map(ComponentCandidateDTO::getPrice).min(BigDecimal::compareTo).orElseThrow();
        BigDecimal ceiling = cheapest.multiply(PREMIUM_TOLERANCE);

        ComponentCandidateDTO chosen = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ComponentCandidateDTO psu : enough) {
            if (psu.getPrice().compareTo(ceiling) > 0) continue;
            double score = partScorer.partScore("psu", psu, ctx);
            if (chosen == null || score > bestScore + 1e-9
                    || (Math.abs(score - bestScore) <= 1e-9 && isCheaper(psu, chosen))) {
                chosen = psu;
                bestScore = score;
            }
        }

        List<BuildLine> result = new ArrayList<>();
        for (BuildLine line : lines) {
            if (line != null && !"psu".equals(line.role())) result.add(line);
        }
        result.add(new BuildLine("psu", chosen, 1));
        return result;
    }

    private static boolean isCheaper(ComponentCandidateDTO candidate, ComponentCandidateDTO current) {
        int byPrice = candidate.getPrice().compareTo(current.getPrice());
        return byPrice < 0 || (byPrice == 0 && candidate.getVariantId() < current.getVariantId());
    }
}
