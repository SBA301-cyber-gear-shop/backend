package com.project.cybershop.promotion.internal.repository;
import com.project.cybershop.promotion.internal.entity.ProductSalePrice;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProductSalePriceRepository extends JpaRepository<ProductSalePrice, Integer> {
    void deleteByProductId(Integer productId);
}
