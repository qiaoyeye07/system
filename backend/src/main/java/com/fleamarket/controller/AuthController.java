package com.fleamarket.controller;

import com.fleamarket.domain.User;
import com.fleamarket.domain.enums.UserRole;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
import com.fleamarket.repository.UserRepository;
import com.fleamarket.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/api/init")
    public ApiResponse<Map<String, Object>> initAdmin(@RequestBody Map<String, String> request) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            throw new BusinessException(ErrorCode.ADMIN_ALREADY_EXISTS);
        }

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名不能为空");
        }
        if (password == null || password.length() < 8) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度至少 8 个字符");
        }

        User admin = User.builder()
                .username(username.trim())
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();

        admin = userRepository.save(admin);

        Map<String, Object> data = Map.of(
                "id", admin.getId(),
                "username", admin.getUsername(),
                "role", admin.getRole().name()
        );

        return ApiResponse.success("管理员初始化成功", data);
    }
}
