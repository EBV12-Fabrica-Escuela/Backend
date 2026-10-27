package com.ebv12.backend.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateServiceRequest(
        @NotBlank(message = "es obligatorio") String name,
        @NotBlank(message = "es obligatorio") String description,
        @NotBlank(message = "es obligatorio") String category,
        @NotNull(message = "es obligatorio") @DecimalMin(value = "0.01", message = "debe ser mayor que cero") BigDecimal price,
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor a cero") Integer durationMinutes,
        UUID providerId
) {
}
