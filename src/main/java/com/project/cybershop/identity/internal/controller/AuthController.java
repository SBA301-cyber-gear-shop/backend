package com.project.cybershop.identity.internal.controller;

import com.project.cybershop.identity.api.dto.AuthResponse;
import com.project.cybershop.identity.api.dto.LoginRequest;
import com.project.cybershop.identity.api.dto.RegisterRequest;
import com.project.cybershop.identity.api.dto.UserDto;
import com.project.cybershop.identity.internal.service.AuthService;
import com.project.cybershop.shared.config.JwtUtil;
import com.project.cybershop.shared.dto.ApiResponseDTO;
import com.project.cybershop.shared.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API Xác thực người dùng (Đăng ký, Đăng nhập, Thông tin tài khoản)")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản khách hàng mới")
    public ResponseEntity<ApiResponseDTO<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng Email/Số điện thoại và Mật khẩu")
    public ResponseEntity<ApiResponseDTO<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponseDTO.success("Đăng nhập thành công", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại qua Bearer Token")
    public ResponseEntity<ApiResponseDTO<UserDto>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("Vui lòng cung cấp Authorization header Bearer token");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("Token không hợp lệ hoặc đã hết hạn");
        }
        String email = jwtUtil.getEmailFromToken(token);
        UserDto userDto = authService.getCurrentUser(email);
        return ResponseEntity.ok(ApiResponseDTO.success("Lấy thông tin tài khoản thành công", userDto));
    }
}
