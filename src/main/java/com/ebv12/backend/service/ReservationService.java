package com.ebv12.backend.service;

import com.ebv12.backend.api.dto.CreateReservationRequest;
import com.ebv12.backend.domain.AvailabilitySlot;
import com.ebv12.backend.domain.Reservation;
import com.ebv12.backend.domain.ReservationStatus;
import com.ebv12.backend.repository.AvailabilitySlotRepository;
import com.ebv12.backend.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(AvailabilitySlotRepository availabilitySlotRepository,
                              ReservationRepository reservationRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Reservation createReservation(CreateReservationRequest request) {
        AvailabilitySlot slot = availabilitySlotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("El slot solicitado no existe"));

        if (!slot.getServicio().getId().equals(request.servicioId())) {
            throw new ResourceNotFoundException("El slot no pertenece al servicio indicado");
        }

        if (!slot.isDisponible()) {
            throw new SlotNotAvailableException("El horario ya no está disponible");
        }

        slot.setDisponible(false);

        Reservation reservation = new Reservation();
        reservation.setClienteId(request.clienteId());
        reservation.setServicio(slot.getServicio());
        reservation.setSlot(slot);
        reservation.setFecha(slot.getFecha());
        reservation.setHoraInicio(slot.getHoraInicio());
        reservation.setEstado(ReservationStatus.PENDIENTE);
        reservation.setObservaciones(request.observaciones());

        return reservationRepository.save(reservation);
    }
}
