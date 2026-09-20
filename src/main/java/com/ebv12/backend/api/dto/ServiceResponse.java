package com.ebv12.backend.api.dto;

import com.ebv12.backend.domain.ServiceEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        UUID proveedorId,
        String nombre,
        String categoria,
        String descripcion,
        BigDecimal precio,
        Integer duracionMinutos,
        boolean activo
) {

    public static ServiceResponse from(ServiceEntity service) {
        return new ServiceResponse(
                service.getId(),
                service.getProveedorId(),
                service.getNombre(),
                service.getCategoria(),
                service.getDescripcion(),
                service.getPrecio(),
                service.getDuracionMinutos(),
                service.isActivo()
        );
    }
}
