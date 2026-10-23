package com.project.cybershop.catalog.internal.repository;

import com.project.cybershop.catalog.internal.entity.Product;
import com.project.cybershop.catalog.internal.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.variants v " +
           "LEFT JOIN FETCH p.brand " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.status = :status " +
           "ORDER BY p.id ASC")
    List<Product> findByStatusWithDetails(@Param("status") ProductStatus status);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategoryIdAndStatus(Integer categoryId, ProductStatus status);

    List<Product> findByBrandIdAndStatus(Integer brandId, ProductStatus status);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.variants v " +
           "LEFT JOIN FETCH p.brand " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.id = :id")
    Optional<Product> findWithDetailsById(@Param("id") Integer id);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.variants v " +
           "LEFT JOIN FETCH p.brand " +
           "LEFT JOIN FETCH p.category " +
           "ORDER BY p.id ASC")
    List<Product> findAllWithDetails();
}
