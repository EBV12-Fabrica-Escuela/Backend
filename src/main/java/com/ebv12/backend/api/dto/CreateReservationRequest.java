package com.ebv12.backend.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReservationRequest(
        @NotNull UUID clienteId,
        @NotNull UUID servicioId,
        @NotNull UUID slotId,
        String observaciones
) {
}
