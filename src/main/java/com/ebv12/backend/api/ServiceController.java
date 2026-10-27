package com.ebv12.backend.api;

import com.ebv12.backend.api.dto.ApiDataResponse;
import com.ebv12.backend.api.dto.AvailabilityResponse;
import com.ebv12.backend.api.dto.ConfigureScheduleRequest;
import com.ebv12.backend.api.dto.CreateServiceRequest;
import com.ebv12.backend.api.dto.PagedApiResponse;
import com.ebv12.backend.api.dto.ServiceResponse;
import com.ebv12.backend.domain.User;
import com.ebv12.backend.domain.UserRole;
import com.ebv12.backend.service.AvailabilityService;
import com.ebv12.backend.service.AuthenticationException;
import com.ebv12.backend.service.NoAvailabilityException;
import com.ebv12.backend.service.ProviderScheduleService;
import com.ebv12.backend.service.ServiceCatalogService;
import com.ebv12.backend.service.SessionTokenService;
import com.ebv12.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api", "/v1"})
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;
    private final AvailabilityService availabilityService;
    private final ProviderScheduleService providerScheduleService;
    private final SessionTokenService sessionTokenService;
    private final UserRepository userRepository;

    public ServiceController(ServiceCatalogService serviceCatalogService,
                             AvailabilityService availabilityService,
                             ProviderScheduleService providerScheduleService,
                             SessionTokenService sessionTokenService,
                             UserRepository userRepository) {
        this.serviceCatalogService = serviceCatalogService;
        this.availabilityService = availabilityService;
        this.providerScheduleService = providerScheduleService;
        this.sessionTokenService = sessionTokenService;
        this.userRepository = userRepository;
    }

    @PostMapping({"/servicios", "/services"})
    public ResponseEntity<ApiDataResponse<ServiceResponse>> createService(
            @Valid @RequestBody CreateServiceRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        User provider = requireProvider(authorizationHeader);
        ServiceResponse response = ServiceResponse.from(serviceCatalogService.createService(request, provider.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(response));
    }

    @PostMapping("/proveedores/{providerId}/horarios")
    public ResponseEntity<ApiDataResponse<String>> configureSchedule(
            @PathVariable UUID providerId,
            @Valid @RequestBody ConfigureScheduleRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        User provider = requireProvider(authorizationHeader);
        if (!provider.getId().equals(providerId)) {
            throw new AuthenticationException("No está autorizado para configurar este horario");
        }
        providerScheduleService.configure(providerId, request);
        return ResponseEntity.ok(new ApiDataResponse<>("Horario configurado correctamente"));
    }

    @GetMapping({"/servicios", "/services"})
    public ResponseEntity<PagedApiResponse<ServiceResponse>> getServices(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoria", required = false) String categoria,
            @RequestParam(name = "precioMin", required = false) BigDecimal precioMin,
            @RequestParam(name = "precioMax", required = false) BigDecimal precioMax,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Page<ServiceResponse> result = serviceCatalogService
                .findServices(q, categoria, precioMin, precioMax, PageRequest.of(page, size))
                .map(ServiceResponse::from);

        return ResponseEntity.ok(new PagedApiResponse<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping({"/servicios/{id}/disponibilidad", "/services/{id}/availability"})
    public ResponseEntity<ApiDataResponse<List<AvailabilityResponse>>> getAvailability(
            @PathVariable("id") UUID serviceId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "proveedorId", required = false) UUID providerId,
            @RequestParam(name = "duracionMinutos", required = false) Integer durationMinutes
    ) {
        List<AvailabilityResponse> data = availabilityService
                .findAvailableSlots(serviceId, fecha, providerId, durationMinutes).stream()
                .map(AvailabilityResponse::from)
                .toList();
        if (data.isEmpty()) {
            throw new NoAvailabilityException();
        }
        return ResponseEntity.ok(new ApiDataResponse<>(data));
    }

    private User requireProvider(String authorizationHeader) {
        UUID userId = sessionTokenService.resolveUserId(authorizationHeader)
                .orElseThrow(() -> new AuthenticationException("Debe iniciar sesión como proveedor"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Debe iniciar sesión como proveedor"));
        if (user.getRole() != UserRole.PROVIDER || !user.isEnabled()) {
            throw new AuthenticationException("Debe iniciar sesión como proveedor");
        }
        return user;
    }
}
