package com.project.cybershop.order.internal.service;

import com.project.cybershop.order.api.dto.CheckoutItemResponse;
import com.project.cybershop.order.api.dto.CheckoutPreviewResponse;
import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {
    private final JdbcTemplate jdbc;

    public CheckoutService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Preview only: NEVER create an Order or deduct stock here.
    @Transactional(readOnly = true)
    public CheckoutPreviewResponse validate(String loginEmail) {
        List<Integer> users = jdbc.query(
                "SELECT \"id\" FROM \"Users\" WHERE lower(\"email\") = lower(?) AND \"is_active\" = true",
                (rs, rowNum) -> rs.getInt("id"), loginEmail);
        if (users.isEmpty()) {
            throw new BusinessException("Không tìm thấy tài khoản đang hoạt động.");
        }
        Integer userId = users.get(0);

        List<Integer> carts = jdbc.query(
                "SELECT \"id\" FROM \"Carts\" WHERE \"user_id\" = ?",
                (rs, rowNum) -> rs.getInt("id"), userId);
        if (carts.isEmpty()) {
            throw new BusinessException("Bạn chưa có giỏ hàng.");
        }
        Integer cartId = carts.get(0);

        // LEFT JOIN to expose cart items referencing missing variants/products.
        String sql = """
                SELECT ci."product_variant_id" AS variant_id,
                       ci."quantity" AS qty,
                       pv."id" AS existing_variant_id,
                       p."id" AS existing_product_id,
                       p."name" AS product_name,
                       p."status" AS product_status,
                       pv."variant_name" AS variant_name,
                       pv."image_url" AS image_url,
                       pv."price" AS unit_price,
                       pv."stock_quantity" AS stock_quantity
                  FROM "Cart_Items" ci
                  LEFT JOIN "Product_Variants" pv ON pv."id" = ci."product_variant_id"
                  LEFT JOIN "Products" p ON p."id" = pv."product_id"
                 WHERE ci."cart_id" = ?
                 ORDER BY ci."id"
                """;
        List<CartRow> rows = jdbc.query(sql, (rs, rowNum) -> new CartRow(
                rs.getInt("variant_id"), rs.getInt("qty"),
                (Integer) rs.getObject("existing_variant_id"),
                (Integer) rs.getObject("existing_product_id"),
                rs.getString("product_name"), rs.getString("product_status"),
                rs.getString("variant_name"), rs.getString("image_url"),
                rs.getBigDecimal("unit_price"),
                (Integer) rs.getObject("stock_quantity")
        ), cartId);
        if (rows.isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống.");
        }

        List<CheckoutItemResponse> items = rows.stream().map(row -> {
            if (row.existingVariantId() == null || row.existingProductId() == null) {
                throw new BusinessException("Sản phẩm trong giỏ không còn tồn tại: variant " + row.variantId());
            }
            if (!"active".equalsIgnoreCase(row.productStatus())) {
                throw new BusinessException("Sản phẩm đã ngừng bán: " + row.productName());
            }
            if (row.quantity() <= 0) {
                throw new BusinessException("Số lượng không hợp lệ: " + row.productName());
            }
            if (row.stockQuantity() == null || row.quantity() > row.stockQuantity()) {
                throw new BusinessException("Không đủ tồn kho: " + row.productName());
            }
            if (row.unitPrice() == null || row.unitPrice().signum() < 0) {
                throw new BusinessException("Giá sản phẩm không hợp lệ: " + row.productName());
            }
            BigDecimal subtotal = row.unitPrice().multiply(BigDecimal.valueOf(row.quantity()));
            return new CheckoutItemResponse(row.variantId(), row.productName(),
                    row.variantName(), row.imageUrl(), row.unitPrice(),
                    row.quantity(), subtotal, row.stockQuantity());
        }).toList();
        BigDecimal total = items.stream().map(CheckoutItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CheckoutPreviewResponse(cartId, items, total, true);
    }

    private record CartRow(Integer variantId, Integer quantity,
                           Integer existingVariantId, Integer existingProductId,
                           String productName, String productStatus,
                           String variantName, String imageUrl,
                           BigDecimal unitPrice, Integer stockQuantity) {}
}
