package com.project.cybershop.catalog.internal.repository;

import com.project.cybershop.catalog.internal.entity.SpecificationValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecificationValueRepository extends JpaRepository<SpecificationValue, Integer> {
    List<SpecificationValue> findByProductVariantId(Integer variantId);

    @Query("SELECT sv FROM SpecificationValue sv JOIN FETCH sv.specification WHERE sv.productVariant.id = :variantId")
    List<SpecificationValue> findWithSpecByVariantId(@Param("variantId") Integer variantId);

    Optional<SpecificationValue> findByProductVariantIdAndSpecificationId(Integer variantId, Integer specificationId);

    void deleteByProductVariantId(Integer variantId);

    @Query("SELECT sv FROM SpecificationValue sv " +
           "JOIN FETCH sv.specification s " +
           "WHERE sv.productVariant.id IN :variantIds")
    List<SpecificationValue> findByProductVariantIdIn(@Param("variantIds") List<Integer> variantIds);
}
