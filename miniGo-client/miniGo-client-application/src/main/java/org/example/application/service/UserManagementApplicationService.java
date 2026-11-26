package org.example.application.service;

import lombok.RequiredArgsConstructor;
import org.example.application.dto.RegisterUserRequestDto;
import org.example.application.dto.UpdateUserRequestDto;
import org.example.application.dto.UserResponseDto;
import org.example.application.exception.ResourceNotFoundException;
import org.example.domain.aggregate.User;
import org.example.domain.ports.PasswordEncoder;
import org.example.domain.ports.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserManagementApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto find(Long id) {
        return UserResponseDto.from(requireUser(id));
    }

    @Transactional
    public UserResponseDto register(RegisterUserRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        String username = requireText(request.username(), "用户名不能为空");
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = User.createPasswordAccount(
                username,
                requireText(request.password(), "密码不能为空"),
                request.nickname(),
                request.phone(),
                request.email(),
                passwordEncoder
        );
        return UserResponseDto.from(userRepository.save(user));
    }

    @Transactional
    public UserResponseDto update(Long id, UpdateUserRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        User user = requireUser(id);
        user = user.updateProfile(
                request.nickname(),
                request.avatarUrl(),
                request.phone(),
                request.email()
        );
        if (request.password() != null && !request.password().isBlank()) {
            user = user.changePassword(request.password(), passwordEncoder);
        }
        return UserResponseDto.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        requireUser(id);
        userRepository.deleteById(id);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
