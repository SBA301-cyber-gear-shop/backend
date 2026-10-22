package com.project.cybershop.identity.internal.service;

import com.project.cybershop.identity.api.dto.request.LoginRequest;
import com.project.cybershop.identity.api.dto.request.RegisterRequest;
import com.project.cybershop.identity.api.dto.response.AuthResponse;
import com.project.cybershop.identity.api.dto.response.UserDto;
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserDto getCurrentUser(String email);
}
