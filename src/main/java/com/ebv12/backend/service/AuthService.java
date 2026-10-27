package com.ebv12.backend.service;

import com.ebv12.backend.api.dto.AuthResponse;
import com.ebv12.backend.api.dto.LoginRequest;
import com.ebv12.backend.api.dto.RegisterUserRequest;
import com.ebv12.backend.api.dto.UserResponse;
import com.ebv12.backend.domain.User;
import com.ebv12.backend.domain.UserRole;
import com.ebv12.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    public AuthService(UserRepository userRepository,
                       SessionTokenService sessionTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.sessionTokenService = sessionTokenService;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String normalizedEmail = request.email().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("El correo ya está registrado");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail.toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null ? request.role() : UserRole.CLIENT);
        user.setTermsAccepted(request.termsAccepted());
        user.setEnabled(true);

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        String token = sessionTokenService.issueToken(user);
        String targetScreen = user.getRole() == UserRole.PROVIDER ? "provider-dashboard" : "client-dashboard";
        return new AuthResponse(token, user.getId(), user.getRole().name(), targetScreen);
    }

    public void logout(String authorizationHeader) {
        sessionTokenService.invalidate(authorizationHeader);
    }
}
