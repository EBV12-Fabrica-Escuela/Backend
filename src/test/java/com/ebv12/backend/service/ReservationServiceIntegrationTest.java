package com.ebv12.backend.service;

import com.ebv12.backend.api.dto.CreateReservationRequest;
import com.ebv12.backend.domain.AvailabilitySlot;
import com.ebv12.backend.domain.Reservation;
import com.ebv12.backend.domain.ReservationStatus;
import com.ebv12.backend.domain.ServiceEntity;
import com.ebv12.backend.repository.AvailabilitySlotRepository;
import com.ebv12.backend.repository.ReservationRepository;
import com.ebv12.backend.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void clean() {
        reservationRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
        serviceRepository.deleteAll();
    }

    @Test
    void shouldCreateReservationAndMarkSlotUnavailable() {
        ServiceEntity service = createService();
        AvailabilitySlot slot = createSlot(service);

        CreateReservationRequest request = new CreateReservationRequest(UUID.randomUUID(), service.getId(), slot.getId(), "Primera reserva");

        Reservation created = reservationService.createReservation(request);

        assertNotNull(created.getId());
        assertEquals(ReservationStatus.PENDIENTE, created.getEstado());
        AvailabilitySlot updatedSlot = availabilitySlotRepository.findById(slot.getId()).orElseThrow();
        assertFalse(updatedSlot.isDisponible());
    }

    @Test
    void shouldRejectReservationWhenSlotAlreadyTaken() {
        ServiceEntity service = createService();
        AvailabilitySlot slot = createSlot(service);

        reservationService.createReservation(new CreateReservationRequest(UUID.randomUUID(), service.getId(), slot.getId(), null));

        assertThrows(SlotNotAvailableException.class,
                () -> reservationService.createReservation(new CreateReservationRequest(UUID.randomUUID(), service.getId(), slot.getId(), null)));
    }

    private ServiceEntity createService() {
        ServiceEntity service = new ServiceEntity();
        service.setProveedorId(UUID.randomUUID());
        service.setNombre("Corte de cabello");
        service.setCategoria("Belleza");
        service.setDescripcion("Servicio de peluquería");
        service.setPrecio(new BigDecimal("35000.00"));
        service.setDuracionMinutos(60);
        service.setActivo(true);
        return serviceRepository.save(service);
    }

    private AvailabilitySlot createSlot(ServiceEntity service) {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setServicio(service);
        slot.setFecha(LocalDate.now().plusDays(1));
        slot.setHoraInicio(LocalTime.of(10, 0));
        slot.setHoraFin(LocalTime.of(11, 0));
        slot.setDisponible(true);
        return availabilitySlotRepository.save(slot);
    }
}
