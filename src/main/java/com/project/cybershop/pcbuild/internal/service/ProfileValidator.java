package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.api.dto.profile.RequirementProfileDto;
import com.project.cybershop.pcbuild.api.dto.response.MetadataDictionaryResponse;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;

/**
 * Kiểm hồ sơ theo metadata (DD 5.4, V1–V15). Hồ sơ từ AI hay từ client đều là dữ liệu không tin cậy:
 * mục sai bị bỏ và ghi vào droppedItems, không làm lỗi cả request. Hồ sơ chỉ được thu hẹp ứng viên và đổi trọng số.
 */
public interface ProfileValidator {

    String SOURCE_AI = "AI";
    String SOURCE_USER_EDITED = "USER_EDITED";

    /**
     * @param source      AI hoặc USER_EDITED (server quyết định, ghi đè giá trị client gửi)
     * @param legacyRamGb trường ramGb cũ của request; khác null thì thành metric TOTAL_RAM_GB GTE priority 1 (V14)
     */
    ValidatedProfile validate(RequirementProfileDto input, MetadataDictionaryResponse dictionary, String source,
                              Integer legacyRamGb);
}
