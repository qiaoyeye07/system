package com.fleamarket.controller;

import com.fleamarket.dto.request.LoginRequest;
import com.fleamarket.dto.request.RegisterRequest;
import com.fleamarket.dto.response.ApiResponse;
import com.fleamarket.dto.response.LoginResponse;
import com.fleamarket.dto.response.UserResponse;
import com.fleamarket.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.error(
                    com.fleamarket.exception.ErrorCode.PASSWORD_MISMATCH);
        }
        UserResponse user = authService.register(request);
        return ApiResponse.success("注册成功，请登录", user);
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/api/init")
    public ApiResponse<Map<String, Object>> initAdmin(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty()) {
            return ApiResponse.error(com.fleamarket.exception.ErrorCode.BAD_REQUEST, "用户名不能为空");
        }
        if (password == null || password.length() < 8) {
            return ApiResponse.error(com.fleamarket.exception.ErrorCode.BAD_REQUEST, "密码长度至少 8 个字符");
        }

        UserResponse admin = authService.initAdmin(username.trim(), password);
        Map<String, Object> data = Map.of(
                "id", admin.getId(),
                "username", admin.getUsername(),
                "role", admin.getRole()
        );
        return ApiResponse.success("管理员初始化成功", data);
    }

    @GetMapping("/api/auth/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        Long userId = com.fleamarket.security.SecurityUtils.getCurrentUserId();
        UserResponse user = authService.getUserById(userId);
        return ApiResponse.success(user);
    }

    @GetMapping("/api/users/search")
    public ApiResponse<UserResponse> searchUser(@RequestParam String username) {
        return ApiResponse.success(authService.getUserByUsername(username));
    }

    @GetMapping("/api/users/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(authService.getUserById(id));
    }
}
