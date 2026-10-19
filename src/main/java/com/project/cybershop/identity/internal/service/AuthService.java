package com.project.cybershop.identity.internal.service;

import com.project.cybershop.identity.api.dto.response.AuthResponse;
import com.project.cybershop.identity.api.dto.request.LoginRequest;
import com.project.cybershop.identity.api.dto.request.RegisterRequest;
import com.project.cybershop.identity.api.dto.response.UserDto;
import com.project.cybershop.identity.internal.entity.Role;
import com.project.cybershop.identity.internal.entity.User;
import com.project.cybershop.identity.internal.repository.RoleRepository;
import com.project.cybershop.identity.internal.repository.UserRepository;
import com.project.cybershop.order.internal.entity.Cart;
import com.project.cybershop.order.internal.repository.CartRepository;
import com.project.cybershop.shared.config.JwtUtil;
import com.project.cybershop.shared.exception.BusinessException;
import com.project.cybershop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final CartRepository cart;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       UserMapper userMapper, CartRepository cart) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.cart = cart;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email '" + email + "' đã được sử dụng");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String phone = request.getPhone().trim();
            if (userRepository.existsByPhone(phone)) {
                throw new BusinessException("Số điện thoại '" + phone + "' đã được sử dụng");
            }
        }

        Role role = roleRepository.findByNameIgnoreCase("CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role("CUSTOMER", "Khách hàng mua sắm")));

        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        } else {
            user.setPhone(null);
        }
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            user.setAddress(request.getAddress().trim());
        } else {
            user.setAddress(null);
        }

        user = userRepository.save(user);
        cart.save(new Cart());

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().getName());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(userMapper.toDto(user))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identity = request.getIdentity().trim();
        User user = userRepository.findByIdentity(identity)
                .orElseThrow(() -> new BusinessException("Email, số điện thoại hoặc mật khẩu không chính xác"));

        System.out.println(passwordEncoder.encode("111111"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Email, số điện thoại hoặc mật khẩu không chính xác");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException("Tài khoản của bạn đã bị khóa hoặc ngừng kích hoạt");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().getName());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(userMapper.toDto(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản: " + email));
        return userMapper.toDto(user);
    }
}
