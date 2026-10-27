package com.ebv12.backend.service;

import com.ebv12.backend.api.dto.CreateServiceRequest;
import com.ebv12.backend.domain.ServiceEntity;
import com.ebv12.backend.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;

    public ServiceCatalogService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public ServiceEntity createService(CreateServiceRequest request, java.util.UUID providerId) {
        ServiceEntity service = new ServiceEntity();
        service.setNombre(request.name());
        service.setDescripcion(request.description());
        service.setCategoria(request.category());
        service.setPrecio(request.price());
        service.setDuracionMinutos(request.durationMinutes());
        service.setProveedorId(providerId);
        service.setActivo(true);
        return serviceRepository.save(service);
    }

    public Page<ServiceEntity> findServices(String query, String categoria, BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {
        Specification<ServiceEntity> spec = (root, cq, cb) -> cb.isTrue(root.get("activo"));

        if (StringUtils.hasText(query)) {
            String like = "%" + query.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("nombre")), like),
                    cb.like(cb.lower(root.get("descripcion")), like)
            ));
        }

        if (StringUtils.hasText(categoria)) {
            spec = spec.and((root, cq, cb) -> cb.equal(cb.lower(root.get("categoria")), categoria.toLowerCase()));
        }

        if (precioMin != null) {
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("precio"), precioMin));
        }

        if (precioMax != null) {
            spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("precio"), precioMax));
        }

        return serviceRepository.findAll(spec, pageable);
    }
}
