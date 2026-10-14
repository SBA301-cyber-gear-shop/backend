package com.project.cybershop.aftersales.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Một bước người gọi được phép làm tiếp. resolutions rỗng nghĩa là bước này không cần chọn kết quả.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowedTransitionDto {
    private String toStatus;
    private List<String> resolutions;
}
