package com.ebv12.backend.service;

import com.ebv12.backend.domain.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionTokenService {

    private final Map<String, UUID> activeTokens = new ConcurrentHashMap<>();

    public String issueToken(User user) {
        String token = UUID.randomUUID().toString();
        activeTokens.put(token, user.getId());
        return token;
    }

    public Optional<UUID> resolveUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeTokens.get(token));
    }

    public boolean isTokenValid(String authorizationHeader) {
        return resolveUserId(authorizationHeader).isPresent();
    }

    public void invalidate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authorizationHeader.substring(7).trim();
        activeTokens.remove(token);
    }
}
