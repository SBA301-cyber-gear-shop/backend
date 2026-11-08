package com.project.cybershop.promotion.internal.service.impl;

import com.project.cybershop.promotion.internal.entity.ProductSalePrice;
import com.project.cybershop.promotion.internal.service.ProductSalePriceService;
import com.project.cybershop.promotion.internal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class ProductSalePriceServiceImpl implements ProductSalePriceService {
    private final ProductSaleRepository sales;
    private final ProductSalePriceRepository prices;
    private final ProductSaleDataRepository data;
    private final Clock clock;

    @Transactional
    @Override
    public void sync(List<Integer> variantIds) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (Integer id : variantIds.stream().distinct().sorted().toList()) syncOne(id, now);
    }

    private void syncOne(Integer id, LocalDateTime now) {
        var before = data.variant(id).orElse(null);
        if (before == null || (!sales.existsById(before.productId()) && !prices.existsById(id))) return;
        data.lockVariant(id);
        var variant = data.variant(id).orElseThrow();
        var sale = sales.findById(variant.productId()).orElse(null);
        var snapshot = prices.findById(id).orElse(null);
        if (snapshot == null) {
            snapshot = new ProductSalePrice();
            snapshot.setVariantId(id); snapshot.setProductId(variant.productId());
            snapshot.setRegularPrice(variant.price()); snapshot.setOriginalPrice(variant.originalPrice());
        } else if (!same(snapshot.getAppliedPrice(), variant.price()) || !same(snapshot.getAppliedOriginalPrice(), variant.originalPrice())) {
            snapshot.setRegularPrice(variant.price()); snapshot.setOriginalPrice(variant.originalPrice());
        }
        boolean active = sale != null && sale.isEnabled() && !now.isBefore(sale.getStart()) && now.isBefore(sale.getEnd());
        BigDecimal original = snapshot.getOriginalPrice();
        BigDecimal base = original != null && original.signum() > 0 ? original : snapshot.getRegularPrice();
        BigDecimal amount = snapshot.getRegularPrice();
        if (active) {
            BigDecimal value = sale.getDiscountValue() != null ? sale.getDiscountValue() : sale.getPercentage();
            amount = "fixed_amount".equals(sale.getDiscountType())
                    ? base.subtract(value).max(BigDecimal.ZERO)
                    : base.multiply(BigDecimal.valueOf(100).subtract(value)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
        // Sale không làm giá cao hơn một mức giá bán thường đang rẻ hơn.
        amount = amount.min(snapshot.getRegularPrice());
        BigDecimal displayedOriginal = active ? base : original;
        boolean changed = !same(amount, variant.price()) || !same(displayedOriginal, variant.originalPrice());
        if (changed) data.applyPrice(id, amount, displayedOriginal);
        if (!same(snapshot.getAppliedPrice(), amount) || !same(snapshot.getAppliedOriginalPrice(), displayedOriginal)) {
            snapshot.setAppliedPrice(amount); snapshot.setAppliedOriginalPrice(displayedOriginal);
            prices.saveAndFlush(snapshot);
        }
    }
    private boolean same(BigDecimal a, BigDecimal b) { return a == null ? b == null : b != null && a.compareTo(b) == 0; }
}
