package com.project.cybershop.aftersales.internal.controller;

import com.project.cybershop.aftersales.api.dto.WarrantyCardDto;
import com.project.cybershop.aftersales.internal.service.Actor;
import com.project.cybershop.aftersales.internal.service.CurrentUser;
import com.project.cybershop.aftersales.internal.service.WarrantyCardService;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warranty-cards")
@Tag(name = "Warranty Cards", description = "API tra cứu thẻ bảo hành cho nhân viên")
public class WarrantyCardController {

    private final WarrantyCardService cardService;
    private final CurrentUser currentUser;

    public WarrantyCardController(WarrantyCardService cardService, CurrentUser currentUser) {
        this.cardService = cardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Tra cứu thẻ theo serial, mã đơn, số điện thoại hoặc mã phiếu (so khớp chính xác)")
    public ResponseEntity<ApiResponseDTO<List<WarrantyCardDto>>> search(@RequestParam("q") String query) {
        currentUser.require(Actor.STAFF, Actor.TECHNICIAN, Actor.ADMIN);
        return ResponseEntity.ok(ApiResponseDTO.success("Tra cứu thẻ bảo hành thành công", cardService.search(query)));
    }
}
