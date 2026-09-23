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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceEndpointsIntegrationTest {

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
    void shouldReturnPagedServicesAndAvailability() throws Exception {
        ServiceEntity service = new ServiceEntity();
        service.setProveedorId(UUID.randomUUID());
        service.setNombre("Manicure");
        service.setCategoria("Belleza");
        service.setDescripcion("Manicure profesional");
        service.setPrecio(new BigDecimal("50000.00"));
        service.setDuracionMinutos(45);
        service.setActivo(true);
        service = serviceRepository.save(service);

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setServicio(service);
        slot.setFecha(LocalDate.now().plusDays(2));
        slot.setHoraInicio(LocalTime.of(9, 0));
        slot.setHoraFin(LocalTime.of(9, 45));
        slot.setDisponible(true);
        availabilitySlotRepository.save(slot);

        mockMvc.perform(get("/api/servicios").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value("Manicure"));

        mockMvc.perform(get("/api/servicios/{id}/disponibilidad", service.getId())
                        .param("fecha", slot.getFecha().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].servicioId").value(service.getId().toString()))
                .andExpect(jsonPath("$.data[0].disponible").value(true));
    }
}
