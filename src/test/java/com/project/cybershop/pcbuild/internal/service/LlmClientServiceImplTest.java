package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.pcbuild.internal.support.AiCircuitBreaker;
import com.project.cybershop.pcbuild.internal.support.AiUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LlmClientServiceImpl - gọi Gemini có timeout, deadline, thử lại và ngắt mạch (DD 8.2, 8.5.1)")
class LlmClientServiceImplTest {

    private static final String GEMINI_OK_RESPONSE = """
            {"candidates":[{"content":{"parts":[{"text":"{\\"buildName\\":\\"B\\",\\"aiReasoning\\":\\"r\\",\\"performanceHighlight\\":\\"p\\",\\"partReasons\\":[]}"}]}}]}
            """;
    private static final String PROFILE_OK_RESPONSE = """
            {"candidates":[{"content":{"parts":[{"thought":true,"text":"đang nghĩ"},{"text":"{\\"summary\\":\\"Game\\",\\"archetype\\":\\"GAMING\\",\\"confidence\\":0.9}"}]}}]}
            """;

    private LlmClientServiceImpl llmClientService;
    private AiCircuitBreaker breaker;
    private HttpServer server;
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicReference<String> lastKeyHeader = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final List<Integer> requestsSeen = new CopyOnWriteArrayList<>();
    private final Deque<Integer> statuses = new ArrayDeque<>();
    private String okBody = GEMINI_OK_RESPONSE;
    private long delayMs;

    @BeforeEach
    void setUp() {
        breaker = new AiCircuitBreaker(Clock.systemUTC(), 5, 30);
        llmClientService = new LlmClientServiceImpl(new ObjectMapper(), breaker, Clock.systemUTC());
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    @DisplayName("Không cấu hình API Key (rỗng hoặc null) -> AI_UNAVAILABLE ngay, không gọi HTTP")
    void missingApiKeyFailsWithoutHttpCall() throws IOException {
        startGeminiStub();
        for (String missingKey : new String[]{"", null}) {
            ReflectionTestUtils.setField(llmClientService, "apiKey", missingKey);

            assertThatThrownBy(() -> llmClientService.extractProfile("p", "{}", Instant.now().plusSeconds(10)))
                    .isInstanceOfSatisfying(AiUnavailableException.class,
                            error -> assertThat(error.detail()).isEqualTo(AiUnavailableException.Detail.PROVIDER_ERROR));
        }
        assertThat(requestsSeen).isEmpty();
    }

    @Test
    @DisplayName("B20, B31: key chỉ đi qua header, URL không có ?key=, body không có key/temperature, model lấy từ cấu hình")
    void geminiCallKeepsKeyOutOfUrlAndSendsNoTemperature() throws IOException {
        startGeminiStub();

        var explanation = llmClientService.explainBuild("System prompt", "{}", Instant.now().plusSeconds(10));

        assertThat(explanation.getBuildName()).isEqualTo("B");
        assertThat(lastPath.get()).isEqualTo("/models/gemini-3.5-flash-lite:generateContent");
        assertThat(lastQuery.get()).isNull();
        assertThat(lastKeyHeader.get()).isEqualTo("test-key");
        assertThat(lastBody.get()).contains("\"thinkingLevel\":\"minimal\"")
                .doesNotContain("temperature").doesNotContain("test-key");
    }

    @Test
    @DisplayName("B19: AI chậm hơn timeout của bước -> ngắt bằng TIMEOUT, không treo request")
    void geminiCallTimesOutUsingConfiguredTimeout() throws IOException {
        delayMs = 3000;
        startGeminiStub();
        ReflectionTestUtils.setField(llmClientService, "profileTimeoutMs", 300L);
        ReflectionTestUtils.setField(llmClientService, "maxRetries", 0);

        long start = System.nanoTime();
        assertThatThrownBy(() -> llmClientService.extractProfile("p", "{}", Instant.now().plusSeconds(10)))
                .isInstanceOfSatisfying(AiUnavailableException.class,
                        error -> assertThat(error.detail()).isEqualTo(AiUnavailableException.Detail.TIMEOUT));
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(2000);
    }

    @Test
    @DisplayName("extractProfile: body có model từ cấu hình, thinkingConfig.thinkingLevel, responseSchema; không temperature/topP/topK; bỏ phần thought")
    void extractProfileSendsThinkingLevelAndParsesSchema() throws IOException {
        okBody = PROFILE_OK_RESPONSE;
        startGeminiStub();

        var profile = llmClientService.extractProfile("prompt", "{}", Instant.now().plusSeconds(10));

        assertThat(profile.getArchetype()).isEqualTo("GAMING");
        assertThat(lastPath.get()).isEqualTo("/models/gemini-3.5-flash-lite:generateContent");
        assertThat(lastBody.get()).contains("\"thinkingLevel\":\"low\"").contains("responseSchema").contains("\"archetype\"")
                .doesNotContain("temperature").doesNotContain("topP").doesNotContain("topK");
    }

    @Test
    @DisplayName("Thử lại đúng 1 lần với 5xx rồi thành công; không thử lại với 400")
    void retriesOnlyProviderErrors() throws IOException {
        okBody = PROFILE_OK_RESPONSE;
        statuses.add(503);
        startGeminiStub();

        assertThat(llmClientService.extractProfile("p", "{}", Instant.now().plusSeconds(10)).getSummary()).isEqualTo("Game");
        assertThat(requestsSeen).hasSize(2);

        requestsSeen.clear();
        statuses.add(400);
        assertThatThrownBy(() -> llmClientService.extractProfile("p", "{}", Instant.now().plusSeconds(10)))
                .isInstanceOfSatisfying(AiUnavailableException.class,
                        error -> assertThat(error.detail()).isEqualTo(AiUnavailableException.Detail.PROVIDER_ERROR));
        assertThat(requestsSeen).hasSize(1);
    }

    @Test
    @DisplayName("Còn 1,5 s tới deadline (< min-call-ms 2 s) → không gọi HTTP, ném DEADLINE_EXCEEDED")
    void respectsDeadline() throws IOException {
        startGeminiStub();

        assertThatThrownBy(() -> llmClientService.explainBuild("p", "{}", Instant.now().plusMillis(1500)))
                .isInstanceOfSatisfying(AiUnavailableException.class,
                        error -> assertThat(error.detail()).isEqualTo(AiUnavailableException.Detail.DEADLINE_EXCEEDED));
        assertThat(requestsSeen).isEmpty();
    }

    @Test
    @DisplayName("Lần gọi đầu timeout, deadline còn ít hơn timeout của bước → lần thử lại dùng thời gian còn lại, không vượt deadline")
    void retryTimeoutIsBoundedByDeadline() throws IOException {
        delayMs = 5000;
        startGeminiStub();
        ReflectionTestUtils.setField(llmClientService, "profileTimeoutMs", 2500L);

        long start = System.nanoTime();
        assertThatThrownBy(() -> llmClientService.extractProfile("p", "{}", Instant.now().plusMillis(4800)))
                .isInstanceOf(AiUnavailableException.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isLessThan(5200);
        assertThat(requestsSeen).hasSize(2);
    }

    @Test
    @DisplayName("Schema hồ sơ bắt buộc đủ 8 vai trò trong roleWeights (Gemini bỏ khóa không bắt buộc)")
    @SuppressWarnings("unchecked")
    void profileSchemaRequiresAllRoleWeights() {
        Map<String, Object> properties = (Map<String, Object>) LlmClientServiceImpl.profileSchema().get("properties");
        Map<String, Object> roleWeights = (Map<String, Object>) properties.get("roleWeights");

        assertThat((List<String>) roleWeights.get("required"))
                .containsExactlyInAnyOrder("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");
        assertThat(((Map<String, Object>) roleWeights.get("properties")).keySet())
                .containsExactlyInAnyOrder("cpu", "mainboard", "ram", "gpu", "ssd", "psu", "case", "cooler");
    }

    @Test
    @DisplayName("Schema hồ sơ có objective tùy chọn, chỉ nhận MAX_PRICE")
    @SuppressWarnings("unchecked")
    void profileSchemaHasOptionalObjective() {
        Map<String, Object> schema = LlmClientServiceImpl.profileSchema();
        Map<String, Object> objective = (Map<String, Object>) ((Map<String, Object>) schema.get("properties")).get("objective");

        assertThat((List<String>) objective.get("enum")).containsExactly("MAX_PRICE");
        assertThat((List<String>) schema.get("required")).doesNotContain("objective");
    }

    @Test
    @DisplayName("Mạch OPEN → không gọi HTTP, ném CIRCUIT_OPEN kèm Retry-After")
    void openCircuitSkipsHttp() throws IOException {
        startGeminiStub();
        for (int failure = 0; failure < 5; failure++) breaker.onProviderFailure("TEST");

        assertThatThrownBy(() -> llmClientService.extractProfile("p", "{}", Instant.now().plusSeconds(10)))
                .isInstanceOfSatisfying(AiUnavailableException.class, error -> {
                    assertThat(error.detail()).isEqualTo(AiUnavailableException.Detail.CIRCUIT_OPEN);
                    assertThat(error.getRetryAfterSeconds()).isBetween(1L, 30L);
                });
        assertThat(requestsSeen).isEmpty();
    }

    private void startGeminiStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", exchange -> {
            requestsSeen.add(1);
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastKeyHeader.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Integer status;
            synchronized (statuses) {
                status = statuses.poll();
            }
            byte[] bytes = (status == null ? okBody : "{\"error\":\"x\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status == null ? 200 : status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        ReflectionTestUtils.setField(llmClientService, "apiKey", "test-key");
        ReflectionTestUtils.setField(llmClientService, "provider", "gemini");
        ReflectionTestUtils.setField(llmClientService, "modelName", "gemini-3.5-flash-lite");
        ReflectionTestUtils.setField(llmClientService, "timeoutMs", 8000L);
        ReflectionTestUtils.setField(llmClientService, "profileTimeoutMs", 6000L);
        ReflectionTestUtils.setField(llmClientService, "maxRetries", 1);
        ReflectionTestUtils.setField(llmClientService, "minCallMs", 2000L);
        ReflectionTestUtils.setField(llmClientService, "profileThinkingLevel", "low");
        ReflectionTestUtils.setField(llmClientService, "explainThinkingLevel", "minimal");
        ReflectionTestUtils.setField(llmClientService, "geminiBaseUrl",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/models/");
        llmClientService.init();
    }
}
