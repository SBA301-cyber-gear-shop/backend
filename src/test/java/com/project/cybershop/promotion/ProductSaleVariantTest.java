package com.project.cybershop.promotion;

import com.project.cybershop.promotion.internal.entity.ProductSale;
import com.project.cybershop.promotion.internal.entity.ProductSalePrice;
import com.project.cybershop.promotion.internal.repository.ProductSaleDataRepository;
import com.project.cybershop.promotion.internal.repository.ProductSalePriceRepository;
import com.project.cybershop.promotion.internal.repository.ProductSaleRepository;
import com.project.cybershop.promotion.internal.service.impl.ProductSalePriceServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductSaleVariantTest {
    @Test
    void selectedVariantIsDiscountedAndUnselectedVariantIsRestored() {
        var sales = mock(ProductSaleRepository.class);
        var prices = mock(ProductSalePriceRepository.class);
        var data = mock(ProductSaleDataRepository.class);
        var now = LocalDateTime.of(2026, 11, 12, 10, 0);
        var sale = new ProductSale();
        sale.setProductId(7);
        sale.setEnabled(true);
        sale.setDiscountType("percentage");
        sale.setDiscountValue(new BigDecimal("10"));
        sale.setStart(now.minusHours(1));
        sale.setEnd(now.plusHours(1));
        sale.setVariantIds(new Integer[]{101});
        when(sales.existsById(7)).thenReturn(true);
        when(sales.findById(7)).thenReturn(Optional.of(sale));
        when(data.variant(101)).thenReturn(Optional.of(new ProductSaleDataRepository.Variant(101, 7, "A", new BigDecimal("1000"), null)));
        when(data.variant(102)).thenReturn(Optional.of(new ProductSaleDataRepository.Variant(102, 7, "B", new BigDecimal("900"), new BigDecimal("1000"))));
        var prior = new ProductSalePrice();
        prior.setVariantId(102);
        prior.setProductId(7);
        prior.setRegularPrice(new BigDecimal("1000"));
        prior.setAppliedPrice(new BigDecimal("900"));
        prior.setAppliedOriginalPrice(new BigDecimal("1000"));
        when(prices.findById(101)).thenReturn(Optional.empty());
        when(prices.findById(102)).thenReturn(Optional.of(prior));
        var service = new ProductSalePriceServiceImpl(sales, prices, data,
                Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC));

        service.sync(List.of(101, 102));

        verify(data).applyPrice(eq(101), eq(new BigDecimal("900")), eq(new BigDecimal("1000")));
        verify(data).applyPrice(eq(102), eq(new BigDecimal("1000")), isNull());
    }
}
