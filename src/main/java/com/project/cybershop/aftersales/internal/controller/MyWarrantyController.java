package com.project.cybershop.aftersales.internal.controller;

import com.project.cybershop.aftersales.api.dto.response.MyWarrantyCardDto;
import com.project.cybershop.aftersales.internal.service.CurrentUser;
import com.project.cybershop.aftersales.internal.service.WarrantyCardService;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my")
@Tag(name = "My Warranty", description = "API bảo hành của khách hàng đang đăng nhập")
public class MyWarrantyController {

    private final WarrantyCardService cardService;
    private final CurrentUser currentUser;

    public MyWarrantyController(WarrantyCardService cardService, CurrentUser currentUser) {
        this.cardService = cardService;
        this.currentUser = currentUser;
    }

    @GetMapping("/warranty-cards")
    @Operation(summary = "Thẻ bảo hành và tiến trình phiếu của tôi, chỉ gồm nội dung công khai")
    public ResponseEntity<ApiResponseDTO<List<MyWarrantyCardDto>>> myCards() {
        // Chỉ trả dữ liệu của chính người gọi, nên biết mã phiếu của người khác cũng không xem được.
        Integer userId = currentUser.require().id();
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy thẻ bảo hành thành công", cardService.myCards(userId)));
    }
}
