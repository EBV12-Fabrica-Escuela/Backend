package com.ebv12.backend.api.dto;

import com.ebv12.backend.domain.Reservation;
import com.ebv12.backend.domain.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID clienteId,
        UUID servicioId,
        UUID slotId,
        LocalDate fecha,
        LocalTime horaInicio,
        ReservationStatus estado,
        String observaciones,
        Instant fechaCreacion
) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getClienteId(),
                reservation.getServicio().getId(),
                reservation.getSlot().getId(),
                reservation.getFecha(),
                reservation.getHoraInicio(),
                reservation.getEstado(),
                reservation.getObservaciones(),
                reservation.getFechaCreacion()
        );
    }
}
