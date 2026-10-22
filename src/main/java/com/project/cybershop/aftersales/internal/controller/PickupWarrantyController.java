package com.project.cybershop.aftersales.internal.controller;

import com.project.cybershop.aftersales.api.PickupWarrantyApi;
import com.project.cybershop.identity.api.IdentityApi;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/me/warranties")
public class PickupWarrantyController {
    private final PickupWarrantyApi warranties;
    private final IdentityApi identity;
    public PickupWarrantyController(PickupWarrantyApi warranties, IdentityApi identity) { this.warranties = warranties; this.identity = identity; }

    @GetMapping
    public List<PickupWarrantyApi.Card> mine(Authentication auth) {
        return warranties.forCustomer(identity.requireActiveUser(auth.getName()).getId());
    }
}
