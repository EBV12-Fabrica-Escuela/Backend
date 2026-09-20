package com.ebv12.backend.service;

import com.ebv12.backend.domain.AvailabilitySlot;
import com.ebv12.backend.repository.AvailabilitySlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AvailabilityService {

    private final AvailabilitySlotRepository availabilitySlotRepository;

    public AvailabilityService(AvailabilitySlotRepository availabilitySlotRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
    }

    public List<AvailabilitySlot> findAvailableSlots(UUID serviceId, LocalDate fecha) {
        return availabilitySlotRepository.findByServicioIdAndFechaAndDisponibleTrueOrderByHoraInicio(serviceId, fecha);
    }
}
