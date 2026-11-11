
package com.project.cybershop.order.internal.controller;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.identity.api.dto.response.UserDto;
import com.project.cybershop.order.api.dto.request.AddToCartRequest;
import com.project.cybershop.order.api.dto.request.BatchAddToCartRequest;
import com.project.cybershop.order.api.dto.request.UpdateCartItemRequest;
import com.project.cybershop.order.internal.service.CartService;
import com.project.cybershop.shared.dto.ApiResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.project.cybershop.order.api.dto.response.CartResponse;


@RestController
@RequestMapping("/api/orders/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final IdentityApi identityApi;

    // =========================================
    // THÊM SẢN PHẨM VÀO GIỎ HÀNG
    // =========================================
    @PostMapping("/items")
    public ResponseEntity<ApiResponseDTO<Void>> addToCart(
            @Valid @RequestBody AddToCartRequest request
    ) {

        Integer userId = getCurrentUserId();

        cartService.addToCart(userId, request);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Thêm sản phẩm vào giỏ hàng thành công",
                        null
                )
        );
    }

    // =========================================
    // THÊM NHIỀU SẢN PHẨM / LINH KIỆN PC VÀO GIỎ HÀNG (BATCH)
    // =========================================
    @PostMapping("/items/batch")
    public ResponseEntity<ApiResponseDTO<Void>> addBatchToCart(
            @Valid @RequestBody BatchAddToCartRequest request
    ) {

        Integer userId = getCurrentUserId();

        cartService.addBatchToCart(userId, request.items(), request.pcCustomId());

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Thêm danh sách linh kiện vào giỏ hàng thành công",
                        null
                )
        );
    }

    // =========================================
    // LẤY USER ID TỪ JWT ĐÃ XÁC THỰC
    // =========================================
    private Integer getCurrentUserId() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {

            throw new InsufficientAuthenticationException(
                    "Vui lòng đăng nhập để thêm vào giỏ hàng"
            );
        }

        String email = auth.getName();

        UserDto user = identityApi
                .getUserByEmail(email)
                .filter(u ->
                        Boolean.TRUE.equals(u.getIsActive())
                )
                .orElseThrow(() ->
                        new InsufficientAuthenticationException(
                                "Tài khoản không tồn tại hoặc đã bị khóa"
                        )
                );

        return user.getId();
    }


    // =========================================
    // XEM GIỎ HÀNG CỦA NGƯỜI DÙNG
    // =========================================
    @GetMapping
    public ResponseEntity<ApiResponseDTO<CartResponse>> getMyCart() {

        // 1. Lấy ID người dùng đang đăng nhập
        Integer userId = getCurrentUserId();

        // 2. Lấy thông tin giỏ hàng từ Service
        CartResponse cartResponse =
                cartService.getMyCart(userId);

        // 3. Trả về dữ liệu cho frontend
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Lấy thông tin giỏ hàng thành công",
                        cartResponse
                )
        );
    }


    // =========================================
    // CẬP NHẬT SỐ LƯỢNG SẢN PHẨM TRONG GIỎ
    // =========================================
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponseDTO<Void>> updateCartItem(
            @PathVariable Integer cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {

        // 1. Lấy ID người dùng đang đăng nhập
        Integer userId = getCurrentUserId();

        // 2. Gọi Service cập nhật số lượng
        cartService.updateCartItem(
                userId,
                cartItemId,
                request
        );

        // 3. Trả về kết quả thành công
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Cập nhật số lượng sản phẩm thành công",
                        null
                )
        );
    }


    // =========================================
    // XÓA SẢN PHẨM KHỎI GIỎ HÀNG
    // =========================================
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponseDTO<Void>> removeCartItem(
            @PathVariable Integer cartItemId
    ) {

        // 1. Lấy ID người dùng đang đăng nhập
        Integer userId = getCurrentUserId();

        // 2. Gọi Service xóa sản phẩm khỏi giỏ
        cartService.removeCartItem(
                userId,
                cartItemId
        );

        // 3. Trả kết quả thành công
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Xóa sản phẩm khỏi giỏ hàng thành công",
                        null
                )
        );
    }

    // =========================================
    // XÓA NGUYÊN BỘ PC KHỎI GIỎ HÀNG
    // =========================================
    @DeleteMapping("/items/pc-build/{pcCustomId}")
    public ResponseEntity<ApiResponseDTO<Void>> removePcSet(
            @PathVariable Integer pcCustomId
    ) {
        Integer userId = getCurrentUserId();
        cartService.removePcSet(userId, pcCustomId);
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Xóa bộ PC khỏi giỏ hàng thành công",
                        null
                )
        );
    }

    // =========================================
    // TÁCH BỘ PC RA MUA LẺ
    // =========================================
    @PostMapping("/items/pc-build/{pcCustomId}/detach")
    public ResponseEntity<ApiResponseDTO<Void>> detachPcSet(
            @PathVariable Integer pcCustomId
    ) {
        Integer userId = getCurrentUserId();
        cartService.detachPcSet(userId, pcCustomId);
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Đã tách bộ PC thành các linh kiện mua lẻ",
                        null
                )
        );
    }

}
