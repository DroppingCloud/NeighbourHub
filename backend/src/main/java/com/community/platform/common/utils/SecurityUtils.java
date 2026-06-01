package com.community.platform.common.utils;

import com.community.platform.entity.User;

public class SecurityUtils {
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();
    public static void setCurrentUser(User user) { CURRENT_USER.set(user); }
    public static User getCurrentUser() { return CURRENT_USER.get(); }
    public static void clearCurrentUser() { CURRENT_USER.remove(); }
}