package com.project.cybershop.catalog.internal.repository;

import com.project.cybershop.catalog.internal.entity.CategorySpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorySpecificationRepository extends JpaRepository<CategorySpecification, Integer> {
    List<CategorySpecification> findByCategoryId(Integer categoryId);

    @Query("SELECT cs FROM CategorySpecification cs JOIN FETCH cs.specification WHERE cs.category.id = :categoryId AND cs.isPrimary = true")
    List<CategorySpecification> findPrimarySpecsByCategoryId(@Param("categoryId") Integer categoryId);

    @Query("SELECT cs FROM CategorySpecification cs JOIN FETCH cs.specification WHERE cs.category.id = :categoryId AND cs.isFilterable = true")
    List<CategorySpecification> findFilterableSpecsByCategoryId(@Param("categoryId") Integer categoryId);

    Optional<CategorySpecification> findByCategoryIdAndSpecificationId(Integer categoryId, Integer specificationId);
}
