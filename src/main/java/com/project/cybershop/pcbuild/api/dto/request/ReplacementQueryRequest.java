package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Request của POST /compatible-replacements (DD 9.3). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplacementQueryRequest {
    @NotBlank(message = "Vai trò cần thay không được để trống")
    private String role;

    @DecimalMin(value = "1", message = "Ngân sách phải lớn hơn 0")
    @DecimalMax(value = "9999999999", message = "Ngân sách tối đa 9.999.999.999 ₫")
    private BigDecimal budget;

    // Toàn bộ build hiện tại
    @Valid
    @Builder.Default
    @Size(max = 16, message = "Tối đa 16 dòng linh kiện")
    private List<ValidateBuildRequest.ItemPart> items = new ArrayList<>();

    // Vị trí dòng cần thay trong items (GPU/SSD nhiều dòng); null = dòng đầu tiên của role
    @Min(value = 0, message = "lineIndex không hợp lệ")
    private Integer lineIndex;

    @Min(value = 1, message = "limit tối thiểu là 1")
    @Max(value = 100, message = "limit tối đa là 100")
    private Integer limit;

    // Hồ sơ đang dùng (tùy chọn): có thì mỗi ứng viên được kiểm thêm check lệch hồ sơ P:* (DD 7.4)
    private com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto profile;
}
