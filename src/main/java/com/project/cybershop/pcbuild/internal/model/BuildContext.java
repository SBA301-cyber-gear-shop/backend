package com.project.cybershop.pcbuild.internal.model;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.entity.CompatibilityRule;

import java.util.List;

/**
 * Dữ liệu đọc một lần cho mỗi request build; sau đó bước 5–6 không truy vấn DB nữa.
 *
 * @param candidates món còn hàng (API auto) hoặc món khách gửi lên (API validate)
 */
public record BuildContext(BuildMetadata metadata, List<ComponentCandidateDTO> candidates,
                           List<CompatibilityRule> rules) {
}
