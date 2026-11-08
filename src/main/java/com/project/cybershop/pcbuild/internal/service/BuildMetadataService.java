package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata;

import java.util.List;

/** Nạp và kiểm khuôn bộ PC (build_template_slots) và chia ngân sách (budget_profiles) cho các bước 4–6. */
public interface BuildMetadataService {

    /**
     * @param candidates món còn hàng, dùng tính maxBuildPrice; truyền danh sách rỗng khi không cần cận trên
     * @throws com.project.cybershop.pcbuild.internal.support.PcBuildException 503 METADATA_INVALID khi cấu hình sai
     */
    BuildMetadata loadMetadata(List<ComponentCandidateDTO> candidates);
}
