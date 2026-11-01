package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.profile.ProfilePreferenceDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import com.project.cybershop.pcbuild.internal.model.BuildTier;

import java.util.List;
import java.util.Map;

/**
 * Hồ sơ mẫu (DD 5.5). Chỉ dùng làm ví dụ trong prompt và để điền phần AI bỏ trống;
 * không phải danh sách khách chọn và không dùng thay thế khi AI lỗi.
 */
public interface ProfilePresets {

    /** Hồ sơ mẫu đầy đủ, dựng tất định theo đúng 6 bước của DD 5.5. */
    RequirementProfileDto preset(Archetype archetype, BuildTier tier);

    /** Trọng số vai trò mẫu (tổng = 1). */
    Map<String, Double> roleWeights(Archetype archetype);

    /** Ưu tiên mặc định của vai trò (dùng khi hồ sơ không có preference nào cho vai trò đó). */
    List<ProfilePreferenceDto> defaultPreferences(String role);

    /**
     * Điền phần AI bỏ trống từ hồ sơ mẫu cùng archetype: roleWeights rỗng, vai trò có trọng số nhưng chưa có preference,
     * preference cùng spec/metric chưa có cap. Trả bản sao, không sửa đầu vào.
     */
    RequirementProfileDto fillMissing(RequirementProfileDto profile, BuildTier tier);
}
