package com.ebv12.backend.service;

import com.ebv12.backend.api.dto.ConfigureScheduleRequest;
import com.ebv12.backend.api.dto.CreateReservationRequest;
import com.ebv12.backend.domain.AvailabilitySlot;
import com.ebv12.backend.domain.Reservation;
import com.ebv12.backend.domain.ReservationStatus;
import com.ebv12.backend.domain.ServiceEntity;
import com.ebv12.backend.repository.AvailabilitySlotRepository;
import com.ebv12.backend.repository.ProviderScheduleRepository;
import com.ebv12.backend.repository.ReservationRepository;
import com.ebv12.backend.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private ProviderScheduleService providerScheduleService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Autowired
    private ProviderScheduleRepository providerScheduleRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void clean() {
        reservationRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
        providerScheduleRepository.deleteAll();
        serviceRepository.deleteAll();
    }

    @Test
    void shouldCreateReservationAndMarkSlotUnavailable() {
        ServiceEntity service = createService();
        AvailabilitySlot slot = createSlot(service, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), true);

        CreateReservationRequest request = new CreateReservationRequest(
                UUID.randomUUID(), service.getId(), slot.getId(), "Primera reserva");
        Reservation created = reservationService.createReservation(request);

        assertNotNull(created.getId());
        assertEquals(ReservationStatus.PENDIENTE, created.getEstado());
        AvailabilitySlot updatedSlot = availabilitySlotRepository.findById(slot.getId()).orElseThrow();
        assertFalse(updatedSlot.isDisponible());
    }

    @Test
    void shouldFilterAvailabilityByServiceProviderDateAndSufficientDuration() {
        ServiceEntity service = createService();
        LocalDate date = LocalDate.now().plusDays(1);
        configureSchedule(service.getProveedorId(), date);

        List<AvailabilitySlot> available = availabilityService.findAvailableSlots(
                service.getId(), date, service.getProveedorId(), service.getDuracionMinutos());
        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(12, 0)),
                available.stream().map(AvailabilitySlot::getHoraInicio).toList());

        assertTrue(availabilityService.findAvailableSlots(
                service.getId(), date, UUID.randomUUID(), service.getDuracionMinutos()).isEmpty());
        assertTrue(availabilityService.findAvailableSlots(
                service.getId(), date, service.getProveedorId(), service.getDuracionMinutos() - 1).isEmpty());
    }

    @Test
    void shouldExcludeOccupiedBlockedOutOfScheduleAndPastSlots() {
        ServiceEntity service = createService();
        LocalDate workDate = LocalDate.now().plusDays(1);
        configureSchedule(service.getProveedorId(), workDate);
        createSlot(service, workDate, LocalTime.of(9, 0), LocalTime.of(10, 0), false);

        List<AvailabilitySlot> available = availabilityService.findAvailableSlots(
                service.getId(), workDate, service.getProveedorId(), 60);
        assertEquals(List.of(LocalTime.of(11, 0), LocalTime.of(12, 0)),
                available.stream().map(AvailabilitySlot::getHoraInicio).toList());

        LocalDate outsideScheduleDate = workDate.plusDays(1);
        while (outsideScheduleDate.getDayOfWeek() == workDate.getDayOfWeek()) {
            outsideScheduleDate = outsideScheduleDate.plusDays(1);
        }
        assertTrue(availabilityService.findAvailableSlots(
                service.getId(), outsideScheduleDate, service.getProveedorId(), 60).isEmpty());
        assertTrue(availabilityService.findAvailableSlots(
                service.getId(), LocalDate.now().minusDays(1), service.getProveedorId(), 60).isEmpty());
    }

    @Test
    void shouldRevalidateSlotImmediatelyBeforeReservation() {
        ServiceEntity service = createService();
        LocalDate date = LocalDate.now().plusDays(1);
        configureSchedule(service.getProveedorId(), date);
        AvailabilitySlot slot = availabilityService.findAvailableSlots(
                service.getId(), date, service.getProveedorId(), 60).get(0);

        reservationService.createReservation(new CreateReservationRequest(
                UUID.randomUUID(), service.getId(), slot.getId(), null));

        assertThrows(SlotNotAvailableException.class,
                () -> reservationService.createReservation(new CreateReservationRequest(
                        UUID.randomUUID(), service.getId(), slot.getId(), null)));
        assertFalse(availabilitySlotRepository.findById(slot.getId()).orElseThrow().isDisponible());
    }

    @Test
    void shouldReturnExactMessageWhenDateHasNoAvailableSlots() throws Exception {
        ServiceEntity service = createService();
        LocalDate date = LocalDate.now().plusDays(2);

        mockMvc.perform(get("/api/servicios/{id}/disponibilidad", service.getId())
                        .param("fecha", date.toString())
                        .param("proveedorId", service.getProveedorId().toString())
                        .param("duracionMinutos", "60"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "No hay turnos disponibles para la fecha seleccionada"));
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

    private void configureSchedule(UUID providerId, LocalDate date) {
        providerScheduleService.configure(providerId, new ConfigureScheduleRequest(
                List.of(date.getDayOfWeek().name()),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                List.of(new ConfigureScheduleRequest.ScheduleBreakRequest(
                        LocalTime.of(10, 0), LocalTime.of(11, 0)))));
    }

    private AvailabilitySlot createSlot(ServiceEntity service, LocalDate date, LocalTime start, LocalTime end,
                                        boolean available) {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setServicio(service);
        slot.setFecha(date);
        slot.setHoraInicio(start);
        slot.setHoraFin(end);
        slot.setDisponible(available);
        return availabilitySlotRepository.save(slot);
    }
}
