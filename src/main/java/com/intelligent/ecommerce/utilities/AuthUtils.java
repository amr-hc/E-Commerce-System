package com.intelligent.ecommerce.utilities;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.intelligent.ecommerce.config.CustomUserDetails;

@Component
public class AuthUtils {
    public CustomUserDetails getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return (CustomUserDetails) principal;
        }
        throw new IllegalStateException("User not authenticated");
    }

    public Long getId() {
        return getAuthenticatedUser().getUser().getId();
    }

}
