package com.project.cybershop.promotion.internal.service;

import com.project.cybershop.promotion.api.dto.response.CustomerVoucherResponse;
import com.project.cybershop.promotion.api.dto.response.VoucherRecipientResponse;
import java.util.List;

public interface CustomerVoucherService {
    List<CustomerVoucherResponse> wallet(String email);
    List<VoucherRecipientResponse> recipients(String email, Integer id, String search, boolean assignedOnly);
    void grant(String email, Integer id, List<Integer> userIds);
    void revoke(String email, Integer id, Integer userId);
}
