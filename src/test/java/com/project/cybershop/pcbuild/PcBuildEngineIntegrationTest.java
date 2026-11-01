package com.project.cybershop.pcbuild;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.EngineReport;
import com.project.cybershop.pcbuild.internal.model.EvaluationContext;
import com.project.cybershop.pcbuild.internal.repository.CompatibilityRuleRepository;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.service.impl.CandidateRepositoryFacadeImpl;
import com.project.cybershop.shared.config.JwtUtil;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tích hợp giai đoạn 1 trên H2 với pcbuild-seed.sql (biến thể 1–118 của docs/Metadata.sql):
 * golden test của engine (DD 13.3), số câu SQL khi nạp snapshot và AC13 (0 câu SQL khi cache ấm).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pcbuild;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:purchase-schema.sql,classpath:pcbuild/pcbuild-schema.sql",
        "spring.sql.init.data-locations=classpath:pcbuild-seed.sql"
})
@AutoConfigureMockMvc
@DisplayName("PC Build - tích hợp engine + snapshot với dữ liệu thật (biến thể 1–118)")
class PcBuildEngineIntegrationTest {

    @Autowired
    private CandidateRepositoryFacade candidateRepositoryFacade;

    @Autowired
    private CompatibilityEngine compatibilityEngine;

    @Autowired
    private CatalogPublicApi catalogPublicApi;

    @Autowired
    private CompatibilityRuleRepository ruleRepository;

    @Autowired
    private com.project.cybershop.pcbuild.internal.service.MetadataDictionaryService dictionaryService;

    @Autowired
    private com.project.cybershop.pcbuild.internal.service.PartScorer partScorer;

    @Autowired
    private com.project.cybershop.pcbuild.internal.service.ProfilePresets profilePresets;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtUtil jwtUtil;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        assertThat(statistics.isStatisticsEnabled()).as("hibernate.generate_statistics phải bật để đo AC13").isTrue();
    }

    /** Bộ cấu hình mẫu DD 13.3: variantId → số lượng, tổng tiền cộng từ giá của biến thể 1–118. */
    static Stream<Arguments> goldenBuilds() {
        return Stream.of(
                golden("base7", 6_969_000, 1, 1, 30, 1, 50, 1, 80, 1, 92, 1, 102, 1),
                golden("base10", 10_178_000, 3, 1, 31, 1, 51, 1, 80, 1, 92, 1, 102, 1),
                golden("game15", 15_118_000, 17, 1, 41, 1, 51, 1, 64, 1, 80, 1, 92, 1, 103, 1, 112, 1),
                golden("game30", 30_608_000, 17, 1, 41, 1, 51, 2, 69, 1, 83, 1, 94, 1, 103, 1, 112, 1),
                golden("game60", 61_097_000, 9, 1, 36, 1, 55, 1, 72, 1, 83, 1, 95, 1, 105, 1, 113, 1),
                golden("game120", 118_837_000, 11, 1, 37, 1, 56, 1, 75, 1, 86, 1, 96, 1, 106, 1, 115, 1),
                golden("game250", 251_588_000, 10, 1, 38, 1, 57, 1, 77, 1, 86, 1, 98, 1, 109, 1, 115, 1),
                golden("game_top", 333_747_000, 11, 1, 39, 1, 59, 1, 78, 1, 90, 1, 100, 1, 110, 1, 116, 1),
                golden("gfx30", 29_508_000, 17, 1, 43, 1, 51, 2, 67, 1, 83, 1, 94, 1, 103, 1, 112, 1),
                golden("gfx60", 58_488_000, 21, 1, 43, 1, 51, 4, 70, 1, 83, 1, 82, 1, 94, 1, 105, 1, 113, 1),
                golden("gfx120", 116_938_000, 13, 1, 37, 1, 58, 1, 74, 1, 89, 1, 95, 1, 105, 1, 114, 1),
                golden("gfx250", 253_537_000, 13, 1, 37, 1, 58, 1, 77, 1, 86, 1, 98, 1, 109, 1, 114, 1),
                golden("gfx_tr600", 612_578_000, 26, 1, 48, 1, 62, 4, 77, 1, 89, 1, 99, 1, 111, 1, 117, 1),
                golden("gfx_tr1050", 1_060_028_000, 26, 1, 48, 1, 62, 8, 77, 2, 90, 1, 101, 1, 111, 1, 117, 1),
                golden("ai120", 119_488_000, 8, 1, 40, 1, 58, 1, 70, 2, 88, 1, 96, 1, 107, 1, 112, 1),
                golden("ai_pro", 1_146_988_000, 25, 1, 48, 1, 62, 4, 79, 2, 90, 1, 99, 1, 111, 1, 117, 1),
                golden("ai_epyc", 1_134_938_000, 29, 2, 49, 1, 62, 8, 79, 1, 90, 1, 101, 1, 111, 1, 118, 2),
                golden("office30", 28_967_000, 7, 1, 35, 1, 53, 1, 85, 1, 94, 1, 104, 1),
                golden("prog250", 246_819_000, 13, 1, 40, 1, 60, 1, 76, 1, 90, 1, 89, 1, 97, 1, 109, 1, 116, 1));
    }

    private static Arguments golden(String code, long total, int... variantAndQuantity) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (int index = 0; index < variantAndQuantity.length; index += 2) {
            quantities.put(variantAndQuantity[index], variantAndQuantity[index + 1]);
        }
        return Arguments.of(code, total, quantities);
    }

    private List<BuildLine> linesOf(CompatibilitySnapshot snapshot, Map<Integer, Integer> quantities) {
        List<BuildLine> lines = new ArrayList<>();
        quantities.forEach((variantId, quantity) -> {
            ComponentCandidateDTO part = snapshot.candidate(variantId)
                    .orElseThrow(() -> new AssertionError("Biến thể " + variantId + " không có trong snapshot"));
            lines.add(new BuildLine(part.getCategorySlug(), part, quantity));
        });
        return lines;
    }

    @ParameterizedTest(name = "{0}: tổng {1} ₫")
    @MethodSource("goldenBuilds")
    @DisplayName("Golden test DD 13.3: engine mode SOLVER, không truyền ngân sách → tương thích và tổng đúng")
    void goldenBuildsAreCompatible(String code, long total, Map<Integer, Integer> quantities) {
        CompatibilitySnapshot snapshot = candidateRepositoryFacade.snapshot();

        EngineReport report = compatibilityEngine.evaluate(linesOf(snapshot, quantities),
                new EvaluationContext(null, EvaluationContext.Mode.SOLVER, snapshot));

        String failures = report.checks().stream()
                .filter(check -> "fail".equals(check.getStatus()))
                .map(check -> check.getKey() + " " + check.getDetail())
                .collect(Collectors.joining("; "));
        assertThat(report.compatible()).as(code + " fail: " + failures).isTrue();
        assertThat(report.total()).as(code).isEqualByComparingTo(BigDecimal.valueOf(total));
    }

    @Test
    @DisplayName("Nạp snapshot lạnh dùng ≤ 10 câu SQL và đọc đủ 12 luật, thông số bắt buộc của 8 danh mục")
    void coldSnapshotLoadUsesFewStatements() {
        CandidateRepositoryFacade freshFacade = new CandidateRepositoryFacadeImpl(
                catalogPublicApi, ruleRepository, dictionaryService, partScorer, profilePresets, Clock.systemUTC(), 60);
        statistics.clear();

        CompatibilitySnapshot snapshot = freshFacade.snapshot();

        assertThat(statistics.getPrepareStatementCount()).isBetween(1L, 10L);
        assertThat(snapshot.rules()).hasSize(12);
        assertThat(snapshot.candidates()).hasSize(117); // 118 biến thể trừ EPYC 9575F hết hàng
        assertThat(snapshot.requiredSpecIds(7)).containsExactlyInAnyOrder(16, 17, 18, 59);
        assertThat(snapshot.display(7, "MICRO_ATX")).isNotBlank();
    }

    private static final String GAME30_ITEMS = """
            [{"role":"cpu","variantId":17,"quantity":1},{"role":"mainboard","variantId":41,"quantity":1},
             {"role":"ram","variantId":51,"quantity":2},{"role":"gpu","variantId":69,"quantity":1},
             {"role":"ssd","variantId":83,"quantity":1},{"role":"psu","variantId":94,"quantity":1},
             {"role":"case","variantId":103,"quantity":1},{"role":"cooler","variantId":112,"quantity":1}]""";

    @Test
    @DisplayName("AC13: POST /validate với cache ấm chạy 0 câu SQL; tổng game30 vượt B = 30 tr là fail J8a")
    void validateRunsWithoutSqlWhenWarm() throws Exception {
        candidateRepositoryFacade.snapshot();
        statistics.clear();

        mockMvc.perform(post("/api/v1/pc-build/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budget\":30000000,\"items\":" + GAME30_ITEMS + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPrice").value(30608000))
                .andExpect(jsonPath("$.data.isCompatible").value(false))
                .andExpect(jsonPath("$.data.budgetFit.status").value("OVER"))
                .andExpect(jsonPath("$.data.checks[0].key").value("J8a"));

        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    @DisplayName("AC13: POST /compatible-replacements với cache ấm chạy 0 câu SQL; nguồn PF450D không đủ cho RX 9060 XT")
    void replacementsRunWithoutSqlWhenWarm() throws Exception {
        candidateRepositoryFacade.snapshot();
        statistics.clear();

        mockMvc.perform(post("/api/v1/pc-build/compatible-replacements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"psu\",\"budget\":31000000,\"items\":" + GAME30_ITEMS + ",\"limit\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].isCompatible").value(true))
                .andExpect(jsonPath("$.data[?(@.variantId == 92)].isCompatible").value(contains(false)))
                .andExpect(jsonPath("$.data[?(@.variantId == 92)].failedChecks[0]").exists());

        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    @DisplayName("POST /save: giá lấy từ DB (bỏ giá client), lưu total_price = Σ giá × số lượng")
    void saveStoresServerPrices() throws Exception {
        String items = GAME30_ITEMS.replace("\"quantity\":1}", "\"quantity\":1,\"unitPrice\":1}");

        mockMvc.perform(post("/api/v1/pc-build/save")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("customer@techzone.demo", "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildName\":\"Game 30\",\"totalPrice\":1,\"items\":" + items + "}"))
                .andExpect(status().isOk());

        Integer buildId = jdbc.queryForObject("SELECT max(id) FROM pc_customs", Integer.class);
        assertThat(jdbc.queryForObject("SELECT total_price FROM pc_customs WHERE id = ?", BigDecimal.class, buildId))
                .isEqualByComparingTo("30608000");
        assertThat(jdbc.queryForObject("SELECT unit_price FROM pc_custom_items WHERE pc_custom_id = ? AND product_variant_id = 69",
                BigDecimal.class, buildId)).isEqualByComparingTo("11790000");
    }

    @Test
    @DisplayName("POST /save: build lệch socket → HTTP 422 BUILD_INCOMPATIBLE kèm checks, không lưu")
    void saveRejectsIncompatibleBuild() throws Exception {
        Integer before = jdbc.queryForObject("SELECT count(*) FROM pc_customs", Integer.class);
        // Bo AM4 (30) với CPU LGA1700 (17)
        String items = GAME30_ITEMS.replace("\"variantId\":41", "\"variantId\":30");

        mockMvc.perform(post("/api/v1/pc-build/save")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("customer@techzone.demo", "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":" + items + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUILD_INCOMPATIBLE"))
                .andExpect(jsonPath("$.data.checks[0].status").value("fail"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM pc_customs", Integer.class)).isEqualTo(before);
    }

    @Test
    @DisplayName("Thông số R của dữ liệu thật hiển thị display_value của option (ví dụ socket)")
    void ruleDetailsUseOptionDisplayValues() {
        CompatibilitySnapshot snapshot = candidateRepositoryFacade.snapshot();
        EngineReport report = compatibilityEngine.evaluate(linesOf(snapshot, Map.of(17, 1, 41, 1)),
                new EvaluationContext(null, EvaluationContext.Mode.USER, snapshot));

        CompatibilityCheckResponse socket = report.checks().stream()
                .filter(check -> "R1".equals(check.getKey())).findFirst().orElseThrow();
        assertThat(socket.getStatus()).isEqualTo("pass");
        assertThat(socket.getDetail()).startsWith("Tương thích (").contains("=");
    }
}
