package com.project.cybershop.promotion.internal.service.impl;

import com.project.cybershop.promotion.api.dto.request.ProductSaleRequest;
import com.project.cybershop.promotion.api.dto.response.ProductSaleResponse;
import com.project.cybershop.promotion.api.dto.response.ProductSaleSearchResult;
import com.project.cybershop.promotion.internal.entity.ProductSale;
import com.project.cybershop.promotion.internal.repository.*;
import com.project.cybershop.promotion.internal.service.ProductSaleService;
import com.project.cybershop.promotion.internal.service.ProductSalePriceService;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class ProductSaleServiceImpl implements ProductSaleService {
    private final ProductSaleRepository sales;
    private final ProductSalePriceRepository prices;
    private final ProductSaleDataRepository data;
    private final ProductSalePriceService synchronizer;
    private final Clock clock;

    @Override public void refreshAll() {
        for (var sale : sales.findAll()) synchronizer.sync(data.variantIds(sale.getProductId()));
    }
    @Override public List<ProductSaleSearchResult> search(String query) {
        String term = query == null ? "" : query.trim();
        if (term.length() > 100) throw new BusinessException("Từ khóa tìm sản phẩm tối đa 100 ký tự.");
        return term.isEmpty() ? List.of() : data.search(term);
    }
    @Override public List<ProductSaleResponse> list() {
        return sales.findAll().stream().filter(s -> data.productName(s.getProductId()).isPresent())
                .map(s -> get(s.getProductId())).toList();
    }
    @Override public ProductSaleResponse get(Integer productId) {
        String name = name(productId);
        var ids = data.variantIds(productId);
        synchronizer.sync(ids);
        var sale = sales.findById(productId).orElse(null);
        var now = LocalDateTime.now(clock);
        boolean active = sale != null && sale.isEnabled() && !now.isBefore(sale.getStart()) && now.isBefore(sale.getEnd());
        var rows = ids.stream().map(id -> {
            var v = data.variant(id).orElseThrow();
            var saved = prices.findById(id).orElse(null);
            BigDecimal regular = saved == null ? v.price() : saved.getRegularPrice();
            BigDecimal original = saved == null ? v.originalPrice() : saved.getOriginalPrice();
            BigDecimal base = regular;
            return new ProductSaleResponse.Variant(id, v.name(), base, regular, v.price());
        }).toList();
        return new ProductSaleResponse(productId, name, sale != null, sale != null && sale.isEnabled(),
                sale == null ? null : sale.getPercentage(), sale == null ? null : sale.getStart(), sale == null ? null : sale.getEnd(), active, rows,
                sale == null ? "percentage" : sale.getDiscountType(),
                sale == null ? null : sale.getDiscountValue() != null ? sale.getDiscountValue() : sale.getPercentage(),
                sale == null || sale.getVariantIds() == null ? null : java.util.Arrays.asList(sale.getVariantIds()));
    }
    @Override @Transactional public ProductSaleResponse save(Integer productId, ProductSaleRequest request) {
        name(productId);
        if (request == null) throw new BusinessException("Thiếu thông tin lịch sale.");
        String type = request.discountType() == null ? "percentage" : request.discountType();
        if (!List.of("percentage", "fixed_amount").contains(type)) throw new BusinessException("Loại giảm giá không hợp lệ.");
        BigDecimal value = request.discountValue() != null ? request.discountValue()
                : "percentage".equals(type) ? request.percentage() : null;
        if (value == null || value.signum() <= 0 || value.scale() > 2
                || value.compareTo(new BigDecimal("9999999999")) > 0
                || "percentage".equals(type) && value.compareTo(BigDecimal.valueOf(100)) > 0
                || "fixed_amount".equals(type) && value.stripTrailingZeros().scale() > 0)
            throw new BusinessException("Giảm phần trăm từ 0,01 đến 100%; giảm tiền phải là số đồng nguyên từ 1 đến 9.999.999.999.");
        if (request.percentage() != null && (!"percentage".equals(type) || request.percentage().compareTo(value) != 0))
            throw new BusinessException("Giá trị phần trăm không khớp loại hoặc mức giảm.");
        if (request.start() == null || request.end() == null || !request.end().isAfter(request.start()))
            throw new BusinessException("Thời gian kết thúc phải sau bắt đầu.");
        var ids = data.variantIds(productId);
        if (ids.isEmpty()) throw new BusinessException("Sản phẩm cần có phiên bản trước khi lên lịch sale.");
        if (request.variantIds() != null && (request.variantIds().isEmpty()
                || request.variantIds().stream().anyMatch(id -> id == null || !ids.contains(id))
                || request.variantIds().stream().distinct().count() != request.variantIds().size()))
            throw new BusinessException("Vui lòng chọn phiên bản hợp lệ thuộc sản phẩm.");
        for (var id : ids) data.lockVariant(id);
        var sale = sales.findById(productId).orElseGet(ProductSale::new);
        sale.setProductId(productId); sale.setDiscountType(type); sale.setDiscountValue(value);
        sale.setPercentage("percentage".equals(type) ? value : null);
        sale.setStart(request.start()); sale.setEnd(request.end()); sale.setEnabled(request.enabled());
        sale.setVariantIds(request.variantIds() == null ? null : request.variantIds().toArray(Integer[]::new));
        sales.saveAndFlush(sale);
        return get(productId);
    }
    @Override @Transactional public void remove(Integer productId) {
        name(productId);
        var ids = data.variantIds(productId);
        for (var id : ids) data.lockVariant(id);
        var sale = sales.findById(productId).orElse(null);
        if (sale != null) { sale.setEnabled(false); sales.saveAndFlush(sale); }
        synchronizer.sync(ids);
        sales.deleteById(productId); prices.deleteByProductId(productId);
        prices.flush(); sales.flush();
    }
    private String name(Integer id) {
        return data.productName(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã sản phẩm này."));
    }
}
