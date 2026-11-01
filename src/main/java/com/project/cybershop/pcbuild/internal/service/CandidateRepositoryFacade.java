package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.internal.model.CandidatePool;
import com.project.cybershop.pcbuild.internal.model.CompatibilitySnapshot;
import com.project.cybershop.pcbuild.internal.model.ValidatedProfile;

import java.math.BigDecimal;

/**
 * Chủ sở hữu duy nhất của CompatibilitySnapshot (DD 11.4): nơi duy nhất trong luồng gợi ý đọc
 * CatalogPublicApi và CompatibilityRuleRepository. Mỗi request gọi snapshot() đúng một lần rồi truyền xuống.
 */
public interface CandidateRepositoryFacade {
    /** Bản chụp đang có nếu còn trong thời gian cache (60 giây), quá hạn thì nạp lại. */
    CompatibilitySnapshot snapshot();

    /**
     * Ứng viên lọc theo hồ sơ (DD 6.1), thuần bộ nhớ trên snapshot được truyền vào.
     *
     * @param budget null = không lọc theo giá (dùng khi tính floorPrice)
     */
    CandidatePool buildPool(CompatibilitySnapshot snapshot, ValidatedProfile profile, BigDecimal budget);
}
