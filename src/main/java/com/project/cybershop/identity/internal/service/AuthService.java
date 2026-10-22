package com.project.cybershop.identity.internal.service;

import com.project.cybershop.identity.api.dto.request.LoginRequest;
import com.project.cybershop.identity.api.dto.request.RegisterRequest;
import com.project.cybershop.identity.api.dto.response.AuthResponse;
import com.project.cybershop.identity.api.dto.response.UserDto;
<<<<<<< Updated upstream
import com.project.cybershop.identity.internal.entity.Role;
import com.project.cybershop.identity.internal.entity.User;
import com.project.cybershop.identity.internal.mapper.UserMapper;
import com.project.cybershop.identity.internal.repository.RoleRepository;
import com.project.cybershop.identity.internal.repository.UserRepository;
import com.project.cybershop.order.internal.entity.Cart;
import com.project.cybershop.order.internal.repository.CartRepository;
import com.project.cybershop.shared.config.JwtUtil;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
=======
>>>>>>> Stashed changes

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserDto getCurrentUser(String email);
}
