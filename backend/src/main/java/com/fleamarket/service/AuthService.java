package com.fleamarket.service;

import com.fleamarket.domain.User;
import com.fleamarket.domain.enums.UserRole;
import com.fleamarket.dto.request.LoginRequest;
import com.fleamarket.dto.request.RegisterRequest;
import com.fleamarket.dto.response.LoginResponse;
import com.fleamarket.dto.response.UserResponse;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
import com.fleamarket.repository.RatingRepository;
import com.fleamarket.repository.ProductRepository;
import com.fleamarket.repository.UserRepository;
import com.fleamarket.security.JwtTokenProvider;
import com.fleamarket.domain.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RatingRepository ratingRepository;
    private final ProductRepository productRepository;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();

        if (!username.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-]+$")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名只能包含中文、字母、数字、下划线和连字符");
        }
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        return toUserResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public UserResponse initAdmin(String username, String password) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            throw new BusinessException(ErrorCode.ADMIN_ALREADY_EXISTS);
        }

        User admin = User.builder()
                .username(username.trim())
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();

        admin = userRepository.save(admin);
        return toUserResponse(admin);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        return toUserResponse(user);
    }

    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Transactional
    public UserResponse toggleUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能禁用管理员账号");
        }
        user.setEnabled(enabled);
        userRepository.save(user);
        return toUserResponse(user);
    }

    public UserResponse toUserResponse(User user) {
        Double avgScore = ratingRepository.getAverageScoreByRatedUser(user.getId());
        Long ratingCount = ratingRepository.countByRatedUser(user.getId());
        Long activeProductCount = productRepository.countBySellerIdAndStatus(user.getId(), ProductStatus.ACTIVE);

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .avgScore(avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : 0.0)
                .ratingCount(ratingCount)
                .activeProductCount(activeProductCount)
                .build();
    }
}
