package com.project.cybershop.promotion.internal.repository;

import lombok.RequiredArgsConstructor;
import com.project.cybershop.promotion.api.dto.response.ProductSaleSearchResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Promotion owns sale scheduling; uses existing product IDs and price columns without changing Catalog. */
@Repository @RequiredArgsConstructor
public class ProductSaleDataRepository {
    private final JdbcTemplate jdbc;
    public record Variant(Integer id, Integer productId, String name, BigDecimal price, BigDecimal originalPrice) {}
    public List<ProductSaleSearchResult> search(String query) {
        int id = -1;
        try { id = Integer.parseInt(query.startsWith("#") ? query.substring(1) : query); }
        catch (NumberFormatException ignored) { /* Tìm theo tên hoặc thương hiệu. */ }
        String pattern = "%" + query.toLowerCase(java.util.Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return jdbc.query("""
                SELECT p.id,p.name,b.name AS brand,count(pv.id) AS variant_count
                FROM products p
                LEFT JOIN brands b ON b.id=p.brand_id
                LEFT JOIN product_variants pv ON pv.product_id=p.id
                WHERE p.id=? OR lower(p.name) LIKE ? ESCAPE '!'
                   OR lower(coalesce(b.name,'')) LIKE ? ESCAPE '!'
                GROUP BY p.id,p.name,b.name ORDER BY p.id LIMIT 50
                """, (rs, row) -> new ProductSaleSearchResult(rs.getInt("id"), rs.getString("name"),
                rs.getString("brand"), rs.getLong("variant_count")), id, pattern, pattern);
    }
    public Optional<String> productName(Integer id) {
        return jdbc.query("SELECT name FROM products WHERE id=?", (rs, row) -> rs.getString(1), id).stream().findFirst();
    }
    public List<Integer> variantIds(Integer productId) {
        return jdbc.queryForList("SELECT id FROM product_variants WHERE product_id=? ORDER BY id", Integer.class, productId);
    }
    public Optional<Variant> variant(Integer id) {
        return jdbc.query("SELECT id,product_id,variant_name,price,original_price FROM product_variants WHERE id=?",
                (rs, row) -> new Variant(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getBigDecimal(4), rs.getBigDecimal(5)), id).stream().findFirst();
    }
    public void lockVariant(Integer id) { jdbc.queryForList("SELECT id FROM product_variants WHERE id=? FOR UPDATE", id); }
    public void applyPrice(Integer id, BigDecimal price, BigDecimal original) {
        jdbc.update("UPDATE product_variants SET price=?,original_price=? WHERE id=?", price, original, id);
    }
}
