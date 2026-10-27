package com.ebv12.backend.api.dto;

import com.ebv12.backend.domain.AvailabilitySlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityResponse(
        UUID id,
        UUID servicioId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean disponible
) {
    public static AvailabilityResponse from(AvailabilitySlot slot) {
        return new AvailabilityResponse(
                slot.getId(),
                slot.getServicio().getId(),
                slot.getFecha(),
                slot.getHoraInicio(),
                slot.getHoraFin(),
                slot.isDisponible()
        );
    }
}
