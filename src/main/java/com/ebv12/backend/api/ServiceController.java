package com.ebv12.backend.api;

import com.ebv12.backend.api.dto.ApiDataResponse;
import com.ebv12.backend.api.dto.AvailabilityResponse;
import com.ebv12.backend.api.dto.PagedApiResponse;
import com.ebv12.backend.api.dto.ServiceResponse;
import com.ebv12.backend.service.AvailabilityService;
import com.ebv12.backend.service.ServiceCatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;
    private final AvailabilityService availabilityService;

    public ServiceController(ServiceCatalogService serviceCatalogService,
                             AvailabilityService availabilityService) {
        this.serviceCatalogService = serviceCatalogService;
        this.availabilityService = availabilityService;
    }

    @GetMapping("/services")
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

    @GetMapping("/services/{id}/availability")
    public ResponseEntity<ApiDataResponse<List<AvailabilityResponse>>> getAvailability(
            @PathVariable("id") UUID serviceId,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        List<AvailabilityResponse> data = availabilityService.findAvailableSlots(serviceId, fecha).stream()
                .map(AvailabilityResponse::from)
                .toList();
        return ResponseEntity.ok(new ApiDataResponse<>(data));
    }
}
