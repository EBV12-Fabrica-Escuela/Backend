package com.ebv12.backend.service;

import com.ebv12.backend.domain.AvailabilitySlot;
import com.ebv12.backend.domain.ServiceEntity;
import com.ebv12.backend.repository.AvailabilitySlotRepository;
import com.ebv12.backend.repository.ReservationRepository;
import com.ebv12.backend.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el criterio no funcional de HU-03:
 * "Solo usuarios autenticados pueden reservar".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void shouldRejectReservationWithoutAuthorizationHeader() throws Exception {
        ServiceEntity service = createService();
        AvailabilitySlot slot = createSlot(service);

        String body = """
                {
                  "clienteId": "%s",
                  "servicioId": "%s",
                  "slotId": "%s",
                  "observaciones": "sin token"
                }
                """.formatted(UUID.randomUUID(), service.getId(), slot.getId());

        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptReservationWithAuthorizationHeader() throws Exception {
        ServiceEntity service = createService();
        AvailabilitySlot slot = createSlot(service);

        String body = """
                {
                  "clienteId": "%s",
                  "servicioId": "%s",
                  "slotId": "%s",
                  "observaciones": "con token"
                }
                """.formatted(UUID.randomUUID(), service.getId(), slot.getId());

        mockMvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer token-de-prueba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private ServiceEntity createService() {
        ServiceEntity service = new ServiceEntity();
        service.setProveedorId(UUID.randomUUID());
        service.setNombre("Masaje relajante");
        service.setCategoria("Bienestar");
        service.setDescripcion("Masaje de 45 minutos");
        service.setPrecio(new BigDecimal("70000.00"));
        service.setDuracionMinutos(45);
        service.setActivo(true);
        return serviceRepository.save(service);
    }

    private AvailabilitySlot createSlot(ServiceEntity service) {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setServicio(service);
        slot.setFecha(LocalDate.now().plusDays(1));
        slot.setHoraInicio(LocalTime.of(15, 0));
        slot.setHoraFin(LocalTime.of(15, 45));
        slot.setDisponible(true);
        return availabilitySlotRepository.save(slot);
    }
}
