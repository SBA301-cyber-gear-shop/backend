package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.pcbuild.api.dto.profile.ProfileConstraintDto;
import com.project.cybershop.pcbuild.api.dto.profile.ProfileMetricDto;
import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;

import java.util.List;

/**
 * Hồ sơ đã qua ProfileValidator (DD 5.4): role/spec/giá trị đã chuẩn hóa theo từ điển, roleWeights tổng = 1.
 * Không sửa trực tiếp; solver hạ yêu cầu bằng cách dựng bản mới qua {@link #withProfile}.
 *
 * @param warnings mã cảnh báo sinh khi kiểm (ví dụ CONSTRAINT_UNSATISFIABLE:c1)
 */
public record ValidatedProfile(RequirementProfileDto profile, Archetype archetype, List<String> warnings) {

    public ValidatedProfile {
        warnings = List.copyOf(warnings);
    }

    public double weight(String role) {
        Double weight = profile.getRoleWeights().get(role);
        return weight != null ? weight : 0.0;
    }

    public boolean isExcluded(String role) {
        return profile.getExcludedRoles().stream().anyMatch(ref -> role.equals(ref.getRole()));
    }

    public boolean isRequired(String role) {
        return profile.getRequiredRoles().stream().anyMatch(ref -> role.equals(ref.getRole()));
    }

    public List<ProfileConstraintDto> constraints() {
        return profile.getConstraints();
    }

    public List<ProfileMetricDto> metrics() {
        return profile.getMetrics();
    }

    public ValidatedProfile withProfile(RequirementProfileDto changed) {
        return new ValidatedProfile(changed, archetype, warnings);
    }
}
