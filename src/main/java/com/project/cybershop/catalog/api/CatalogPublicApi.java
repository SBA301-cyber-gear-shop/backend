package com.project.cybershop.catalog.api;

import com.project.cybershop.catalog.api.dto.CategorySpecDto;
import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;

import java.util.List;
import java.util.Map;

public interface CatalogPublicApi {
    List<ComponentCandidateDTO> getActiveCandidatesForBuild();

    /** Biến thể theo id, KHÔNG lọc tồn kho hay trạng thái: build đã lưu vẫn hiện được món hết hàng. */
    Map<Integer, ComponentCandidateDTO> getCandidateMapByIds(List<Integer> variantIds);

    /** Như getCandidateMapByIds nhưng trả danh sách; giá đọc mới từ DB (dùng khi lưu build). */
    List<ComponentCandidateDTO> getCandidatesByIds(List<Integer> variantIds);

    /** Toàn bộ category_specifications kèm option ENUM, nạp bằng vài truy vấn cố định. */
    List<CategorySpecDto> getSpecificationCatalog();

    List<String> getAllBrandNames();

    List<com.project.cybershop.catalog.api.dto.response.CategoryResponse> getAllCategories();
}
