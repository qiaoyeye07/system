package com.fleamarket.security;

import com.fleamarket.domain.User;
import com.fleamarket.exception.BusinessException;
import com.fleamarket.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static boolean isAdmin() {
        User user = getCurrentUser();
        return user.getRole() == com.fleamarket.domain.enums.UserRole.ADMIN;
    }
}
