package com.ebv12.backend.api.dto;

import com.ebv12.backend.domain.User;
import com.ebv12.backend.domain.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean enabled,
        boolean termsAccepted
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.isTermsAccepted()
        );
    }
}
