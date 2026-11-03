package com.project.cybershop.pcbuild.api.dto.request;

import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request POST /ai-suggest (DD 9.1). purpose/userPrompt là văn bản tự do; ngưỡng ngân sách thật (minBudget, floorPrice)
 * kiểm ở RequestValidator vì là giá trị cấu hình hoặc động.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiBuildRequest {
    // Ngưỡng max-budget kiểm ở RequestValidator để trả mã BUDGET_TOO_HIGH (AC4); ở đây chỉ chặn số quá dài
    @NotNull(message = "Vui lòng nhập ngân sách")
    @DecimalMin(value = "1", message = "Ngân sách phải lớn hơn 0")
    @Digits(integer = 13, fraction = 0, message = "Ngân sách phải là số nguyên đồng")
    private BigDecimal budget;

    // Nhu cầu chính, văn bản tự do; bắt buộc (2–300 ký tự) khi không gửi profile
    @Size(max = 300, message = "Nhu cầu tối đa 300 ký tự")
    private String purpose;

    // Trường cũ, giữ để tương thích: có thì thành metric TOTAL_RAM_GB GTE priority 1 (V14)
    private Integer ramGb;

    // Trường cũ, bị bỏ qua
    private String scope;

    @Size(max = 500, message = "Mong muốn thêm tối đa 500 ký tự")
    private String userPrompt;

    // Hồ sơ khách đã sửa; có thì không gọi AI trích hồ sơ, server ghi đè source = USER_EDITED
    private RequirementProfileDto profile;

    // Chế độ Trọn bộ: ngân sách khách nhập (gồm màn, phím, chuột); budget chỉ là phần cho thùng máy. Chỉ dùng cho lời giải thích
    @Digits(integer = 13, fraction = 0, message = "Ngân sách trọn bộ phải là số nguyên đồng")
    private BigDecimal fullSetBudget;
}
