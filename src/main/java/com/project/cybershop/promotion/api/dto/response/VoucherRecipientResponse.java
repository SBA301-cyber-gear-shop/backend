package com.project.cybershop.promotion.api.dto.response;

public record VoucherRecipientResponse(Integer id, String name, String email, boolean assigned, boolean used) {}
