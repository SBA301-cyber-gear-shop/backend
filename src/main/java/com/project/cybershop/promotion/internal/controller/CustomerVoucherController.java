package com.project.cybershop.promotion.internal.controller;

import com.project.cybershop.promotion.internal.service.CustomerVoucherService;
import com.project.cybershop.promotion.api.dto.response.CustomerVoucherResponse;
import com.project.cybershop.promotion.api.dto.response.VoucherRecipientResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController @RequiredArgsConstructor
public class CustomerVoucherController {
    private final CustomerVoucherService service;
    public record GrantRequest(@NotEmpty @Size(max=100) List<@NotNull @Positive Integer> userIds) {}
    @GetMapping("/api/me/vouchers") @PreAuthorize("hasRole('CUSTOMER')")
    public List<CustomerVoucherResponse> wallet(Authentication auth) {return service.wallet(auth.getName());}
    @GetMapping("/api/admin/promotions/{id}/recipients") @PreAuthorize("hasRole('ADMIN')")
    public List<VoucherRecipientResponse> recipients(Authentication auth,@PathVariable Integer id,
            @RequestParam(defaultValue="") String search,@RequestParam(defaultValue="true") boolean assignedOnly) {
        return service.recipients(auth.getName(),id,search,assignedOnly);
    }
    @PostMapping("/api/admin/promotions/{id}/recipients") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grant(Authentication auth,@PathVariable Integer id,@Valid @RequestBody GrantRequest request) {service.grant(auth.getName(),id,request.userIds());}
    @DeleteMapping("/api/admin/promotions/{id}/recipients/{userId}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(Authentication auth,@PathVariable Integer id,@PathVariable Integer userId) {service.revoke(auth.getName(),id,userId);}
}
