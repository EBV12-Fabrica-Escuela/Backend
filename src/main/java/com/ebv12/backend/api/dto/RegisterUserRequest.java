package com.ebv12.backend.api.dto;

import com.ebv12.backend.domain.UserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterUserRequest(
        @NotBlank(message = "es obligatorio") String name,
        @NotBlank(message = "es obligatorio") @Email(message = "debe tener un formato válido") String email,
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "debe tener mínimo 8 caracteres, una letra y un número")
        String password,
        UserRole role,
        @AssertTrue(message = "debe aceptar los términos y condiciones") boolean termsAccepted
) {
}
