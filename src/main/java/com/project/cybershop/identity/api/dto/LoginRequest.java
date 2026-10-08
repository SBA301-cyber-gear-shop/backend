package com.project.cybershop.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String identity;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
