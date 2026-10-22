package com.project.cybershop.pcbuild.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.AiBuildResponse;
import com.project.cybershop.pcbuild.api.dto.response.CompatibilityCheckResponse;
import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.pcbuild.internal.repository.PcCustomRepository;
import com.project.cybershop.pcbuild.internal.service.AiPcBuilderService;
import com.project.cybershop.pcbuild.internal.service.BudgetHeuristicService;
import com.project.cybershop.pcbuild.internal.service.HardwareCompatibilityValidator;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiPcBuilderServiceImpl - Kiểm thử điều phối toàn luồng xử lý AI PC Builder")
class AiPcBuilderServiceImplTest {

    @Mock
    private CatalogPublicApi catalogPublicApi;

    @Mock
    private BudgetHeuristicService heuristicService;

    @Mock
    private LlmClientService llmClientService;

    @Mock
    private HardwareCompatibilityValidator validator;

    @Mock
    private PcCustomRepository pcCustomRepository;

    @Mock
    private IdentityApi identityApi;

    private ObjectMapper objectMapper = new ObjectMapper();

    private AiPcBuilderServiceImpl aiPcBuilderService;

    @BeforeEach
    void setUp() {
        aiPcBuilderService = new AiPcBuilderServiceImpl(
                catalogPublicApi, heuristicService, llmClientService,
                validator, pcCustomRepository, identityApi, objectMapper
        );
    }

    private ComponentCandidateDTO createSamplePart(int id, String slug, String name, BigDecimal price) {
        return ComponentCandidateDTO.builder()
                .variantId(id)
                .categorySlug(slug)
                .productName(name)
                .variantName(name)
                .price(price)
                .imageUrl("https://example.com/part.jpg")
                .specs(Map.of("Socket", "LGA1700", "RamStandard", "DDR5"))
                .build();
    }

    @Test
    @DisplayName("Kho hàng rỗng -> Ném IllegalStateException báo lỗi lập tức")
    void shouldThrowExceptionWhenCatalogIsEmpty() {
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(List.of());

        AiBuildRequest request = AiBuildRequest.builder()
                .budget(new BigDecimal("20000000"))
                .purpose("gaming")
                .build();

        assertThatThrownBy(() -> aiPcBuilderService.processAiBuild(request, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kho hàng hiện chưa có linh kiện nào");
    }

    @Test
    @DisplayName("Tạo cấu hình AI thành công: Đầy đủ linh kiện, tương thích 100%, lưu DB DRAFT")
    void shouldBuildPcSuccessfullyWhenAiSuggestsValidParts() {
        ComponentCandidateDTO cpu = createSamplePart(101, "cpu", "Intel Core i5 13400F", new BigDecimal("4500000"));
        ComponentCandidateDTO mb = createSamplePart(102, "mainboard", "B760M DDR5", new BigDecimal("3200000"));
        List<ComponentCandidateDTO> candidates = List.of(cpu, mb);

        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(candidates);
        when(heuristicService.filterCandidates(any(), any(), any())).thenReturn(candidates);

        LlmClientService.AiDraftProposal proposal = new LlmClientService.AiDraftProposal();
        proposal.setBuildName("Cấu hình Test");
        proposal.setAiReasoning("Tối ưu");
        proposal.setPerformanceHighlight("Mượt mà");

        var p1 = new LlmClientService.AiDraftProposal.PartSelection();
        p1.setRole("cpu"); p1.setVariantId(101); p1.setQuantity(1); p1.setReason("CPU mạnh");
        var p2 = new LlmClientService.AiDraftProposal.PartSelection();
        p2.setRole("mainboard"); p2.setVariantId(102); p2.setQuantity(1); p2.setReason("Bo mạch tốt");
        proposal.setSelectedParts(List.of(p1, p2));

        when(llmClientService.generateBuildProposal(any(), any())).thenReturn(proposal);

        HardwareCompatibilityValidator.ValidationReport validReport = new HardwareCompatibilityValidator.ValidationReport(
                true,
                List.of(new CompatibilityCheckResponse("socket", "Socket CPU & Mainboard", true, "Khớp LGA1700")),
                List.of()
        );
        when(validator.validate(any(), any())).thenReturn(validReport);

        when(pcCustomRepository.save(any())).thenAnswer(inv -> {
            PcCustom custom = inv.getArgument(0);
            custom.setId(99);
            return custom;
        });

        AiBuildRequest request = AiBuildRequest.builder()
                .budget(new BigDecimal("20000000"))
                .purpose("gaming")
                .build();

        var response = aiPcBuilderService.processAiBuild(request, 12);

        assertThat(response).isNotNull();
        assertThat(response.getBuildId()).isEqualTo(99);
        assertThat(response.isCompatible()).isTrue();
        assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("7700000"));

        ArgumentCaptor<PcCustom> captor = ArgumentCaptor.forClass(PcCustom.class);
        verify(pcCustomRepository).save(captor.capture());
        PcCustom savedCustom = captor.getValue();
        assertThat(savedCustom.getUserId()).isEqualTo(12);
        assertThat(savedCustom.getTotalPrice()).isEqualByComparingTo(new BigDecimal("7700000"));
        assertThat(savedCustom.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Cấu hình AI phát hiện lỗi tương thích -> Trả về isCompatible=false và chi tiết cảnh báo")
    void shouldReturnIncompatibleResponseWhenValidatorFails() {
        ComponentCandidateDTO cpu = createSamplePart(101, "cpu", "Intel Core i5 13400F", new BigDecimal("4500000"));
        when(catalogPublicApi.getActiveCandidatesForBuild()).thenReturn(List.of(cpu));
        when(heuristicService.filterCandidates(any(), any(), any())).thenReturn(List.of(cpu));

        LlmClientService.AiDraftProposal proposal = new LlmClientService.AiDraftProposal();
        proposal.setBuildName("Lỗi Cấu hình");
        var p1 = new LlmClientService.AiDraftProposal.PartSelection();
        p1.setRole("cpu"); p1.setVariantId(101); p1.setQuantity(1);
        proposal.setSelectedParts(List.of(p1));
        when(llmClientService.generateBuildProposal(any(), any())).thenReturn(proposal);

        HardwareCompatibilityValidator.ValidationReport invalidReport = new HardwareCompatibilityValidator.ValidationReport(
                false,
                List.of(new CompatibilityCheckResponse("socket", "Socket", false, "Lệch socket AM5 vs LGA1700")),
                List.of("Lỗi socket")
        );
        when(validator.validate(any(), any())).thenReturn(invalidReport);
        when(pcCustomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AiBuildRequest request = AiBuildRequest.builder()
                .budget(new BigDecimal("10000000"))
                .purpose("gaming")
                .build();

        var response = aiPcBuilderService.processAiBuild(request, null);

        assertThat(response.isCompatible()).isFalse();
        assertThat(response.getChecks().get(0).isValid()).isFalse();
    }
}
