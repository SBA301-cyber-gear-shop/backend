package com.project.cybershop.catalog.internal.repository;

import com.project.cybershop.catalog.internal.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    @Query(value = """
            SELECT pv.id AS "variantId", p.name AS "productName",
                   pv.variant_name AS "variantName", pv.price AS "price",
                   p.warranty_period_months AS "warrantyPeriodMonths",
                   p.status AS "status", pv.stock_quantity AS "stockQuantity",
                   pv.image_url AS "imageUrl"
            FROM product_variants pv
            JOIN products p ON p.id = pv.product_id
            WHERE pv.id = :variantId
            """, nativeQuery = true)
    Optional<PurchaseVariantProjection> findPurchaseVariant(@Param("variantId") Integer variantId);

    @Query(value = """
            SELECT pv.id FROM product_variants pv
            JOIN products p ON p.id = pv.product_id
            WHERE pv.id = :variantId FOR UPDATE
            """, nativeQuery = true)
    Optional<Integer> lockForPurchase(@Param("variantId") Integer variantId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE product_variants SET stock_quantity = stock_quantity - :quantity
            WHERE id = :variantId AND stock_quantity >= :quantity
            """, nativeQuery = true)
    int deductAvailableStock(@Param("variantId") Integer variantId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE product_variants SET stock_quantity = coalesce(stock_quantity, 0) + :quantity
            WHERE id = :variantId
            """, nativeQuery = true)
    int restoreAvailableStock(@Param("variantId") Integer variantId, @Param("quantity") int quantity);

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

    @Query("SELECT DISTINCT pv FROM ProductVariant pv " +
           "LEFT JOIN FETCH pv.attributeValues av " +
           "LEFT JOIN FETCH av.specification " +
           "LEFT JOIN FETCH av.option " +
           "WHERE pv.product.id = :productId")
    List<ProductVariant> findWithAttributesByProductId(@Param("productId") Integer productId);
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
}
