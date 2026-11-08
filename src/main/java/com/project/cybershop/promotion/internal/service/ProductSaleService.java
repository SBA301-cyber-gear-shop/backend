package com.project.cybershop.promotion.internal.service;
import com.project.cybershop.promotion.api.dto.request.ProductSaleRequest;
import com.project.cybershop.promotion.api.dto.response.ProductSaleResponse;
import com.project.cybershop.promotion.api.dto.response.ProductSaleSearchResult;
import java.util.List;
public interface ProductSaleService {
    List<ProductSaleResponse> list();
    List<ProductSaleSearchResult> search(String query);
    ProductSaleResponse get(Integer productId);
    ProductSaleResponse save(Integer productId, ProductSaleRequest request);
    void remove(Integer productId);
    void refreshAll();
}
