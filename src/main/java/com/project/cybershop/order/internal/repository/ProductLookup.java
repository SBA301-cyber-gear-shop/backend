
package com.project.cybershop.order.internal.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class ProductLookup {

    private final EntityManager em;
    public ProductLookup(EntityManager em) {
        this.em = em;
    }

    // Dữ liệu phiên bản sản phẩm dùng cho luồng mua hàng
    public record ProductInfo(
            Integer variantId,
            Integer productId,
            String productName,
            String variantName,
            BigDecimal price,
            Integer stockQuantity,
            String imageUrl,
            Integer warrantyPeriodMonths
    ) {}

    // Lấy các phiên bản sản phẩm đang kinh doanh
    public List<ProductInfo> findAllActive() {

        List<?> rows = em.createNativeQuery("""
                SELECT
                    pv.id,
                    p.id,
                    p.name,
                    pv.variant_name,
                    pv.price,
                    pv.stock_quantity,
                    pv.image_url,
                    p.warranty_period_months
                FROM product_variants pv
                JOIN products p
                    ON pv.product_id = p.id
                WHERE p.status = 'active'
                ORDER BY p.id, pv.id
                """).getResultList();

        return rows.stream()
                .map(this::mapProduct)
                .toList();
    }

    // Tìm kiếm theo tên sản phẩm hoặc phiên bản
    public List<ProductInfo> search(String keyword) {

        List<?> rows = em.createNativeQuery("""
                SELECT
                    pv.id,
                    p.id,
                    p.name,
                    pv.variant_name,
                    pv.price,
                    pv.stock_quantity,
                    pv.image_url,
                    p.warranty_period_months
                FROM product_variants pv
                JOIN products p
                    ON pv.product_id = p.id
                WHERE p.status = 'active'
                  AND (
                    LOWER(p.name) LIKE :keyword
                    OR LOWER(COALESCE(pv.variant_name, ''))
                       LIKE :keyword
                  )
                ORDER BY p.id, pv.id
                """)
                .setParameter(
                        "keyword",
                        "%" + (keyword == null ? "" : keyword.trim())
                                .toLowerCase(java.util.Locale.ROOT) + "%"
                )
                .getResultList();

        return rows.stream()
                .map(this::mapProduct)
                .toList();
    }

    // Lấy một phiên bản sản phẩm theo ID
    public ProductInfo findByVariantId(Integer variantId) {

        List<?> rows = em.createNativeQuery("""
                SELECT
                    pv.id,
                    p.id,
                    p.name,
                    pv.variant_name,
                    pv.price,
                    pv.stock_quantity,
                    pv.image_url,
                    p.warranty_period_months
                FROM product_variants pv
                JOIN products p
                    ON pv.product_id = p.id
                WHERE pv.id = :variantId
                  AND p.status = 'active'
                """)
                .setParameter("variantId", variantId)
                .getResultList();

        if (rows.isEmpty()) {
            return null;
        }

        return mapProduct(rows.get(0));
    }

    private ProductInfo mapProduct(Object row) {

        Object[] r = (Object[]) row;

        return new ProductInfo(
                ((Number) r[0]).intValue(),
                ((Number) r[1]).intValue(),
                (String) r[2],
                (String) r[3],
                (BigDecimal) r[4],
                r[5] == null ? 0 : ((Number) r[5]).intValue(),
                (String) r[6],
                ((Number) r[7]).intValue()
        );
    }
}
