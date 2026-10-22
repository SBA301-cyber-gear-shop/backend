package com.project.cybershop.catalog.internal.service.impl;

import com.project.cybershop.catalog.internal.service.CatalogService;

import com.project.cybershop.catalog.api.dto.response.CatalogProductResponse;
import com.project.cybershop.review.api.ReviewApi;
import com.project.cybershop.review.api.dto.response.ProductFeedbackSummary;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogServiceImpl implements CatalogService {
    private final JdbcTemplate jdbc;
    private final ReviewApi reviewApi;

    public CatalogServiceImpl(JdbcTemplate jdbc, ReviewApi reviewApi) {
        this.jdbc = jdbc;
        this.reviewApi = reviewApi;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogProductResponse> list() {
        var feedback = reviewApi.productSummaries();
        Map<Integer, Map<String, String>> specs = new LinkedHashMap<>();
        Map<Integer, List<String>> primary = new LinkedHashMap<>();
        Map<Integer, List<String>> filters = new LinkedHashMap<>();
        jdbc.query("""
                SELECT sv.product_variant_id, s.name, COALESCE(so.display_value, sv.value) AS value,
                       cs.is_primary, cs.is_filterable
                FROM specification_values sv
                JOIN specifications s ON s.id = sv.specification_id
                JOIN product_variants pv ON pv.id = sv.product_variant_id
                JOIN products p ON p.id = pv.product_id AND lower(p.status) = 'active'
                LEFT JOIN specification_options so ON so.id = sv.option_id
                LEFT JOIN category_specifications cs
                    ON cs.category_id = p.category_id AND cs.specification_id = s.id
                ORDER BY sv.product_variant_id, s.id
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            int id = rs.getInt("product_variant_id");
            String name = rs.getString("name");
            if (rs.getString("value") != null) {
                specs.computeIfAbsent(id, key -> new LinkedHashMap<>()).put(name, rs.getString("value"));
            }
            if (rs.getBoolean("is_primary")) primary.computeIfAbsent(id, key -> new ArrayList<>()).add(name);
            if (rs.getBoolean("is_filterable")) filters.computeIfAbsent(id, key -> new ArrayList<>()).add(name);
        });
        return jdbc.query("""
                SELECT pv.id, pv.product_id, p.name, c.slug, c.name AS category_name,
                       b.name AS brand, pv.serial_code, pv.variant_name, pv.price, pv.original_price,
                       COALESCE(pv.stock_quantity, 0) AS stock, p.warranty_period_months,
                       pv.image_url, p.description, p.status
                FROM product_variants pv
                JOIN products p ON p.id = pv.product_id
                JOIN brands b ON b.id = p.brand_id
                JOIN categories c ON c.id = p.category_id
                WHERE lower(p.status) = 'active'
                ORDER BY p.id, pv.id
                """, (rs, row) -> {
            var summary = feedback.getOrDefault(rs.getInt("id"), new ProductFeedbackSummary(
                    rs.getInt("id"), BigDecimal.ZERO, 0, 0
            ));
            return new CatalogProductResponse(
                rs.getString("id"), rs.getString("product_id"), rs.getString("name"),
                rs.getString("slug"), rs.getString("category_name"), rs.getString("brand"),
                rs.getString("serial_code"), rs.getString("variant_name"), rs.getBigDecimal("price"),
                rs.getBigDecimal("original_price"), rs.getInt("stock"), rs.getInt("warranty_period_months"),
                rs.getString("image_url"), rs.getString("description"), rs.getString("status"),
                specs.getOrDefault(rs.getInt("id"), Map.of()),
                primary.getOrDefault(rs.getInt("id"), List.of()),
                filters.getOrDefault(rs.getInt("id"), List.of()),
                summary.averageRating(), summary.reviewCount(), summary.buyerCount());
        });
    }
}
