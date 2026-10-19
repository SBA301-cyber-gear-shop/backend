package com.project.cybershop.identity.internal.service.impl;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.identity.api.dto.response.UserDto;
import com.project.cybershop.identity.internal.repository.UserRepository;
import com.project.cybershop.identity.internal.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IdentityApiImpl implements IdentityApi {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<UserDto> getUserById(Integer id) {
        return userRepository.findById(id).map(userMapper::toDto);
    }

    @Override
    public Optional<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(userMapper::toDto);
    }

    @Override
    public boolean existsById(Integer id) {
        return userRepository.existsById(id);
    }
}
