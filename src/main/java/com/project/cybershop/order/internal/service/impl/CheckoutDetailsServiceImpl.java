package com.project.cybershop.order.internal.service.impl;

import com.project.cybershop.order.api.dto.request.CheckoutDetailsRequest;
import com.project.cybershop.order.api.dto.response.CheckoutDetailsResponse;
import com.project.cybershop.order.internal.service.CheckoutDetailsService;
import com.project.cybershop.order.internal.service.CheckoutService;
import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.promotion.api.PromotionApi;
import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutDetailsServiceImpl implements CheckoutDetailsService {
    private final CheckoutService checkout;
    private final IdentityApi identity;
    private final PromotionApi promotions;

    public CheckoutDetailsServiceImpl(CheckoutService checkout, IdentityApi identity, PromotionApi promotions) {
        this.checkout = checkout; this.identity = identity; this.promotions = promotions;
    }

    @Override
    @Transactional(readOnly=true)
    public CheckoutDetailsResponse preview(String email, CheckoutDetailsRequest request) {
        if (request == null) throw new BusinessException("Thiếu thông tin nhận hàng.");

        String name = trim(request.pickupName()), phone = trim(request.pickupPhone()),
                note = trim(request.note()), code = trim(request.promotionCode());

        if (!name.isEmpty() && name.length() > 100) {
            throw new BusinessException("Tên người nhận phải từ 1 đến 100 ký tự.");
        }

        if (!phone.isEmpty()) {
            String normalizedPhone = phone.replaceAll("[ .()-]", "");
            if (phone.length() > 20 || !normalizedPhone.matches("(?:0[0-9]{9}|\\+84[0-9]{9})")) {
                throw new BusinessException("Số điện thoại nhận hàng không hợp lệ.");
            }
        }

        if (code.length() > 50) throw new BusinessException("Mã voucher quá dài.");

        if (note.length() > 2000) throw new BusinessException("Ghi chú không được vượt quá 2000 ký tự.");

        var user = identity.requireActiveUser(email);
        var cart = checkout.validate(email);
        var quote = promotions.quote(user.getId(), code, cart.totalAmount(), false);

        return new CheckoutDetailsResponse(cart.cartId(), name, phone, note, quote.code(), cart.items(),
                cart.totalAmount(), quote.discount(), cart.totalAmount().subtract(quote.discount()));
    }

    private static String trim(String value) { return value == null ? "" : value.trim(); }
}
