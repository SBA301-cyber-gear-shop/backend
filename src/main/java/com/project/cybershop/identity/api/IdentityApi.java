package com.project.cybershop.identity.api;

import com.project.cybershop.identity.api.dto.UserDto;

import java.util.Optional;

public interface IdentityApi {
    Optional<UserDto> getUserById(Integer id);
    Optional<UserDto> getUserByEmail(String email);
    boolean existsById(Integer id);
}
