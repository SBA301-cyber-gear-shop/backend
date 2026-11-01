package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;
import com.project.cybershop.pcbuild.internal.model.CandidatePool;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.ParsedSpecs;
import com.project.cybershop.pcbuild.internal.model.RuleDefinition;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.MetadataDictionaryService;
import com.project.cybershop.pcbuild.internal.service.PartScorer;
import com.project.cybershop.pcbuild.internal.service.ProfilePresets;
import com.project.cybershop.pcbuild.internal.support.BuildRuleException;
import com.project.cybershop.pcbuild.internal.support.CandidatePoolBuilder;
import com.project.cybershop.pcbuild.internal.support.PerfIndexEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class CandidateRepositoryFacadeImpl implements CandidateRepositoryFacade {

    private final CatalogPublicApi catalogPublicApi;
    private final CompatibilityRuleRepository ruleRepository;
    private final MetadataDictionaryService dictionaryService;
    private final CandidatePoolBuilder candidatePoolBuilder;
    private final Clock clock;
    private final Duration cacheTtl;
    private final Object reloadLock = new Object();

    private volatile CompatibilitySnapshot current;

    public CandidateRepositoryFacadeImpl(CatalogPublicApi catalogPublicApi,
                                         CompatibilityRuleRepository ruleRepository,
                                         MetadataDictionaryService dictionaryService,
                                         PartScorer partScorer,
                                         ProfilePresets profilePresets,
                                         Clock clock,
                                         @Value("${app.pcbuild.candidate-cache-seconds:60}") long cacheSeconds) {
        this.catalogPublicApi = catalogPublicApi;
        this.ruleRepository = ruleRepository;
        this.dictionaryService = dictionaryService;
        // Dựng pool là phép tính thuần, không phụ thuộc trạng thái của facade (giữ chữ ký constructor cho test hiện có)
        this.candidatePoolBuilder = new CandidatePoolBuilder(partScorer, profilePresets);
        this.clock = clock;
        this.cacheTtl = Duration.ofSeconds(cacheSeconds);
    }

    @Override
    public CompatibilitySnapshot snapshot() {
        CompatibilitySnapshot snapshot = current;
        if (isFresh(snapshot)) {
            return snapshot;
        }
        // Chỉ một luồng nạp lại; luồng khác chờ khóa rồi dùng bản vừa nạp
        synchronized (reloadLock) {
            snapshot = current;
            if (isFresh(snapshot)) {
                return snapshot;
            }
            try {
                CompatibilitySnapshot loaded = load();
                current = loaded;
                return loaded;
            } catch (RuntimeException e) {
                if (snapshot != null) {
                    log.warn("Nạp lại CompatibilitySnapshot lỗi, dùng tiếp bản nạp lúc {}: {}", snapshot.loadedAt(), e.getMessage());
                    return snapshot;
                }
                log.error("Không nạp được CompatibilitySnapshot lần đầu", e);
                throw new BuildRuleException("CATALOG_EMPTY", HttpStatus.SERVICE_UNAVAILABLE,
                        "Chưa tải được dữ liệu linh kiện. Vui lòng thử lại sau.", null);
            }
        }
    }

    private boolean isFresh(CompatibilitySnapshot snapshot) {
        return snapshot != null && Duration.between(snapshot.loadedAt(), clock.instant()).compareTo(cacheTtl) < 0;
    }

    private CompatibilitySnapshot load() {
        List<ComponentCandidateDTO> candidates = catalogPublicApi.getActiveCandidatesForBuild().stream()
                .filter(candidate -> candidate.getCategorySlug() != null
                        && CandidatePoolBuilder.BUILD_ROLES.contains(candidate.getCategorySlug().toLowerCase()))
                .toList();
        List<CategorySpecDto> specificationCatalog = catalogPublicApi.getSpecificationCatalog();
        List<RuleDefinition> rules = ruleRepository.findAll().stream()
                .sorted(Comparator.comparing(CompatibilityRule::getId))
                .map(CandidateRepositoryFacadeImpl::toRuleDefinition)
                .toList();

        Map<Integer, ParsedSpecs> parsedSpecsByVariantId = new HashMap<>();
        for (ComponentCandidateDTO candidate : candidates) {
            parsedSpecsByVariantId.put(candidate.getVariantId(), ParsedSpecs.of(candidate));
        }

        Map<Integer, Set<Integer>> requiredSpecIdsByCategoryId = new HashMap<>();
        Map<Integer, Map<String, String>> optionDisplayBySpecId = new HashMap<>();
        Map<Integer, String> specNameById = new HashMap<>();
        for (CategorySpecDto row : specificationCatalog) {
            if (row.isCompatibility()) {
                requiredSpecIdsByCategoryId.computeIfAbsent(row.getCategoryId(), key -> new HashSet<>())
                        .add(row.getSpecificationId());
            }
            specNameById.put(row.getSpecificationId(), row.getSpecificationName());
            Map<String, String> options = optionDisplayBySpecId.computeIfAbsent(row.getSpecificationId(), key -> new HashMap<>());
            for (CategorySpecDto.Option option : row.getOptions()) {
                options.put(option.getCode(), option.getDisplayValue());
            }
        }

        // Dựng sẵn cùng lần nạp, không truy vấn thêm: ước lượng Perf_Index (DD 6.2.1) và từ điển (DD 5.2)
        PerfIndexEstimator estimator = PerfIndexEstimator.build(
                candidates.stream().filter(candidate -> "gpu".equalsIgnoreCase(candidate.getCategorySlug())).toList(),
                parsedSpecsByVariantId);
        MetadataDictionaryResponse dictionary = dictionaryService.build(candidates, parsedSpecsByVariantId,
                specificationCatalog, estimator);

        Instant loadedAt = clock.instant();
        log.info("Đã nạp CompatibilitySnapshot: {} ứng viên, {} luật, dictionaryVersion={}",
                candidates.size(), rules.size(), dictionary.getDictionaryVersion());
        return new CompatibilitySnapshot(candidates, parsedSpecsByVariantId, rules, requiredSpecIdsByCategoryId,
                optionDisplayBySpecId, specNameById, dictionary, estimator, loadedAt);
    }

    @Override
    public CandidatePool buildPool(CompatibilitySnapshot snapshot, ValidatedProfile profile, BigDecimal budget) {
        return candidatePoolBuilder.build(snapshot, profile, budget);
    }

    private static RuleDefinition toRuleDefinition(CompatibilityRule rule) {
        return new RuleDefinition(rule.getId(), rule.getRuleName(), rule.getCategory1Id(), rule.getSpec1Id(),
                rule.getCategory2Id(), rule.getSpec2Id(), rule.getOperator());
    }
}
