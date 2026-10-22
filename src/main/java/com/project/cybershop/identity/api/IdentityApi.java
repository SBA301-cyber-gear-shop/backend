package com.project.cybershop.identity.api;

import com.project.cybershop.identity.api.dto.response.UserDto;

import java.util.Optional;

public interface IdentityApi {
    default UserDto requireActiveUser(String email) {
        return getUserByEmail(email).filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .orElseThrow(() -> new org.springframework.security.authentication.InsufficientAuthenticationException(
                        "Tài khoản không tồn tại hoặc đã bị khóa."));
    }
    Optional<UserDto> getUserById(Integer id);
    Optional<UserDto> getUserByEmail(String email);
    boolean existsById(Integer id);
}
