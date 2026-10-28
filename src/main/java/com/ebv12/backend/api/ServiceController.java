package com.ebv12.backend.api;

import com.ebv12.backend.api.dto.ApiDataResponse;
import com.ebv12.backend.api.dto.AvailabilityResponse;
import com.ebv12.backend.api.dto.ConfigureScheduleRequest;
import com.ebv12.backend.api.dto.CreateServiceRequest;
import com.ebv12.backend.api.dto.PagedApiResponse;
import com.ebv12.backend.api.dto.ServiceResponse;
import com.ebv12.backend.domain.ServiceEntity;
import com.ebv12.backend.service.AuthenticationException;
import com.ebv12.backend.service.AvailabilityService;
import com.ebv12.backend.service.ProviderScheduleService;
import com.ebv12.backend.service.ServiceCatalogService;
import com.ebv12.backend.service.SessionTokenService;
import com.ebv12.backend.service.NoAvailabilityException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;
    private final AvailabilityService availabilityService;
    private final ProviderScheduleService providerScheduleService;
    private final SessionTokenService sessionTokenService;

    public ServiceController(ServiceCatalogService serviceCatalogService,
                             AvailabilityService availabilityService,
                             ProviderScheduleService providerScheduleService,
                             SessionTokenService sessionTokenService) {
        this.serviceCatalogService = serviceCatalogService;
        this.availabilityService = availabilityService;
        this.providerScheduleService = providerScheduleService;
        this.sessionTokenService = sessionTokenService;
    }

    @GetMapping("/v1/services")
    public ResponseEntity<PagedApiResponse<ServiceResponse>> findServices(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ServiceEntity> services = serviceCatalogService.findServices(
                query, categoria, precioMin, precioMax, pageable);
        List<ServiceResponse> data = services.getContent().stream()
                .map(ServiceResponse::from)
                .toList();
        return ResponseEntity.ok(new PagedApiResponse<>(
                data, services.getNumber(), services.getSize(), services.getTotalElements()));
    }

    @GetMapping("/v1/services/{serviceId}/availability")
    public ResponseEntity<ApiDataResponse<List<AvailabilityResponse>>> findV1Availability(
            @PathVariable UUID serviceId,
            @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(new ApiDataResponse<>(availabilityService
                .findAvailableSlots(serviceId, fecha).stream()
                .map(AvailabilityResponse::from)
                .toList()));
    }

    @PostMapping("/api/servicios")
    public ResponseEntity<ApiDataResponse<ServiceResponse>> createService(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateServiceRequest request) {
        UUID providerId = requireAuthenticatedUser(authorizationHeader);
        ServiceEntity service = serviceCatalogService.createService(request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiDataResponse<>(ServiceResponse.from(service)));
    }

    @PostMapping("/api/proveedores/{providerId}/horarios")
    public ResponseEntity<ApiDataResponse<Object>> configureSchedule(
            @PathVariable UUID providerId,
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ConfigureScheduleRequest request) {
        UUID authenticatedUserId = requireAuthenticatedUser(authorizationHeader);
        if (!authenticatedUserId.equals(providerId)) {
            throw new AuthenticationException("No puede configurar horarios de otro proveedor");
        }
        providerScheduleService.configure(providerId, request);
        return ResponseEntity.ok(new ApiDataResponse<>(null));
    }

    @GetMapping("/api/servicios/{serviceId}/disponibilidad")
    public ResponseEntity<ApiDataResponse<List<AvailabilityResponse>>> findAvailability(
            @PathVariable UUID serviceId,
            @RequestParam LocalDate fecha,
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) Integer duracionMinutos) {
        List<AvailabilityResponse> availability = availabilityService
                .findAvailableSlots(serviceId, fecha, proveedorId, duracionMinutos).stream()
                .map(AvailabilityResponse::from)
                .toList();
        if (availability.isEmpty()) {
            throw new NoAvailabilityException();
        }
        return ResponseEntity.ok(new ApiDataResponse<>(availability));
    }

    private UUID requireAuthenticatedUser(String authorizationHeader) {
        return sessionTokenService.resolveUserId(authorizationHeader)
                .orElseThrow(() -> new AuthenticationException("Debe iniciar sesión"));
    }
}