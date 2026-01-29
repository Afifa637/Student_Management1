package com.universityofengineers.sms.util;

import com.universityofengineers.sms.entity.Role;
import com.universityofengineers.sms.security.UserPrincipal;
import com.universityofengineers.sms.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static UserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException("Not authenticated.");
        }
        return principal;
    }

    public static Long currentAccountId() {
        return currentPrincipal().getId();
    }

    public static Role currentRole() {
        return currentPrincipal().getRole();
    }

    public static boolean isTeacher() {
        return currentRole() == Role.TEACHER;
    }

    public static boolean isStudent() {
        return currentRole() == Role.STUDENT;
    }
}
