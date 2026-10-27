package com.ebv12.backend.api.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String role,
        String targetScreen
) {
}
