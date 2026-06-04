package com.community.platform.common.utils;

import com.community.platform.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();
    public static void setCurrentUser(User user) { CURRENT_USER.set(user); }
    public static User getCurrentUser() { return CURRENT_USER.get(); }
    public static void clearCurrentUser() { CURRENT_USER.remove(); }

    /**
     * 从 Spring Security 上下文中获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }
}