package com.project.cybershop.identity.internal.service;

import com.project.cybershop.identity.api.dto.RegisterRequest;
import com.project.cybershop.identity.api.dto.UserDto;
import com.project.cybershop.identity.internal.entity.Role;
import com.project.cybershop.identity.internal.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role", qualifiedByName = "formatRole")
    UserDto toDto(User user);

    List<UserDto> toDtoList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request);

    @Named("formatRole")
    default String formatRole(Role role) {
        if (role == null || role.getName() == null) return null;
        return switch (role.getName().toUpperCase()) {
            case "ADMIN" -> "Admin";
            case "STAFF" -> "Staff";
            case "TECHNICIAN" -> "Technician";
            case "CUSTOMER" -> "Customer";
            default -> role.getName();
        };
    }
}
