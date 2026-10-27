package com.ebv12.backend.api;

import com.ebv12.backend.api.dto.ApiDataResponse;
import com.ebv12.backend.api.dto.AuthResponse;
import com.ebv12.backend.api.dto.LoginRequest;
import com.ebv12.backend.api.dto.RegisterUserRequest;
import com.ebv12.backend.api.dto.UserResponse;
import com.ebv12.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ApiDataResponse<UserResponse>> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiDataResponse<>(user));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiDataResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new ApiDataResponse<>(authService.login(request)));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiDataResponse<String>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(new ApiDataResponse<>("Sesión cerrada correctamente"));
    }
}
