package com.project.cybershop.catalog.internal.repository;

import com.project.cybershop.catalog.internal.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
       Optional<ProductVariant> findBySerialCode(String serialCode);

       boolean existsBySerialCode(String serialCode);

       Optional<ProductVariant> findBySlug(String slug);

       boolean existsBySlug(String slug);

       List<ProductVariant> findByProductId(Integer productId);

       @Query("SELECT DISTINCT pv FROM ProductVariant pv " +
                     "LEFT JOIN FETCH pv.attributeValues av " +
                     "LEFT JOIN FETCH av.specification " +
                     "LEFT JOIN FETCH av.option " +
                     "WHERE pv.id = :id")
       Optional<ProductVariant> findWithAttributesById(@Param("id") Integer id);

       @Query("SELECT pv FROM ProductVariant pv " +
                     "JOIN FETCH pv.product p " +
                     "JOIN FETCH p.category c " +
                     "LEFT JOIN FETCH p.brand b " +
                     "WHERE p.status = com.project.cybershop.catalog.internal.entity.ProductStatus.ACTIVE AND pv.stockQuantity > 0")
       List<ProductVariant> findAllActiveInStock();

       @Query("SELECT pv FROM ProductVariant pv " +
                     "JOIN FETCH pv.product p " +
                     "JOIN FETCH p.category c " +
                     "WHERE p.status = com.project.cybershop.catalog.internal.entity.ProductStatus.ACTIVE AND pv.stockQuantity > 0 AND c.id = :categoryId")
       List<ProductVariant> findActiveByCategoryId(@Param("categoryId") Integer categoryId);

       @Query("SELECT DISTINCT pv FROM ProductVariant pv " +
                     "LEFT JOIN FETCH pv.attributeValues av " +
                     "LEFT JOIN FETCH av.specification " +
                     "LEFT JOIN FETCH av.option " +
                     "WHERE pv.product.id = :productId")
       List<ProductVariant> findWithAttributesByProductId(@Param("productId") Integer productId);

}
