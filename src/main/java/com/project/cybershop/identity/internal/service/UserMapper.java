package com.project.cybershop.identity.internal.service;

import com.project.cybershop.identity.api.dto.UserDto;
import com.project.cybershop.identity.internal.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole() != null ? formatRole(user.getRole().getName()) : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String formatRole(String role) {
        if (role == null) return null;
        return switch (role.toUpperCase()) {
            case "ADMIN" -> "Admin";
            case "STAFF" -> "Staff";
            case "TECHNICIAN" -> "Technician";
            case "CUSTOMER" -> "Customer";
            default -> role;
        };
    }
}
