package com.project.cybershop.pcbuild.internal.controller;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.identity.api.dto.response.UserDto;
import com.project.cybershop.pcbuild.api.dto.request.AiBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.PeripheralQueryRequest;
import com.project.cybershop.pcbuild.api.dto.request.ReplacementQueryRequest;
import com.project.cybershop.pcbuild.api.dto.request.SavePcBuildRequest;
import com.project.cybershop.pcbuild.api.dto.request.ValidateBuildRequest;
import com.project.cybershop.pcbuild.api.dto.response.AiBuildResponse;
import com.project.cybershop.pcbuild.api.dto.response.BuildOptionsResponse;
import com.project.cybershop.pcbuild.api.dto.response.CompatibleReplacementResponse;
import com.project.cybershop.pcbuild.api.dto.response.PeripheralSuggestionResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildDetailResponse;
import com.project.cybershop.pcbuild.api.dto.response.SavedPcBuildSummaryResponse;
import com.project.cybershop.pcbuild.api.dto.response.ValidateBuildResponse;
import com.project.cybershop.pcbuild.internal.service.AiPcBuilderService;
import com.project.cybershop.pcbuild.internal.service.PcCustomService;
import com.project.cybershop.pcbuild.internal.service.PeripheralService;
import com.project.cybershop.pcbuild.internal.support.RateLimiter;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pc-build")
@RequiredArgsConstructor
@Tag(name = "PC Build API", description = "Chức năng xây dựng cấu hình PC bằng AI & Kiểm tra tương thích")
public class PcBuildController {

    private final AiPcBuilderService aiPcBuilderService;
    private final PcCustomService pcCustomService;
    private final PeripheralService peripheralService;
    private final IdentityApi identityApi;
    private final RateLimiter rateLimiter;

    @PostMapping("/ai-suggest")
    @Operation(summary = "Đề xuất cấu hình PC bằng AI dựa trên Database thực tế")
    public ResponseEntity<ApiResponseDTO<AiBuildResponse>> suggestBuild(
            @Valid @RequestBody AiBuildRequest request,
            Principal principal,
            HttpServletRequest httpRequest
    ) {
        rateLimiter.acquire(RateLimiter.Bucket.AI_SUGGEST, clientKey(principal, httpRequest));
        Integer userId = resolveUserId(principal);
        AiBuildResponse response = aiPcBuilderService.processAiBuild(request, userId);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã tạo đề xuất cấu hình PC bằng AI thành công", response));
    }

    @GetMapping("/options")
    @Operation(summary = "Giới hạn ngân sách, từ điển metadata và câu gợi ý nhu cầu cho form xây dựng cấu hình")
    public ResponseEntity<ApiResponseDTO<BuildOptionsResponse>> options(Principal principal, HttpServletRequest httpRequest) {
        rateLimiter.acquire(RateLimiter.Bucket.OPTIONS, clientKey(principal, httpRequest));
        return ResponseEntity.ok(ApiResponseDTO.success(aiPcBuilderService.options()));
    }

    @PostMapping("/validate")
    @Operation(summary = "Kiểm tra tương thích, ngân sách và tồn kho của toàn bộ cấu hình (giá lấy từ server)")
    public ResponseEntity<ApiResponseDTO<ValidateBuildResponse>> validateBuild(
            @Valid @RequestBody ValidateBuildRequest request,
            Principal principal,
            HttpServletRequest httpRequest
    ) {
        rateLimiter.acquire(RateLimiter.Bucket.VALIDATE, clientKey(principal, httpRequest));
        return ResponseEntity.ok(ApiResponseDTO.success(pcCustomService.validateBuild(request)));
    }

    @PostMapping("/compatible-replacements")
    @Operation(summary = "Danh sách linh kiện thay thế, mỗi ứng viên được kiểm trên toàn bộ cấu hình hiện tại")
    public ResponseEntity<ApiResponseDTO<List<CompatibleReplacementResponse>>> queryReplacements(
            @Valid @RequestBody ReplacementQueryRequest request,
            Principal principal,
            HttpServletRequest httpRequest
    ) {
        rateLimiter.acquire(RateLimiter.Bucket.REPLACEMENTS, clientKey(principal, httpRequest));
        return ResponseEntity.ok(ApiResponseDTO.success(pcCustomService.getCompatibleReplacements(request)));
    }

    @PostMapping("/peripherals")
    @Operation(summary = "Chọn màn hình, bàn phím, chuột cho chế độ Trọn bộ theo tiền còn lại (giá lấy từ server, không gọi AI)")
    public ResponseEntity<ApiResponseDTO<PeripheralSuggestionResponse>> suggestPeripherals(
            @Valid @RequestBody PeripheralQueryRequest request,
            Principal principal,
            HttpServletRequest httpRequest
    ) {
        rateLimiter.acquire(RateLimiter.Bucket.PERIPHERALS, clientKey(principal, httpRequest));
        return ResponseEntity.ok(ApiResponseDTO.success(peripheralService.suggest(request)));
    }

    @PostMapping("/save")
    @Operation(summary = "Lưu cấu hình PC từ danh sách linh kiện đã chọn vào tài khoản")
    public ResponseEntity<ApiResponseDTO<Integer>> saveCustomBuild(
            @Valid @RequestBody SavePcBuildRequest request,
            Principal principal
    ) {
        Integer userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponseDTO.<Integer>builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .message("Vui lòng đăng nhập để lưu cấu hình")
                            .data(null)
                            .build()
            );
        }
        Integer buildId = pcCustomService.saveNewBuild(request, userId);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã lưu cấu hình vào danh sách yêu thích của bạn", buildId));
    }

    @GetMapping("/my-builds")
    @Operation(summary = "Lấy danh sách các cấu hình PC đã lưu của người dùng hiện tại")
    public ResponseEntity<ApiResponseDTO<List<SavedPcBuildSummaryResponse>>> getMySavedBuilds(
            Principal principal
    ) {
        Integer userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponseDTO.<List<SavedPcBuildSummaryResponse>>builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .message("Vui lòng đăng nhập để xem danh sách cấu hình đã lưu")
                            .data(null)
                            .build()
            );
        }
        List<SavedPcBuildSummaryResponse> list = pcCustomService.getMySavedBuilds(userId);
        return ResponseEntity.ok(ApiResponseDTO.success(list));
    }

    @GetMapping("/{buildId}")
    @Operation(summary = "Xem chi tiết một cấu hình PC đã lưu theo buildId")
    public ResponseEntity<ApiResponseDTO<SavedPcBuildDetailResponse>> getSavedBuildDetail(
            @PathVariable Integer buildId,
            Principal principal
    ) {
        Integer userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponseDTO.<SavedPcBuildDetailResponse>builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .message("Vui lòng đăng nhập để xem chi tiết cấu hình")
                            .data(null)
                            .build()
            );
        }
        SavedPcBuildDetailResponse detail = pcCustomService.getSavedBuildDetail(buildId, userId);
        return ResponseEntity.ok(ApiResponseDTO.success(detail));
    }

    @DeleteMapping("/{buildId}")
    @Operation(summary = "Xóa một cấu hình PC đã lưu")
    public ResponseEntity<ApiResponseDTO<Void>> deleteSavedBuild(
            @PathVariable Integer buildId,
            Principal principal
    ) {
        Integer userId = resolveUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponseDTO.<Void>builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .message("Vui lòng đăng nhập để thực hiện thao tác này")
                            .data(null)
                            .build()
            );
        }
        pcCustomService.deleteSavedBuild(buildId, userId);
        return ResponseEntity.ok(ApiResponseDTO.success("Đã xóa cấu hình PC thành công", null));
    }

    /**
     * Khóa rate limit: email của user đã đăng nhập (không tra DB, giữ 0 câu SQL cho /validate, AC13),
     * nếu không thì IP kết nối. Không đọc X-Forwarded-For vì client tự đặt được (DD 11.1, U13).
     */
    private static String clientKey(Principal principal, HttpServletRequest httpRequest) {
        if (principal != null && principal.getName() != null) {
            return "user:" + principal.getName();
        }
        return "ip:" + httpRequest.getRemoteAddr();
    }

    private Integer resolveUserId(Principal principal) {
        if (principal != null && principal.getName() != null) {
            return identityApi.getUserByEmail(principal.getName())
                    .map(UserDto::getId)
                    .orElse(null);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return identityApi.getUserByEmail(auth.getName())
                    .map(UserDto::getId)
                    .orElse(null);
        }
        return null;
    }
}
