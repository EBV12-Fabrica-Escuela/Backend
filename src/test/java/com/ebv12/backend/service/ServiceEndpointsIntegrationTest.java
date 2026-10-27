package com.ebv12.backend.service;

import com.ebv12.backend.api.dto.ConfigureScheduleRequest;
import com.ebv12.backend.api.dto.CreateServiceRequest;
import com.ebv12.backend.api.dto.RegisterUserRequest;
import com.ebv12.backend.domain.AvailabilitySlot;
import com.ebv12.backend.domain.ServiceEntity;
import com.ebv12.backend.domain.User;
import com.ebv12.backend.domain.UserRole;
import com.ebv12.backend.repository.AvailabilitySlotRepository;
import com.ebv12.backend.repository.ProviderScheduleRepository;
import com.ebv12.backend.repository.ReservationRepository;
import com.ebv12.backend.repository.ServiceRepository;
import com.ebv12.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Autowired
    private ProviderScheduleRepository providerScheduleRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionTokenService sessionTokenService;

    @BeforeEach
    void clean() {
        reservationRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
        providerScheduleRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnPagedServicesAndAvailability() throws Exception {
        ServiceEntity service = createService(UUID.randomUUID(), 45);
        AvailabilitySlot slot = createSlot(service, LocalDate.now().plusDays(2),
                LocalTime.of(9, 0), LocalTime.of(9, 45), true);

        mockMvc.perform(get("/v1/services").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value("Manicure"));

        mockMvc.perform(get("/v1/services/{id}/availability", service.getId())
                        .param("fecha", slot.getFecha().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].servicioId").value(service.getId().toString()))
                .andExpect(jsonPath("$.data[0].disponible").value(true));
    }

    @Test
    void shouldCreateServiceAsAuthenticatedProvider() throws Exception {
        User provider = createProvider();
        String authorization = loginAs(provider);
        CreateServiceRequest request = new CreateServiceRequest(
                "Manicure", "Manicure profesional", "Belleza", new BigDecimal("50000"), 45, null);

        mockMvc.perform(post("/api/servicios")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombre").value("Manicure"))
                .andExpect(jsonPath("$.data.proveedorId").value(provider.getId().toString()));
    }

    @Test
    void shouldRejectServiceWithNonPositivePriceOrDuration() throws Exception {
        String authorization = loginAs(createProvider());

        CreateServiceRequest zeroPrice = new CreateServiceRequest(
                "Manicure", "Manicure profesional", "Belleza", BigDecimal.ZERO, 45, null);
        mockMvc.perform(post("/api/servicios")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroPrice)))
                .andExpect(status().isBadRequest());

        CreateServiceRequest zeroDuration = new CreateServiceRequest(
                "Manicure", "Manicure profesional", "Belleza", new BigDecimal("50000"), 0, null);
        mockMvc.perform(post("/api/servicios")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroDuration)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectServiceWithBlankRequiredFields() throws Exception {
        String authorization = loginAs(createProvider());
        String request = """
                {
                  "name": " ",
                  "description": "",
                  "category": "Belleza",
                  "price": 50000,
                  "durationMinutes": 45
                }
                """;

        mockMvc.perform(post("/api/servicios")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldConfigureWorkingHoursAndBreaksAndReturnOnlyAvailableSlots() throws Exception {
        User provider = createProvider();
        String authorization = loginAs(provider);
        ServiceEntity service = createService(provider.getId(), 60);
        LocalDate date = LocalDate.now().plusDays(1);
        ConfigureScheduleRequest request = new ConfigureScheduleRequest(
                List.of(date.getDayOfWeek().name()),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                List.of(new ConfigureScheduleRequest.ScheduleBreakRequest(
                        LocalTime.of(10, 0), LocalTime.of(11, 0))));

        mockMvc.perform(post("/api/proveedores/{providerId}/horarios", provider.getId())
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/servicios/{id}/disponibilidad", service.getId())
                        .param("fecha", date.toString())
                        .param("proveedorId", provider.getId().toString())
                        .param("duracionMinutos", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.data[1].horaInicio").value("11:00:00"))
                .andExpect(jsonPath("$.data[2].horaInicio").value("12:00:00"));
    }

    @Test
    void shouldRejectScheduleWhenStartTimeIsNotBeforeEndTime() throws Exception {
        User provider = createProvider();
        String authorization = loginAs(provider);
        ConfigureScheduleRequest request = new ConfigureScheduleRequest(
                List.of("MONDAY"), LocalTime.of(12, 0), LocalTime.of(12, 0), List.of());

        mockMvc.perform(post("/api/proveedores/{providerId}/horarios", provider.getId())
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private User createProvider() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "Proveedor de prueba",
                UUID.randomUUID() + "@example.test",
                "password123",
                UserRole.PROVIDER,
                true);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID userId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText());
        return userRepository.findById(userId).orElseThrow();
    }

    private String loginAs(User provider) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(provider.getEmail())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = response.path("data").path("token").asText();
        assertTrue(sessionTokenService.isTokenValid("Bearer " + token));
        return "Bearer " + token;
    }

    private ServiceEntity createService(UUID providerId, int durationMinutes) {
        ServiceEntity service = new ServiceEntity();
        service.setProveedorId(providerId);
        service.setNombre("Manicure");
        service.setCategoria("Belleza");
        service.setDescripcion("Manicure profesional");
        service.setPrecio(new BigDecimal("50000.00"));
        service.setDuracionMinutos(durationMinutes);
        service.setActivo(true);
        return serviceRepository.save(service);
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
