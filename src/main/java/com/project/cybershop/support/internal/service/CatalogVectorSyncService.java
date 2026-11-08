package com.project.cybershop.support.internal.service;

import com.project.cybershop.support.api.dto.response.AiChatResponse;

import java.util.List;
import java.util.Map;

public interface CatalogVectorSyncService {

    /**
     * Đồng bộ toàn bộ sản phẩm và linh kiện từ kho hàng Postgres vào Vector DB (cyberbot_knowledge_vectors).
     * Chạy định kỳ mỗi ngày hoặc kích hoạt thủ công.
     */
    Map<String, Object> syncCatalogVectors();

    /**
     * Tìm kiếm linh kiện trong kho hàng bằng Vector RAG (Cosine Similarity trên pgvector).
     */
    List<AiChatResponse.ProductSummary> searchProductsByVector(String query, int limit);

    /**
     * Lấy số lượng bản ghi linh kiện hiện có trong Vector DB.
     */
    int getCatalogVectorCount();
}
