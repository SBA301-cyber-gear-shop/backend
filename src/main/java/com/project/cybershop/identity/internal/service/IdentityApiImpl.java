package com.project.cybershop.identity.internal.service;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.identity.api.dto.UserDto;
import com.project.cybershop.identity.internal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class IdentityApiImpl implements IdentityApi {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public IdentityApiImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

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
