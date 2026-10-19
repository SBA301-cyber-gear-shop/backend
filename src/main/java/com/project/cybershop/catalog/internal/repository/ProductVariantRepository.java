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
           "WHERE pv.id = :id")
    Optional<ProductVariant> findWithAttributesById(@Param("id") Integer id);
}
