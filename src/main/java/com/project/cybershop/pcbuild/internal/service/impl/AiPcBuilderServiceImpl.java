package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.AiBuildMetadataResponse;
import com.project.cybershop.pcbuild.api.dto.response.AiBuildResponse;
import com.project.cybershop.pcbuild.api.dto.response.BudgetFitResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildOptionsResponse;
import com.project.cybershop.pcbuild.api.dto.response.SelectedPartResponse;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildTier;
import com.project.cybershop.pcbuild.internal.model.CandidatePool;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.EngineReport;
import com.project.cybershop.pcbuild.internal.model.EvaluationContext;
import com.project.cybershop.pcbuild.internal.model.MetricKind;
import com.project.cybershop.pcbuild.internal.model.SolverInput;
import com.project.cybershop.pcbuild.internal.model.SolverResult;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.service.AiPcBuilderService;
import com.project.cybershop.pcbuild.internal.service.BuildExplainer;
import com.project.cybershop.pcbuild.internal.service.BuildSolver;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.service.ProfileExtractor;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import com.project.cybershop.pcbuild.internal.service.ProfileValidator;
import com.project.cybershop.pcbuild.internal.service.RequestValidator;
import com.project.cybershop.pcbuild.internal.support.BuildMetrics;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import com.project.cybershop.pcbuild.internal.support.PeripheralSelector;
import com.project.cybershop.pcbuild.internal.support.ProfileMatcher;
import com.project.cybershop.pcbuild.internal.support.TextNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Orchestrator /ai-suggest (DD 3.3). Không @Transactional (B06, DD 8.6): mọi lần đọc DB nằm trong lần nạp snapshot,
 * kết thúc trước khi gọi AI. "Java chọn, AI giải thích": AI chỉ trích hồ sơ và viết lời giải thích.
 */
@Slf4j
@Service
public class AiPcBuilderServiceImpl implements AiPcBuilderService {

    private static final List<String> EXAMPLES = List.of(
            "Chơi game 2K, thỉnh thoảng stream",
            "Dựng phim 4K bằng DaVinci Resolve",
            "Học và chạy thử model AI nhỏ",
            "Văn phòng, Excel, họp online",
            "Lập trình web, chạy Docker");
    private static final BigDecimal WINDOW_MIN = new BigDecimal("0.90");
    // So trên văn bản đã bỏ dấu ("đắt tiền nhất" → "dat tien nhat")
    private static final Pattern MOST_EXPENSIVE = Pattern.compile(
            "\\b(dat|mac)( tien| do)? nhat\\b|\\bgia cao nhat\\b|\\bmost expensive\\b|\\bpriciest\\b");

    private final CandidateRepositoryFacade candidateRepositoryFacade;
    private final RequestValidator requestValidator;
    private final ProfileExtractor profileExtractor;
    private final ProfilePresets profilePresets;
    private final ProfileValidator profileValidator;
    private final BuildSolver buildSolver;
    private final BuildExplainer buildExplainer;
    private final CompatibilityEngine compatibilityEngine;
    private final Clock clock;
    private final long requestDeadlineMs;
    private final double needsUnclearConfidence;
    private final String provider;
    private final String model;

    public AiPcBuilderServiceImpl(CandidateRepositoryFacade candidateRepositoryFacade, RequestValidator requestValidator,
                                  ProfileExtractor profileExtractor, ProfilePresets profilePresets,
                                  ProfileValidator profileValidator, BuildSolver buildSolver, BuildExplainer buildExplainer,
                                  CompatibilityEngine compatibilityEngine, Clock clock,
                                  @Value("${app.ai.request-deadline-ms:14000}") long requestDeadlineMs,
                                  @Value("${app.pcbuild.needs-unclear-confidence:0.4}") double needsUnclearConfidence,
                                  @Value("${app.ai.provider:gemini}") String provider,
                                  @Value("${app.ai.model:gemini-3.5-flash-lite}") String model) {
        this.candidateRepositoryFacade = candidateRepositoryFacade;
        this.requestValidator = requestValidator;
        this.profileExtractor = profileExtractor;
        this.profilePresets = profilePresets;
        this.profileValidator = profileValidator;
        this.buildSolver = buildSolver;
        this.buildExplainer = buildExplainer;
        this.compatibilityEngine = compatibilityEngine;
        this.clock = clock;
        this.requestDeadlineMs = requestDeadlineMs;
        this.needsUnclearConfidence = needsUnclearConfidence;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public AiBuildResponse processAiBuild(AiBuildRequest request, Integer currentUserId) {
        Instant started = clock.instant();
        // Một deadline chung cho mọi lời gọi AI của request (DD 8.5.1)
        Instant deadline = started.plusMillis(requestDeadlineMs);
        String requestId = UUID.randomUUID().toString();

        CompatibilitySnapshot snapshot = candidateRepositoryFacade.snapshot();   // đúng một lần cho cả request
        requestValidator.validate(request, buildSolver.floorPrice(snapshot));
        BigDecimal budget = request.getBudget();
        BuildTier tier = BuildTier.of(budget);

        // Hồ sơ: khách gửi (không gọi AI) → cache → AI
        long profileStarted = System.nanoTime();
        RequirementProfileDto draft;
        String extractStatus;
        String source;
        if (request.getProfile() != null) {
            draft = request.getProfile();
            extractStatus = "USER_PROFILE";
            source = ProfileValidator.SOURCE_USER_EDITED;
        } else {
            ProfileExtractor.Extraction extraction = profileExtractor.extract(request.getPurpose(), request.getUserPrompt(),
                    budget, tier, snapshot, deadline);
            draft = extraction.draft();
            // Chế độ này làm khách tốn thêm rất nhiều tiền nên không chỉ tin AI: lời khách phải thật sự nhắc tới "đắt nhất"
            if (!MOST_EXPENSIVE.matcher(TextNormalizer.normalize(request.getPurpose() + " " + request.getUserPrompt())).find()) {
                draft.setObjective(null);
            }
            extractStatus = extraction.status();
            source = ProfileValidator.SOURCE_AI;
        }
        ValidatedProfile profile = profileValidator.validate(profilePresets.fillMissing(draft, tier), snapshot.dictionary(),
                source, request.getRamGb());
        long profileMs = elapsedMs(profileStarted);
        if (ProfileValidator.SOURCE_AI.equals(source)) {
            rejectIfUnclear(profile);
        }
        if (!profile.profile().getDroppedItems().isEmpty()) {
            log.info("ai-suggest requestId={} droppedItems={}", requestId, profile.profile().getDroppedItems());
        }

        // Java chọn bộ máy (tất định, tổng ≤ B)
        long solverStarted = System.nanoTime();
        CandidatePool pool = candidateRepositoryFacade.buildPool(snapshot, profile, budget);
        SolverResult result = buildSolver.solve(new SolverInput(budget, profile, pool, snapshot));
        long solverMs = elapsedMs(solverStarted);

        // AI giải thích; lỗi thì 503, không trả bộ máy chưa có lời giải thích [ĐÃ CHỐT]
        long explainStarted = System.nanoTime();
        BuildExplainer.Explanation explanation = buildExplainer.explain(result.profile(), result, budget,
                request.getPurpose(), request.getUserPrompt(), snapshot, deadline, request.getFullSetBudget());
        long explainMs = elapsedMs(explainStarted);

        // Kiểm lần cuối với hồ sơ gốc: mục đã hạ hiện thành warning P:*
        EngineReport report = compatibilityEngine.evaluate(result.lines(),
                new EvaluationContext(budget, profile, EvaluationContext.Mode.USER, snapshot));

        long totalMs = Duration.between(started, clock.instant()).toMillis();
        Map<String, Long> latency = new LinkedHashMap<>();
        latency.put("profile", profileMs);
        latency.put("solver", solverMs);
        latency.put("explain", explainMs);
        latency.put("total", totalMs);

        List<String> warnings = new ArrayList<>(profile.warnings());
        warnings.addAll(result.warnings());
        // DD 6.5, Phụ lục B: nguồn cần > 1600 W (check J4b) được nêu thêm bằng mã ổn định để FE hiện lời nhắc ổ điện riêng
        if (report.checks().stream().anyMatch(check -> "J4b".equals(check.getKey()))) {
            warnings.add("PSU_HIGH_WATTAGE");
        }

        log.info("ai-suggest xong requestId={} user={} budget={} purposeHash={} archetype={} constraints={} metrics={} "
                        + "relaxedItems={} total={} percent={} reasonCode={} extractStatus={} aiStatus={} latencyMs={} iterations={}",
                requestId, currentUserId != null ? currentUserId : "anon", budget, hash(request.getPurpose()),
                profile.archetype(), profile.constraints().size(), profile.metrics().size(), result.relaxedItems(),
                result.total(), percent(result.total(), budget), result.reasonCode(), extractStatus,
                explanation.aiStatus(), latency, result.iterations());

        return AiBuildResponse.builder()
                .buildId(null)
                .buildName(explanation.buildName())
                .purpose(request.getPurpose())
                .totalPrice(result.total())
                .budget(budget)
                .aiReasoning(explanation.aiReasoning())
                .performanceHighlight(explanation.performanceHighlight())
                .isCompatible(report.compatible())
                .checks(report.checks())
                .items(items(result.lines(), explanation))
                .reasonCode(result.reasonCode())
                .budgetFit(budgetFit(result, budget))
                .profile(withSatisfaction(profile, result.lines(), snapshot))
                .relaxedItems(result.relaxedItems())
                .warnings(warnings)
                .capacity(report.capacity())
                .requiredPsuW(report.requiredPsuW())
                .floorPrice(result.floorPrice())
                .metadata(AiBuildMetadataResponse.builder()
                        .requestId(requestId)
                        .generatedAt(OffsetDateTime.now(clock))
                        .extractStatus(extractStatus)
                        .aiStatus(explanation.aiStatus())
                        .provider(provider)
                        .model(model)
                        .promptVersions(Map.of("profile", ProfileExtractor.PROMPT_VERSION, "explain", BuildExplainer.PROMPT_VERSION))
                        .dictionaryVersion(snapshot.dictionaryVersion())
                        .latencyMs(latency)
                        .candidateCount(pool.size())
                        .solverIterations(result.iterations())
                        .build())
                .build();
    }

    @Override
    public BuildOptionsResponse options() {
        CompatibilitySnapshot snapshot = candidateRepositoryFacade.snapshot();
        List<BuildOptionsResponse.Tier> tiers = new ArrayList<>();
        for (BuildTier tier : BuildTier.values()) {
            tiers.add(new BuildOptionsResponse.Tier(tier.name(), tier.lowerBound(), tier.upperBound()));
        }
        PeripheralSelector.BudgetRange peripherals = PeripheralSelector.budgetRange(snapshot.peripherals());
        return BuildOptionsResponse.builder()
                .peripheralMinTotal(peripherals != null ? peripherals.min() : null)
                .peripheralMaxTotal(peripherals != null ? peripherals.max() : null)
                .minBudget(requestValidator.minBudget())
                .floorPrice(buildSolver.floorPrice(snapshot))
                .maxBudget(requestValidator.maxBudget())
                .dictionaryVersion(snapshot.dictionaryVersion())
                .dictionary(snapshot.dictionary())
                .examples(EXAMPLES)
                .tiers(tiers)
                .build();
    }

    // DD 5.6: confidence < 0,4 hoặc không còn mục nào dùng được sau khi kiểm → hỏi lại khách
    private void rejectIfUnclear(ValidatedProfile profile) {
        RequirementProfileDto dto = profile.profile();
        boolean lowConfidence = dto.getConfidence() != null && dto.getConfidence() < needsUnclearConfidence;
        boolean nothingUsable = dto.getConstraints().isEmpty() && dto.getMetrics().isEmpty() && dto.getPreferences().isEmpty();
        if (lowConfidence || nothingUsable) {
            String question = dto.getClarifyingQuestion() != null && !dto.getClarifyingQuestion().isBlank()
                    ? dto.getClarifyingQuestion()
                    : "Bạn sẽ dùng máy chủ yếu cho việc gì (chơi game, làm đồ họa, học tập…)?";
            throw new BuildRuleException("NEEDS_UNCLEAR", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Nhu cầu chưa đủ rõ để đề xuất cấu hình", Map.of("clarifyingQuestion", question));
        }
    }

    private static List<SelectedPartResponse> items(List<BuildLine> lines, BuildExplainer.Explanation explanation) {
        return lines.stream().map(line -> {
            BigDecimal unitPrice = line.part().getPrice();
            String reason = explanation.partReasons().get(line.part().getVariantId());
            return SelectedPartResponse.builder()
                    .role(line.role())
                    .variantId(line.part().getVariantId())
                    .productName(line.part().getProductName())
                    .variantName(line.part().getVariantName())
                    .imageUrl(line.part().getImageUrl())
                    .unitPrice(unitPrice)
                    .quantity(line.quantity())
                    .subtotal(unitPrice.multiply(BigDecimal.valueOf(line.quantity())))
                    .specs(line.part().getSpecs())
                    .aiReason(reason != null && !reason.isBlank() ? reason : "—")
                    .build();
        }).toList();
    }

    private static BudgetFitResponse budgetFit(SolverResult result, BigDecimal budget) {
        BigDecimal ratio = result.total().divide(budget, 4, RoundingMode.HALF_UP);
        BigDecimal windowMin = budget.multiply(WINDOW_MIN).setScale(0, RoundingMode.CEILING);
        String status;
        if (result.total().compareTo(windowMin) >= 0) {
            status = "IN_WINDOW";
        } else if ("CATALOG_CEILING".equals(result.reasonCode())) {
            status = "UNDER_CEILING";
        } else if ("NEEDS_SATURATED".equals(result.reasonCode())) {
            status = "UNDER_SATURATED";
        } else if ("MAX_PRICE_REACHED".equals(result.reasonCode())) {
            status = "UNDER_MAX_PRICE";
        } else {
            status = "UNDER_RELAXED";
        }
        return BudgetFitResponse.builder()
                .ratio(ratio)
                .percent(ratio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP))
                .status(status)
                .windowMin(windowMin)
                .windowMax(budget)
                .leftover(result.leftover())
                .build();
    }

    /** Hồ sơ gốc đã kiểm, kèm cờ satisfied của từng mục trên bộ máy đề xuất (mục đã hạ có thể false). */
    private static RequirementProfileDto withSatisfaction(ValidatedProfile profile, List<BuildLine> lines, CompatibilitySnapshot snapshot) {
        RequirementProfileDto dto = profile.profile();
        Map<MetricKind, Integer> metrics = BuildMetrics.of(lines, snapshot);
        List<ProfileConstraintDto> constraints = dto.getConstraints().stream().map(constraint -> {
            List<BuildLine> roleLines = lines.stream().filter(line -> constraint.getRole().equals(line.role())).toList();
            boolean satisfied = !roleLines.isEmpty() && roleLines.stream()
                    .allMatch(line -> ProfileMatcher.matches(constraint, line.part(), snapshot));
            return constraint.toBuilder().satisfied(satisfied).build();
        }).toList();
        List<ProfileMetricDto> metricItems = dto.getMetrics().stream()
                .map(metric -> metric.toBuilder().satisfied(ProfileMatcher.matches(metric, metrics)).build())
                .toList();
        return dto.toBuilder().constraints(new ArrayList<>(constraints)).metrics(new ArrayList<>(metricItems)).build();
    }

    private static BigDecimal percent(BigDecimal total, BigDecimal budget) {
        return total.multiply(BigDecimal.valueOf(100)).divide(budget, 1, RoundingMode.HALF_UP);
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    // Không log nguyên văn nhu cầu (có thể chứa thông tin cá nhân), chỉ log hash (DD 11.5)
    private static String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(TextNormalizer.normalize(text).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return "n/a";
        }
    }
}
