package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileRoleRefDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildOption;
import com.project.cybershop.pcbuild.internal.model.CandidatePool;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.EngineReport;
import com.project.cybershop.pcbuild.internal.model.EvaluationContext;
import com.project.cybershop.pcbuild.internal.model.MetricKind;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.ScoreContext;
import com.project.cybershop.pcbuild.internal.model.SolverInput;
import com.project.cybershop.pcbuild.internal.model.SolverResult;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.BuildSolver;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.service.PartScorer;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import com.project.cybershop.pcbuild.internal.service.PsuSizer;
import com.project.cybershop.pcbuild.internal.support.BuildMetrics;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import com.project.cybershop.pcbuild.internal.support.CandidatePoolBuilder;
import com.project.cybershop.pcbuild.internal.support.ProfileMatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Solver 3 pha (DD 6.4): dựng bộ hợp lệ rẻ nhất (hạ yêu cầu nếu vượt B) → nâng cấp tham lam theo Δđiểm/Δgiá →
 * hoán đổi để lấp tiền còn lại. Thuần bộ nhớ (nguyên tắc 7): không inject repository, CatalogPublicApi, LLM hay
 * CandidateRepositoryFacade; dữ liệu chỉ đến từ snapshot trong SolverInput, pool dựng lại bằng CandidatePoolBuilder.
 */
@Slf4j
@Service
public class GreedyBuildSolver implements BuildSolver {

    static final List<String> ROLE_ORDER = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");
    private static final List<String> SCORED_ROLES = List.of("cpu", "mainboard", "ram", "gpu", "ssd", "case", "cooler");
    private static final int MAX_ITERATIONS = 200;
    private static final int MAX_REFINE_ROUNDS = 50;
    private static final int SWAP2_TOP = 15;
    private static final int REPAIR_TOP = 8;
    private static final int REPAIR_STEPS = 12;
    private static final int MAX_QUANTITY = 8;
    private static final double EPS = 1e-4;
    private static final BigDecimal WINDOW_MIN = new BigDecimal("0.90"); // [ĐÃ CHỐT]

    // Id thông số (DD 4.2)
    private static final int SOCKET = 1;
    private static final int MEMORY_TYPE = 4;
    private static final int RAM_SLOTS = 5;
    private static final int MAX_RAM_GB = 6;
    private static final int FORM_FACTOR = 7;
    private static final int M2_SLOTS = 8;
    private static final int CAPACITY_GB = 9;
    private static final int LENGTH_MM = 11;
    private static final int VRAM_GB = 12;
    private static final int MAX_GPU_LENGTH_MM = 16;
    private static final int MAX_COOLER_HEIGHT_MM = 17;
    private static final int SUPPORTED_MOTHERBOARDS = 18;
    private static final int HEIGHT_MM = 19;
    private static final int COOLER_TYPE = 20;
    private static final int SUPPORTED_SOCKETS = 21;
    private static final int CORE_COUNT = 22;
    private static final int HAS_IGPU = 26;
    private static final int INCLUDES_COOLER = 27;
    private static final int MAX_CPU_SOCKETS = 28;
    private static final int CPU_SOCKET_COUNT = 31;
    private static final int PCIE_X16_SLOTS = 32;
    private static final int MODULES_COUNT = 34;
    private static final int M2_SLOTS_USED = 40;
    private static final int GPU_COOLER_DESIGN = 53;
    private static final int MAX_GPU_QTY = 54;
    private static final int MAX_RADIATOR_MM = 59;
    private static final int RADIATOR_SIZE_MM = 60;

    private final CompatibilityEngine compatibilityEngine;
    private final PsuSizer psuSizer;
    private final PartScorer partScorer;
    private final CandidatePoolBuilder candidatePoolBuilder;
    private final ProfilePresets profilePresets;

    private volatile FloorPriceCache floorPriceCache;

    private record FloorPriceCache(CompatibilitySnapshot snapshot, BigDecimal price) {
    }

    public GreedyBuildSolver(CompatibilityEngine compatibilityEngine, PsuSizer psuSizer, PartScorer partScorer,
                             CandidatePoolBuilder candidatePoolBuilder, ProfilePresets profilePresets) {
        this.compatibilityEngine = compatibilityEngine;
        this.psuSizer = psuSizer;
        this.partScorer = partScorer;
        this.candidatePoolBuilder = candidatePoolBuilder;
        this.profilePresets = profilePresets;
    }

    @Override
    public SolverResult solve(SolverInput input) {
        return new Run(input.budget(), input.snapshot()).solve(input.profile(), input.pool());
    }

    @Override
    public BigDecimal floorPrice(CompatibilitySnapshot snapshot) {
        FloorPriceCache cache = floorPriceCache;
        if (cache != null && cache.snapshot() == snapshot) {
            return cache.price();
        }
        // DD 6.8: hồ sơ rỗng (không constraint, metric, requiredRoles), không lọc theo giá
        RequirementProfileDto empty = RequirementProfileDto.builder()
                .profileVersion(1).source("AI").archetype(Archetype.GENERAL.name())
                .roleWeights(new LinkedHashMap<>(profilePresets.roleWeights(Archetype.GENERAL)))
                .build();
        ValidatedProfile profile = new ValidatedProfile(empty, Archetype.GENERAL, List.of());
        Run run = new Run(null, snapshot);
        List<BuildLine> cheapest = run.cheapestValidBuild(profile, candidatePoolBuilder.build(snapshot, profile, null));
        BigDecimal price = cheapest != null ? totalOf(cheapest) : null;
        floorPriceCache = new FloorPriceCache(snapshot, price);
        return price;
    }

    /** Trạng thái của một lần giải; mọi vòng lặp duyệt theo thứ tự pool nên kết quả tất định (DD 6.4.4). */
    private final class Run {
        private final BigDecimal budget;
        private final CompatibilitySnapshot snapshot;
        private ValidatedProfile profile;
        private CandidatePool pool;
        private ScoreContext ctx;
        private final List<String> relaxedItems = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        private Run(BigDecimal budget, CompatibilitySnapshot snapshot) {
            this.budget = budget;
            this.snapshot = snapshot;
        }

        private void use(ValidatedProfile nextProfile, CandidatePool nextPool) {
            this.profile = nextProfile;
            this.pool = nextPool;
            this.ctx = ScoreContext.of(nextProfile, nextPool, snapshot, budget, profilePresets::defaultPreferences);
        }

        private SolverResult solve(ValidatedProfile originalProfile, CandidatePool originalPool) {
            use(originalProfile, originalPool);

            // Pha 1: bộ hợp lệ rẻ nhất; floorPrice ghi MỘT LẦN ở đây, trước mọi bước hạ
            List<BuildLine> start = cheapestValidBuild(profile, pool);
            BigDecimal floorFirst = start != null ? totalOf(start) : null;
            while (start == null || totalOf(start).compareTo(budget) > 0) {
                if (!relaxOneStep()) {
                    throw noFeasibleBuild(originalProfile, floorFirst);
                }
                start = cheapestValidBuild(profile, pool);
            }

            // Pha 2: nâng cấp tham lam theo tỷ lệ Δđiểm / Δgiá
            List<BuildLine> current = start;
            double currentUtility = utility(current);
            BigDecimal windowFloor = budget.multiply(WINDOW_MIN);
            int iterations = 0;
            while (iterations < MAX_ITERATIONS) {
                BuildOption best = null;
                BigDecimal currentTotal = totalOf(current);
                for (List<BuildLine> move : upgradeMoves(current)) {
                    List<BuildLine> next = psuSizer.fix(move, ctx);
                    if (next == null) continue;
                    BigDecimal nextTotal = totalOf(next);
                    if (nextTotal.compareTo(budget) > 0 || !satisfiesProfile(next)) continue;
                    double nextUtility = utility(next);
                    double gain = nextUtility - currentUtility;
                    if (gain <= EPS) continue;   // không cải thiện → bỏ, chống "nhồi" linh kiện
                    double priceDelta = Math.max(nextTotal.subtract(currentTotal).doubleValue(), 1);
                    double ratio = gain / priceDelta;
                    if (currentTotal.compareTo(windowFloor) < 0 && nextTotal.compareTo(windowFloor) >= 0) {
                        ratio *= 1.5;
                    }
                    if (best != null && ratio <= best.ratio()) continue;
                    if (!isCompatible(next)) continue;
                    best = new BuildOption(next, nextUtility, ratio);
                }
                if (best == null) break;
                current = best.lines();
                currentUtility = best.utility();
                iterations++;
            }
            if (iterations >= MAX_ITERATIONS) {
                log.warn("Solver chạm giới hạn {} vòng nâng cấp, trả bộ hiện tại", MAX_ITERATIONS);
            }

            // Pha 3: tinh chỉnh sát ngân sách bằng hoán đổi, chỉ nhận bước làm U tăng thật
            for (int round = 0; round < MAX_REFINE_ROUNDS; round++) {
                List<BuildLine> best = null;
                double bestUtility = currentUtility;
                BigDecimal bestTotal = null;
                for (List<BuildLine> move : neighbours(current)) {
                    List<BuildLine> next = psuSizer.fix(move, ctx);
                    if (next == null) continue;
                    BigDecimal nextTotal = totalOf(next);
                    if (nextTotal.compareTo(budget) > 0 || !satisfiesProfile(next)) continue;
                    double nextUtility = utility(next);
                    if (nextUtility <= currentUtility + EPS) continue;
                    boolean better = best == null || nextUtility > bestUtility + EPS
                            || (Math.abs(nextUtility - bestUtility) <= EPS && nextTotal.compareTo(bestTotal) > 0);
                    if (!better || !isCompatible(next)) continue;
                    best = next;
                    bestUtility = nextUtility;
                    bestTotal = nextTotal;
                }
                if (best == null) break;
                current = best;
                currentUtility = bestUtility;
                iterations++;
            }

            BigDecimal total = totalOf(current);
            String reasonCode = relaxedItems.isEmpty() ? null : "REQUIREMENT_RELAXED";
            if (total.compareTo(windowFloor) < 0) {
                reasonCode = isCatalogCeiling(current) ? "CATALOG_CEILING" : "NEEDS_SATURATED";
            }
            EngineReport report = compatibilityEngine.evaluate(current,
                    new EvaluationContext(budget, EvaluationContext.Mode.SOLVER, snapshot));
            return new SolverResult(ordered(current), total, budget.subtract(total), currentUtility, report, reasonCode,
                    relaxedItems, floorFirst, iterations, warnings, profile);
        }

        // ---------- Pha 1: cheapestValidBuild (DD 6.4.1) ----------

        private List<BuildLine> cheapestValidBuild(ValidatedProfile currentProfile, CandidatePool currentPool) {
            ValidatedProfile savedProfile = profile;
            CandidatePool savedPool = pool;
            ScoreContext savedCtx = ctx;
            use(currentProfile, currentPool);
            try {
                return findCheapest();
            } finally {
                profile = savedProfile;
                pool = savedPool;
                ctx = savedCtx;
            }
        }

        private List<BuildLine> findCheapest() {
            Needs needs = Needs.of(profile);
            List<BuildLine> best = null;
            BigDecimal bestTotal = null;
            double bestUtility = 0;
            for (ComponentCandidateDTO cpu : pool.of("cpu")) {
                ParsedSpecs cpuSpecs = snapshot.parsed(cpu);
                for (ComponentCandidateDTO board : pool.of("mainboard")) {
                    List<BuildLine> lines = assemble(cpu, cpuSpecs, board, needs);
                    if (lines == null) continue;
                    BigDecimal total = totalOf(lines);
                    if (best != null && total.compareTo(bestTotal) > 0) continue;
                    if (!satisfiesProfile(lines) || !isCompatible(lines)) continue;
                    double lineUtility = utility(lines);
                    boolean better = best == null || total.compareTo(bestTotal) < 0
                            || lineUtility > bestUtility + EPS
                            || (Math.abs(lineUtility - bestUtility) <= EPS && idSum(lines) < idSum(best));
                    if (better) {
                        best = lines;
                        bestTotal = total;
                        bestUtility = lineUtility;
                    }
                }
            }
            return best;
        }

        /** Ghép nền tảng (CPU, bo) với nhóm RAM / GPU / SSD / tản / vỏ rẻ nhất hợp lệ cho cặp đó, rồi chỉnh nguồn. */
        private List<BuildLine> assemble(ComponentCandidateDTO cpu, ParsedSpecs cpuSpecs, ComponentCandidateDTO board, Needs needs) {
            ParsedSpecs boardSpecs = snapshot.parsed(board);
            if (!equalsIgnoreCase(cpuSpecs.raw().get(SOCKET), boardSpecs.raw().get(SOCKET))) return null;
            int cpuCount = boardSpecs.ints().getOrDefault(CPU_SOCKET_COUNT, 1);
            if (cpuSpecs.ints().getOrDefault(MAX_CPU_SOCKETS, 1) < cpuCount || cpuCount < needs.cpu() || cpuCount > needs.maxCpu()) {
                return null;
            }
            if (cpuSpecs.ints().getOrDefault(CORE_COUNT, 0) * cpuCount < needs.cores()) return null;
            boolean integratedGraphics = "YES".equalsIgnoreCase(cpuSpecs.raw().get(HAS_IGPU));
            if (profile.isExcluded("gpu") && !integratedGraphics) return null;
            boolean needsGpu = needs.gpuRequired() || !integratedGraphics;

            Selection ram = cheapestRam(boardSpecs, needs);
            if (ram == null) return null;
            Selection gpu = null;
            if (needsGpu) {
                gpu = cheapestGpu(boardSpecs, needs);
                if (gpu == null) return null;
            }
            Selection ssd = cheapestSsd(boardSpecs, needs);
            if (ssd == null) return null;

            boolean needsCooler = !"YES".equalsIgnoreCase(cpuSpecs.raw().get(INCLUDES_COOLER))
                    || profile.isRequired("cooler") || cpuCount >= 2;
            List<ComponentCandidateDTO> coolerOptions = new ArrayList<>();
            if (needsCooler) {
                String socket = cpuSpecs.raw().get(SOCKET);
                for (ComponentCandidateDTO cooler : pool.of("cooler")) {
                    if (socket != null && snapshot.parsed(cooler).lists().getOrDefault(SUPPORTED_SOCKETS, java.util.Set.of())
                            .contains(socket.toUpperCase(Locale.ROOT))) {
                        coolerOptions.add(cooler);
                    }
                }
                if (coolerOptions.isEmpty()) return null;
            } else {
                coolerOptions.add(null);
            }

            ComponentCandidateDTO chosenCooler = null;
            ComponentCandidateDTO chosenCase = null;
            BigDecimal chosenCost = null;
            for (ComponentCandidateDTO cooler : coolerOptions) {
                ComponentCandidateDTO pcCase = cheapestCase(boardSpecs, gpu != null ? gpu.part() : null, cooler);
                if (pcCase == null) continue;
                BigDecimal cost = pcCase.getPrice().add(cooler != null ? cooler.getPrice().multiply(BigDecimal.valueOf(cpuCount)) : BigDecimal.ZERO);
                if (chosenCost == null || cost.compareTo(chosenCost) < 0) {
                    chosenCooler = cooler;
                    chosenCase = pcCase;
                    chosenCost = cost;
                }
            }
            if (chosenCase == null) return null;

            List<BuildLine> lines = new ArrayList<>();
            lines.add(new BuildLine("cpu", cpu, cpuCount));
            lines.add(new BuildLine("mainboard", board, 1));
            lines.add(new BuildLine("ram", ram.part(), ram.quantity()));
            if (gpu != null) lines.add(new BuildLine("gpu", gpu.part(), gpu.quantity()));
            lines.add(new BuildLine("ssd", ssd.part(), ssd.quantity()));
            lines.add(new BuildLine("case", chosenCase, 1));
            if (chosenCooler != null) lines.add(new BuildLine("cooler", chosenCooler, cpuCount));
            return psuSizer.fix(lines, ctx);
        }

        private Selection cheapestRam(ParsedSpecs board, Needs needs) {
            Selection best = null;
            for (ComponentCandidateDTO ram : pool.of("ram")) {
                ParsedSpecs specs = snapshot.parsed(ram);
                if (!equalsIgnoreCase(specs.raw().get(MEMORY_TYPE), board.raw().get(MEMORY_TYPE))) continue;
                Integer capacity = specs.ints().get(CAPACITY_GB);
                Integer modules = specs.ints().get(MODULES_COUNT);
                if (capacity == null || capacity <= 0 || modules == null) continue;
                int quantity = Math.max(1, ceilDiv(needs.ramGb(), capacity));
                if (quantity > MAX_QUANTITY || modules * quantity > board.ints().getOrDefault(RAM_SLOTS, 0)
                        || capacity * quantity > board.ints().getOrDefault(MAX_RAM_GB, 0)) continue;
                best = cheaper(best, new Selection(ram, quantity));
            }
            return best;
        }

        private Selection cheapestGpu(ParsedSpecs board, Needs needs) {
            Selection best = null;
            int slots = board.ints().getOrDefault(PCIE_X16_SLOTS, 1);
            for (ComponentCandidateDTO gpu : pool.of("gpu")) {
                ParsedSpecs specs = snapshot.parsed(gpu);
                int vram = specs.ints().getOrDefault(VRAM_GB, 0);
                if (needs.vramGb() > 0 && vram <= 0) continue;
                int quantity = Math.max(1, Math.max(needs.gpu(), needs.vramGb() > 0 ? ceilDiv(needs.vramGb(), vram) : 0));
                if (quantity > needs.maxGpu() || quantity > slots || quantity > specs.ints().getOrDefault(MAX_GPU_QTY, 1)) continue;
                if (quantity >= 3 && !"BLOWER".equalsIgnoreCase(specs.raw().get(GPU_COOLER_DESIGN))) continue;
                best = cheaper(best, new Selection(gpu, quantity));
            }
            return best;
        }

        private Selection cheapestSsd(ParsedSpecs board, Needs needs) {
            Selection best = null;
            for (ComponentCandidateDTO ssd : pool.of("ssd")) {
                ParsedSpecs specs = snapshot.parsed(ssd);
                Integer capacity = specs.ints().get(CAPACITY_GB);
                if (capacity == null || capacity <= 0) continue;
                int quantity = Math.max(1, ceilDiv(needs.storageGb(), capacity));
                if (quantity > MAX_QUANTITY
                        || specs.ints().getOrDefault(M2_SLOTS_USED, 0) * quantity > board.ints().getOrDefault(M2_SLOTS, 0)) continue;
                best = cheaper(best, new Selection(ssd, quantity));
            }
            return best;
        }

        // Vỏ rẻ nhất thỏa R3, R4 (bỏ qua khi tản nước), R6, R10 và ngoại lệ J10 của vỏ
        private ComponentCandidateDTO cheapestCase(ParsedSpecs board, ComponentCandidateDTO gpu, ComponentCandidateDTO cooler) {
            ParsedSpecs gpuSpecs = gpu != null ? snapshot.parsed(gpu) : null;
            ParsedSpecs coolerSpecs = cooler != null ? snapshot.parsed(cooler) : null;
            boolean airCooler = coolerSpecs != null && !"Liquid".equalsIgnoreCase(coolerSpecs.raw().get(COOLER_TYPE));
            String formFactor = board.raw().get(FORM_FACTOR);
            ComponentCandidateDTO best = null;
            for (ComponentCandidateDTO pcCase : pool.of("case")) {
                ParsedSpecs specs = snapshot.parsed(pcCase);
                if (formFactor == null || !specs.lists().getOrDefault(SUPPORTED_MOTHERBOARDS, java.util.Set.of())
                        .contains(formFactor.toUpperCase(Locale.ROOT))) continue;
                if (gpuSpecs != null) {
                    Integer maxLength = specs.ints().get(MAX_GPU_LENGTH_MM);
                    Integer length = gpuSpecs.ints().get(LENGTH_MM);
                    if (maxLength == null || length == null || length > maxLength) continue;
                }
                if (airCooler) {
                    Integer maxHeight = specs.ints().get(MAX_COOLER_HEIGHT_MM);
                    Integer height = coolerSpecs.ints().get(HEIGHT_MM);
                    if (maxHeight == null || height == null || height > maxHeight) continue;
                }
                if (coolerSpecs != null && specs.ints().getOrDefault(MAX_RADIATOR_MM, 0)
                        < coolerSpecs.ints().getOrDefault(RADIATOR_SIZE_MM, 0)) continue;
                if (best == null || isCheaper(pcCase, best)) best = pcCase;
            }
            return best;
        }

        // ---------- Hạ yêu cầu (DD 6.7) ----------

        private boolean relaxOneStep() {
            for (int priority = 3; priority >= 2; priority--) {
                List<Relaxation> options = relaxationsAt(priority);
                if (options.isEmpty()) continue;
                List<Relaxation> unsatisfiable = options.stream().filter(Relaxation::unsatisfiable).toList();
                List<Relaxation> pickFrom = unsatisfiable.isEmpty() ? options : unsatisfiable;
                Relaxation chosen = null;
                BigDecimal chosenCost = null;
                CandidatePool chosenPool = null;
                for (Relaxation option : pickFrom) {
                    CandidatePool relaxedPool = candidatePoolBuilder.build(snapshot, option.profile(), budget);
                    List<BuildLine> cheapest = cheapestValidBuild(option.profile(), relaxedPool);
                    BigDecimal cost = cheapest != null ? totalOf(cheapest) : null;
                    // DD 6.7: ưu tiên mục làm giảm chi phí bộ rẻ nhất nhiều nhất. Bổ sung: nếu đã có cách hạ làm bộ rẻ nhất
                    // vừa B thì chọn cách hạ ÍT nhất (bộ rẻ nhất sát B nhất), để không bỏ hẳn GPU khi chỉ cần lùi một bậc.
                    // Hòa thì mục có id lớn hơn (AI liệt kê sau)
                    int comparison = compareRelaxation(cost, chosenCost);
                    boolean better = chosen == null || comparison < 0
                            || (comparison == 0 && option.id().compareTo(chosen.id()) > 0);
                    if (better) {
                        chosen = option;
                        chosenCost = cost;
                        chosenPool = relaxedPool;
                    }
                }
                if (!relaxedItems.contains(chosen.id())) relaxedItems.add(chosen.id());
                warnings.add("REQUIREMENT_RELAXED:" + chosen.id() + ":" + chosen.change());
                use(chosen.profile(), chosenPool);
                return true;
            }
            return false;
        }

        /** Âm = cost tốt hơn chosen: vừa B thắng không vừa B; cùng vừa B thì đắt hơn (hạ ít hơn) thắng; cùng không vừa thì rẻ hơn thắng. */
        private int compareRelaxation(BigDecimal cost, BigDecimal chosenCost) {
            boolean fits = cost != null && cost.compareTo(budget) <= 0;
            boolean chosenFits = chosenCost != null && chosenCost.compareTo(budget) <= 0;
            if (fits != chosenFits) return fits ? -1 : 1;
            if (fits) return chosenCost.compareTo(cost);
            return compareCost(cost, chosenCost);
        }

        private record Relaxation(String id, boolean unsatisfiable, ValidatedProfile profile, String change) {
        }

        private List<Relaxation> relaxationsAt(int priority) {
            RequirementProfileDto current = profile.profile();
            List<Relaxation> options = new ArrayList<>();
            for (ProfileConstraintDto constraint : current.getConstraints()) {
                if (Objects.equals(constraint.getPriority(), priority)) options.add(relaxConstraint(constraint));
            }
            for (ProfileMetricDto metric : current.getMetrics()) {
                if (Objects.equals(metric.getPriority(), priority)) options.add(relaxMetric(metric));
            }
            for (ProfileRoleRefDto required : current.getRequiredRoles()) {
                if (Objects.equals(required.getPriority(), priority)) {
                    RequirementProfileDto changed = copy(current);
                    changed.getRequiredRoles().removeIf(ref -> ref.getRole().equals(required.getRole()));
                    options.add(new Relaxation("required:" + required.getRole(), false, profile.withProfile(changed),
                            "bắt buộc " + required.getRole() + "→bỏ"));
                }
            }
            return options;
        }

        // INT GTE lùi về giá trị nhỏ hơn kế tiếp có trong pool (LTE ngược lại); hết bậc, ENUM/LIST thì bỏ mục
        private Relaxation relaxConstraint(ProfileConstraintDto constraint) {
            RequirementProfileDto withoutIt = copy(profile.profile());
            withoutIt.getConstraints().removeIf(item -> item.getId().equals(constraint.getId()));
            ValidatedProfile dropped = profile.withProfile(withoutIt);
            String oldValue = constraint.getValue() != null ? format(constraint.getValue()) : String.join(",", constraint.getValues());
            Integer specId = snapshot.specId(constraint.getSpec());
            Double next = null;
            if (specId != null && constraint.getValue() != null && ("GTE".equals(constraint.getOp()) || "LTE".equals(constraint.getOp()))) {
                boolean lower = "GTE".equals(constraint.getOp());
                for (ComponentCandidateDTO candidate : candidatePoolBuilder.build(snapshot, dropped, budget).of(constraint.getRole())) {
                    Integer value = snapshot.effectiveInt(candidate, specId);
                    if (value == null) continue;
                    boolean steps = lower ? value < constraint.getValue() : value > constraint.getValue();
                    if (steps && (next == null || (lower ? value > next : value < next))) next = value.doubleValue();
                }
            }
            boolean unsatisfiable = Boolean.TRUE.equals(constraint.getUnsatisfiable());
            if (next == null) {
                return new Relaxation(constraint.getId(), unsatisfiable, dropped, oldValue + "→bỏ");
            }
            RequirementProfileDto stepped = copy(profile.profile());
            for (ProfileConstraintDto item : stepped.getConstraints()) {
                if (item.getId().equals(constraint.getId())) {
                    item.setValue(next);
                    item.setUnsatisfiable(null);
                    item.setLabel(item.getLabel() != null ? item.getLabel().replace(format(constraint.getValue()), format(next)) : null);
                }
            }
            return new Relaxation(constraint.getId(), unsatisfiable, profile.withProfile(stepped), oldValue + "→" + format(next));
        }

        // shortcut: metric GTE hạ bằng cách giảm một nửa (DD chưa định nghĩa bậc cho metric); LTE/EQ thì bỏ mục
        private Relaxation relaxMetric(ProfileMetricDto metric) {
            RequirementProfileDto changed = copy(profile.profile());
            boolean unsatisfiable = Boolean.TRUE.equals(metric.getUnsatisfiable());
            String oldValue = format(metric.getValue());
            if ("GTE".equals(metric.getOp()) && metric.getValue() != null && metric.getValue() >= 2) {
                double next = Math.floor(metric.getValue() / 2);
                for (ProfileMetricDto item : changed.getMetrics()) {
                    if (item.getId().equals(metric.getId())) {
                        item.setValue(next);
                        item.setUnsatisfiable(null);
                        item.setLabel(item.getLabel() != null ? item.getLabel().replace(oldValue, format(next)) : null);
                    }
                }
                return new Relaxation(metric.getId(), unsatisfiable, profile.withProfile(changed), oldValue + "→" + format(next));
            }
            changed.getMetrics().removeIf(item -> item.getId().equals(metric.getId()));
            return new Relaxation(metric.getId(), unsatisfiable, profile.withProfile(changed), oldValue + "→bỏ");
        }

        private BuildRuleException noFeasibleBuild(ValidatedProfile original, BigDecimal floorFirst) {
            RequirementProfileDto priorityOne = copy(original.profile());
            priorityOne.getConstraints().removeIf(item -> !Objects.equals(item.getPriority(), 1));
            priorityOne.getMetrics().removeIf(item -> !Objects.equals(item.getPriority(), 1));
            priorityOne.getRequiredRoles().removeIf(item -> !Objects.equals(item.getPriority(), 1));
            ValidatedProfile onlyPriorityOne = original.withProfile(priorityOne);
            List<BuildLine> cheapest = cheapestValidBuild(onlyPriorityOne, candidatePoolBuilder.build(snapshot, onlyPriorityOne, null));
            BigDecimal minPriority1Price = cheapest != null ? totalOf(cheapest) : null;

            List<Map<String, String>> blocking = new ArrayList<>();
            priorityOne.getConstraints().forEach(item -> blocking.add(Map.of("id", item.getId(), "reason", reasonOf(item.getReason(), item.getLabel()))));
            priorityOne.getMetrics().forEach(item -> blocking.add(Map.of("id", item.getId(), "reason", reasonOf(item.getReason(), item.getLabel()))));
            priorityOne.getRequiredRoles().forEach(item -> blocking.add(Map.of("id", "required:" + item.getRole(), "reason", reasonOf(item.getReason(), item.getRole()))));

            String reasons = blocking.stream().map(item -> item.get("reason")).collect(Collectors.joining(", "));
            String message = "Không ráp được máy" + (reasons.isEmpty() ? "" : " thỏa '" + reasons + "'") + " trong ngân sách này. "
                    + (minPriority1Price != null ? "Mức tối thiểu là " + vnd(minPriority1Price) : "Kho chưa có linh kiện thỏa yêu cầu này");
            Map<String, Object> data = new HashMap<>();
            data.put("floorPrice", floorFirst);
            data.put("minPriority1Price", minPriority1Price);
            data.put("blockingItems", blocking);
            return new BuildRuleException("NO_FEASIBLE_BUILD", HttpStatus.UNPROCESSABLE_ENTITY, message, data);
        }

        // ---------- Pha 2: các bước nâng cấp (DD 6.4.2) ----------

        private List<List<BuildLine>> upgradeMoves(List<BuildLine> current) {
            List<List<BuildLine>> moves = new ArrayList<>();
            for (String role : SCORED_ROLES) {
                if (profile.weight(role) <= 0 && !"psu".equals(role)) continue;
                List<BuildLine> roleLines = linesOf(current, role);
                if (roleLines.isEmpty()) continue;
                BuildLine target = roleLines.get(0);
                double targetScore = partScorer.partScore(role, target.part(), ctx);
                for (ComponentCandidateDTO candidate : pool.of(role)) {
                    if (candidate.getPrice().compareTo(target.part().getPrice()) <= 0) continue;
                    if (partScorer.partScore(role, candidate, ctx) <= targetScore + 1e-9) continue;
                    addReplacements(moves, current, target, candidate);
                }
            }
            moves.addAll(platformSwaps(current, true));
            addQuantityMoves(moves, current);
            return moves;
        }

        private void addQuantityMoves(List<List<BuildLine>> moves, List<BuildLine> current) {
            BuildLine ram = first(current, "ram");
            if (ram != null && ram.quantity() < MAX_QUANTITY) {
                moves.add(replaceLine(current, ram, new BuildLine("ram", ram.part(), ram.quantity() + 1)));
            }
            if (profile.weight("ssd") > 0) {
                for (ComponentCandidateDTO ssd : pool.of("ssd")) {
                    BuildLine existing = linesOf(current, "ssd").stream()
                            .filter(line -> line.part().getVariantId().equals(ssd.getVariantId())).findFirst().orElse(null);
                    if (existing != null) {
                        if (existing.quantity() < MAX_QUANTITY) {
                            moves.add(replaceLine(current, existing, new BuildLine("ssd", ssd, existing.quantity() + 1)));
                        }
                    } else {
                        List<BuildLine> added = new ArrayList<>(current);
                        added.add(new BuildLine("ssd", ssd, 1));
                        moves.add(added);
                    }
                }
            }
            BuildLine gpu = first(current, "gpu");
            boolean gpuCountBlocked = profile.metrics().stream().anyMatch(metric -> MetricKind.GPU_COUNT.name().equals(metric.getMetric())
                    && ("EQ".equals(metric.getOp()) || "LTE".equals(metric.getOp())));
            if (profile.weight("gpu") > 0 && !profile.isExcluded("gpu")) {
                if (gpu != null && !gpuCountBlocked && gpu.quantity() < MAX_QUANTITY) {
                    moves.add(replaceLine(current, gpu, new BuildLine("gpu", gpu.part(), gpu.quantity() + 1)));
                }
                if (gpu == null) {
                    for (ComponentCandidateDTO candidate : pool.of("gpu")) {
                        List<BuildLine> added = new ArrayList<>(current);
                        added.add(new BuildLine("gpu", candidate, 1));
                        moves.add(added);
                    }
                }
            }
            if (first(current, "cooler") == null && profile.weight("cooler") > 0) {
                int cpuCount = Math.max(1, quantity(current, "cpu"));
                for (ComponentCandidateDTO cooler : pool.of("cooler")) {
                    List<BuildLine> added = new ArrayList<>(current);
                    added.add(new BuildLine("cooler", cooler, cpuCount));
                    moves.add(added);
                }
            }
        }

        /** Đổi nền tảng sang socket khác: ghép nền tảng mới rẻ nhất rồi giữ lại các món cũ còn dùng được. */
        private List<List<BuildLine>> platformSwaps(List<BuildLine> current, boolean onlyBetterCpu) {
            List<List<BuildLine>> moves = new ArrayList<>();
            BuildLine currentCpu = first(current, "cpu");
            if (currentCpu == null) return moves;
            String currentSocket = snapshot.parsed(currentCpu.part()).raw().get(SOCKET);
            double currentScore = partScorer.partScore("cpu", currentCpu.part(), ctx);
            Needs needs = Needs.of(profile);
            for (ComponentCandidateDTO cpu : pool.of("cpu")) {
                ParsedSpecs cpuSpecs = snapshot.parsed(cpu);
                if (equalsIgnoreCase(cpuSpecs.raw().get(SOCKET), currentSocket)) continue;
                if (onlyBetterCpu && partScorer.partScore("cpu", cpu, ctx) <= currentScore + 1e-9) continue;
                List<BuildLine> platform = null;
                for (ComponentCandidateDTO board : pool.of("mainboard").stream()
                        .sorted(Comparator.comparing(ComponentCandidateDTO::getPrice).thenComparing(ComponentCandidateDTO::getVariantId)).toList()) {
                    platform = assemble(cpu, cpuSpecs, board, needs);
                    if (platform != null) break;
                }
                if (platform == null) continue;
                for (String role : List.of("gpu", "ssd", "ram", "cooler", "case")) {
                    List<BuildLine> kept = linesOf(current, role);
                    if (kept.isEmpty()) continue;
                    List<BuildLine> trial = new ArrayList<>(platform.stream().filter(line -> !role.equals(line.role())).toList());
                    trial.addAll(kept);
                    List<BuildLine> fixed = psuSizer.fix(trial, ctx);
                    if (fixed != null && satisfiesProfile(fixed) && isCompatible(fixed)) platform = fixed;
                }
                moves.add(platform);
            }
            return moves;
        }

        // ---------- Pha 3: hoán đổi (DD 6.4.3) ----------

        private List<List<BuildLine>> neighbours(List<BuildLine> current) {
            List<List<BuildLine>> moves = new ArrayList<>();
            List<String> roles = SCORED_ROLES.stream().filter(role -> profile.weight(role) > 0).toList();
            // SWAP1: mọi ứng viên cùng role (đắt hơn hoặc rẻ hơn)
            for (String role : roles) {
                BuildLine target = first(current, role);
                if (target == null) continue;
                for (ComponentCandidateDTO candidate : pool.of(role)) {
                    if (!candidate.getVariantId().equals(target.part().getVariantId())) {
                        addReplacements(moves, current, target, candidate);
                    }
                }
            }
            // SWAP2: đổi đồng thời 2 role (một lên, một xuống hoặc cả hai lên). Mỗi role tối đa 15 ứng viên điểm cao nhất
            // còn vừa ngân sách; role nhiều đơn vị (ram, gpu, ssd) có thêm lựa chọn "giảm số lượng" để giải phóng tiền
            BigDecimal total = totalOf(current);
            Map<String, List<List<BuildLine>>> alternatives = new HashMap<>();
            for (String role : roles) {
                BuildLine target = first(current, role);
                if (target == null) continue;
                BigDecimal room = budget.subtract(total).add(target.part().getPrice().multiply(BigDecimal.valueOf(target.quantity())));
                List<List<BuildLine>> options = new ArrayList<>();
                pool.of(role).stream()
                        .filter(candidate -> !candidate.getVariantId().equals(target.part().getVariantId()))
                        .filter(candidate -> candidate.getPrice().multiply(BigDecimal.valueOf(target.quantity())).compareTo(room) <= 0)
                        .limit(SWAP2_TOP)
                        .forEach(candidate -> options.add(List.of(new BuildLine(role, candidate, target.quantity()))));
                options.addAll(reductions(current, role));
                if (!options.isEmpty()) alternatives.put(role, options);
            }
            List<String> swappable = roles.stream().filter(alternatives::containsKey).toList();
            for (int first = 0; first < swappable.size(); first++) {
                for (int second = first + 1; second < swappable.size(); second++) {
                    String roleA = swappable.get(first);
                    String roleB = swappable.get(second);
                    for (List<BuildLine> optionA : alternatives.get(roleA)) {
                        List<BuildLine> withA = withRole(current, roleA, optionA);
                        for (List<BuildLine> optionB : alternatives.get(roleB)) {
                            moves.add(withRole(withA, roleB, optionB));
                        }
                    }
                }
            }
            // QTY_SHIFT: đổi kit RAM/SSD sang kit khác với số lượng khác
            addQuantityShifts(moves, current);
            // UPGRADE_REPAIR: nâng 1 role rồi hạ dần các role khác cho vừa B (thoát cực trị cục bộ của pha 2)
            moves.addAll(upgradesWithRepair(current, roles));
            // PLATFORM_SWAP: nhận cả nền tảng rẻ hơn nếu phần tiết kiệm làm U tăng
            moves.addAll(platformSwaps(current, false));
            return moves;
        }

        /**
         * Pha 2 mua bước "đáng tiền nhất" trước nên có thể kẹt: muốn lên GPU đắt phải bớt cùng lúc nhiều role khác,
         * điều SWAP2 (2 role) không làm được. Ở đây mỗi role nâng lên tối đa {@value #REPAIR_TOP} ứng viên điểm cao hơn,
         * rồi lặp: hạ role khác theo tỷ lệ (điểm mất / tiền tiết kiệm) nhỏ nhất cho tới khi tổng ≤ B.
         * Kết quả vẫn chỉ được nhận khi U tăng thật (pha 3).
         */
        private List<List<BuildLine>> upgradesWithRepair(List<BuildLine> current, List<String> roles) {
            List<List<BuildLine>> moves = new ArrayList<>();
            for (String role : roles) {
                BuildLine target = first(current, role);
                if (target == null) continue;
                double targetScore = partScorer.partScore(role, target.part(), ctx);
                List<ComponentCandidateDTO> better = pool.of(role).stream()
                        .filter(candidate -> candidate.getPrice().compareTo(target.part().getPrice()) > 0)
                        .filter(candidate -> partScorer.partScore(role, candidate, ctx) > targetScore + 1e-9)
                        .limit(REPAIR_TOP).toList();
                for (ComponentCandidateDTO candidate : better) {
                    // Nguồn chỉnh ngay sau khi nâng để tổng tiền tính cả phần nguồn phải lên theo (ví dụ GPU mạnh hơn)
                    List<BuildLine> upgraded = psuSizer.fix(replaceLine(current, target, new BuildLine(role, candidate, target.quantity())), ctx);
                    if (upgraded == null) continue;
                    List<BuildLine> repaired = repairToBudget(upgraded, role);
                    if (repaired != null) moves.add(repaired);
                }
            }
            return moves;
        }

        private List<BuildLine> repairToBudget(List<BuildLine> lines, String lockedRole) {
            List<BuildLine> current = lines;
            for (int step = 0; step < REPAIR_STEPS && totalOf(current).compareTo(budget) > 0; step++) {
                double currentUtility = utility(current);
                BigDecimal currentTotal = totalOf(current);
                List<BuildLine> bestStep = null;
                double bestLossPerMoney = Double.POSITIVE_INFINITY;
                for (String role : SCORED_ROLES) {
                    if (role.equals(lockedRole)) continue;
                    BuildLine target = first(current, role);
                    if (target == null) continue;
                    List<List<BuildLine>> options = new ArrayList<>(reductions(current, role));
                    pool.of(role).stream()
                            .filter(candidate -> candidate.getPrice().compareTo(target.part().getPrice()) < 0)
                            .limit(REPAIR_TOP)
                            .forEach(candidate -> options.add(List.of(new BuildLine(role, candidate, target.quantity()))));
                    for (List<BuildLine> option : options) {
                        List<BuildLine> next = psuSizer.fix(withRole(current, role, option), ctx);
                        if (next == null) continue;
                        double saved = currentTotal.subtract(totalOf(next)).doubleValue();
                        if (saved <= 0 || !satisfiesProfile(next)) continue;
                        double lossPerMoney = (currentUtility - utility(next)) / saved;
                        if (lossPerMoney < bestLossPerMoney) {
                            bestLossPerMoney = lossPerMoney;
                            bestStep = next;
                        }
                    }
                }
                if (bestStep == null) return null;
                current = bestStep;
            }
            return totalOf(current).compareTo(budget) <= 0 ? current : null;
        }

        /** Các cách giảm số đơn vị của role (dòng thứ 2 trở đi bị bỏ; dòng đầu giảm 1 hoặc về 1), dùng trong SWAP2. */
        private List<List<BuildLine>> reductions(List<BuildLine> current, String role) {
            List<List<BuildLine>> options = new ArrayList<>();
            if (!List.of("ram", "gpu", "ssd").contains(role)) return options;
            List<BuildLine> roleLines = linesOf(current, role);
            if (roleLines.isEmpty()) return options;
            BuildLine head = roleLines.get(0);
            if (roleLines.size() > 1) {
                options.add(List.of(head));
            }
            if (head.quantity() > 1) {
                List<BuildLine> minusOne = new ArrayList<>(roleLines);
                minusOne.set(0, new BuildLine(role, head.part(), head.quantity() - 1));
                options.add(minusOne);
                if (head.quantity() > 2) {
                    options.add(List.of(new BuildLine(role, head.part(), 1)));
                }
            }
            return options;
        }

        private void addQuantityShifts(List<List<BuildLine>> moves, List<BuildLine> current) {
            Map<MetricKind, Integer> metrics = BuildMetrics.of(current, snapshot);
            Needs needs = Needs.of(profile);
            BuildLine ram = first(current, "ram");
            if (ram != null) {
                for (ComponentCandidateDTO candidate : pool.of("ram")) {
                    int capacity = snapshot.parsed(candidate).ints().getOrDefault(CAPACITY_GB, 0);
                    if (capacity <= 0) continue;
                    for (int quantity : distinct(Math.max(1, ceilDiv(needs.ramGb(), capacity)),
                            Math.max(1, ceilDiv(metrics.get(MetricKind.TOTAL_RAM_GB), capacity)))) {
                        if (quantity <= MAX_QUANTITY && (quantity != ram.quantity() || !candidate.getVariantId().equals(ram.part().getVariantId()))) {
                            moves.add(replaceLine(current, ram, new BuildLine("ram", candidate, quantity)));
                        }
                    }
                }
            }
            List<BuildLine> ssds = linesOf(current, "ssd");
            if (!ssds.isEmpty()) {
                for (ComponentCandidateDTO candidate : pool.of("ssd")) {
                    int capacity = snapshot.parsed(candidate).ints().getOrDefault(CAPACITY_GB, 0);
                    if (capacity <= 0) continue;
                    for (int quantity : distinct(Math.max(1, ceilDiv(needs.storageGb(), capacity)),
                            Math.max(1, ceilDiv(metrics.get(MetricKind.TOTAL_STORAGE_GB), capacity)))) {
                        if (quantity > MAX_QUANTITY) continue;
                        List<BuildLine> shifted = new ArrayList<>(current.stream().filter(line -> !"ssd".equals(line.role())).toList());
                        shifted.add(new BuildLine("ssd", candidate, quantity));
                        moves.add(shifted);
                    }
                }
            }
        }

        // ---------- Kết luận ----------

        // CATALOG_CEILING: mọi role có trọng số đã dùng món điểm cao nhất pool và không còn bước ADD_* hợp lệ (DD 6.6)
        private boolean isCatalogCeiling(List<BuildLine> current) {
            for (String role : SCORED_ROLES) {
                if (profile.weight(role) <= 0 || profile.isExcluded(role) || pool.of(role).isEmpty()) continue;
                List<BuildLine> roleLines = linesOf(current, role);
                if (roleLines.isEmpty()) return false;
                double best = partScorer.partScore(role, pool.of(role).get(0), ctx);
                for (BuildLine line : roleLines) {
                    if (partScorer.partScore(role, line.part(), ctx) < best - 1e-9) return false;
                }
            }
            List<List<BuildLine>> additions = new ArrayList<>();
            addQuantityMoves(additions, current);
            for (List<BuildLine> addition : additions) {
                List<BuildLine> fixed = psuSizer.fix(addition, ctx);
                if (fixed != null && totalOf(fixed).compareTo(budget) <= 0 && satisfiesProfile(fixed) && isCompatible(fixed)) {
                    return false;
                }
            }
            return true;
        }

        // ---------- Kiểm tra ----------

        /** Metric cứng và requiredRoles; constraint đã được pool bảo đảm. */
        private boolean satisfiesProfile(List<BuildLine> lines) {
            Map<MetricKind, Integer> metrics = BuildMetrics.of(lines, snapshot);
            for (ProfileMetricDto metric : profile.metrics()) {
                if (!ProfileMatcher.matches(metric, metrics)) return false;
            }
            for (ProfileRoleRefDto required : profile.profile().getRequiredRoles()) {
                if (quantity(lines, required.getRole()) == 0) return false;
            }
            return true;
        }

        // Không truyền ngân sách: mọi pha đã tự kiểm total ≤ B; bộ rẻ nhất (floorPrice, minPriority1Price) được phép > B
        private boolean isCompatible(List<BuildLine> lines) {
            return compatibilityEngine.evaluate(lines, new EvaluationContext(null, EvaluationContext.Mode.SOLVER, snapshot)).compatible();
        }

        private double utility(List<BuildLine> lines) {
            return partScorer.utility(lines, ctx);
        }

        private void addReplacements(List<List<BuildLine>> moves, List<BuildLine> current, BuildLine target, ComponentCandidateDTO candidate) {
            moves.add(replaceLine(current, target, new BuildLine(target.role(), candidate, target.quantity())));
            if ("ram".equals(target.role())) {
                // RAM: thử thêm số lượng tối thiểu để đạt metric TOTAL_RAM_GB với kit mới
                int capacity = snapshot.parsed(candidate).ints().getOrDefault(CAPACITY_GB, 0);
                int minimum = capacity > 0 ? Math.max(1, ceilDiv(Needs.of(profile).ramGb(), capacity)) : target.quantity();
                if (minimum != target.quantity() && minimum <= MAX_QUANTITY) {
                    moves.add(replaceLine(current, target, new BuildLine("ram", candidate, minimum)));
                }
            }
        }
    }

    // ---------- Nhu cầu tối thiểu từ hồ sơ (DD 6.4.1 bước 0) ----------

    private record Needs(int cpu, int maxCpu, int cores, int ramGb, int gpu, int maxGpu, int vramGb, int storageGb,
                         boolean gpuRequired) {

        static Needs of(ValidatedProfile profile) {
            int gpu = Math.max(value(profile, MetricKind.GPU_COUNT, "GTE", 0), value(profile, MetricKind.GPU_COUNT, "EQ", 0));
            int vram = value(profile, MetricKind.TOTAL_VRAM_GB, "GTE", 0);
            return new Needs(
                    Math.max(1, Math.max(value(profile, MetricKind.CPU_COUNT, "GTE", 1), value(profile, MetricKind.CPU_COUNT, "EQ", 1))),
                    Math.min(value(profile, MetricKind.CPU_COUNT, "LTE", Integer.MAX_VALUE), value(profile, MetricKind.CPU_COUNT, "EQ", Integer.MAX_VALUE)),
                    value(profile, MetricKind.TOTAL_CPU_CORES, "GTE", 0),
                    Math.max(value(profile, MetricKind.TOTAL_RAM_GB, "GTE", 0), value(profile, MetricKind.TOTAL_RAM_GB, "EQ", 0)),
                    gpu,
                    Math.min(value(profile, MetricKind.GPU_COUNT, "LTE", Integer.MAX_VALUE), value(profile, MetricKind.GPU_COUNT, "EQ", Integer.MAX_VALUE)),
                    vram,
                    value(profile, MetricKind.TOTAL_STORAGE_GB, "GTE", 0),
                    profile.isRequired("gpu") || gpu >= 1 || vram > 0);
        }

        private static int value(ValidatedProfile profile, MetricKind kind, String op, int fallback) {
            return profile.metrics().stream()
                    .filter(metric -> kind.name().equals(metric.getMetric()) && op.equals(metric.getOp()) && metric.getValue() != null)
                    .map(metric -> metric.getValue().intValue())
                    .findFirst().orElse(fallback);
        }
    }

    private record Selection(ComponentCandidateDTO part, int quantity) {
        BigDecimal cost() {
            return part.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }

    // Tổng price × q nhỏ nhất; hòa thì q nhỏ hơn; hòa nữa thì variantId nhỏ hơn
    private static Selection cheaper(Selection current, Selection candidate) {
        if (current == null) return candidate;
        int byCost = candidate.cost().compareTo(current.cost());
        if (byCost != 0) return byCost < 0 ? candidate : current;
        if (candidate.quantity() != current.quantity()) return candidate.quantity() < current.quantity() ? candidate : current;
        return candidate.part().getVariantId() < current.part().getVariantId() ? candidate : current;
    }

    // ---------- Tiện ích ----------

    static BigDecimal totalOf(List<BuildLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (BuildLine line : lines) {
            if (line != null && line.part() != null && line.part().getPrice() != null) {
                total = total.add(line.part().getPrice().multiply(BigDecimal.valueOf(line.quantity())));
            }
        }
        return total;
    }

    private static List<BuildLine> ordered(List<BuildLine> lines) {
        return lines.stream().sorted(Comparator
                .comparing((BuildLine line) -> ROLE_ORDER.indexOf(line.role()))
                .thenComparing(line -> line.part().getVariantId())).toList();
    }

    private static List<BuildLine> replaceLine(List<BuildLine> lines, BuildLine target, BuildLine replacement) {
        List<BuildLine> result = new ArrayList<>(lines.size());
        boolean replaced = false;
        for (BuildLine line : lines) {
            if (!replaced && line == target) {
                result.add(replacement);
                replaced = true;
            } else {
                result.add(line);
            }
        }
        return result;
    }

    /** Thay toàn bộ dòng của role bằng các dòng mới. */
    private static List<BuildLine> withRole(List<BuildLine> lines, String role, List<BuildLine> replacement) {
        List<BuildLine> result = new ArrayList<>(lines.stream().filter(line -> !role.equals(line.role())).toList());
        result.addAll(replacement);
        return result;
    }

    private static List<BuildLine> linesOf(List<BuildLine> lines, String role) {
        return lines.stream().filter(line -> role.equals(line.role())).toList();
    }

    private static BuildLine first(List<BuildLine> lines, String role) {
        return lines.stream().filter(line -> role.equals(line.role())).findFirst().orElse(null);
    }

    private static int quantity(List<BuildLine> lines, String role) {
        return lines.stream().filter(line -> role.equals(line.role())).mapToInt(BuildLine::quantity).sum();
    }

    private static long idSum(List<BuildLine> lines) {
        return lines.stream().mapToLong(line -> line.part().getVariantId()).sum();
    }

    private static boolean isCheaper(ComponentCandidateDTO candidate, ComponentCandidateDTO current) {
        int byPrice = candidate.getPrice().compareTo(current.getPrice());
        return byPrice < 0 || (byPrice == 0 && candidate.getVariantId() < current.getVariantId());
    }

    private static int compareCost(BigDecimal first, BigDecimal second) {
        if (first == null && second == null) return 0;
        if (first == null) return 1;
        if (second == null) return -1;
        return first.compareTo(second);
    }

    private static int ceilDiv(int value, int divisor) {
        return value <= 0 ? 0 : (value + divisor - 1) / divisor;
    }

    private static List<Integer> distinct(int first, int second) {
        return first == second ? List.of(first) : List.of(first, second);
    }

    private static boolean equalsIgnoreCase(String first, String second) {
        return first != null && first.equalsIgnoreCase(second);
    }

    private static RequirementProfileDto copy(RequirementProfileDto profile) {
        return profile.toBuilder()
                .constraints(profile.getConstraints().stream().map(item -> item.toBuilder().build()).collect(Collectors.toCollection(ArrayList::new)))
                .metrics(profile.getMetrics().stream().map(item -> item.toBuilder().build()).collect(Collectors.toCollection(ArrayList::new)))
                .requiredRoles(new ArrayList<>(profile.getRequiredRoles()))
                .build();
    }

    private static String reasonOf(String reason, String fallback) {
        return reason != null && !reason.isBlank() ? reason : fallback;
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static String vnd(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount.setScale(0, RoundingMode.HALF_UP)) + " ₫";
    }
}
