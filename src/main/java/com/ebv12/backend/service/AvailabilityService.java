package com.ebv12.backend.service;

import com.ebv12.backend.domain.AvailabilitySlot;
import com.ebv12.backend.domain.ProviderSchedule;
import com.ebv12.backend.domain.ServiceEntity;
import com.ebv12.backend.repository.AvailabilitySlotRepository;
import com.ebv12.backend.repository.ProviderScheduleRepository;
import com.ebv12.backend.repository.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

@Service
public class AvailabilityService {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final ProviderScheduleRepository providerScheduleRepository;
    private final ServiceRepository serviceRepository;

    public AvailabilityService(AvailabilitySlotRepository availabilitySlotRepository,
                               ProviderScheduleRepository providerScheduleRepository,
                               ServiceRepository serviceRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.providerScheduleRepository = providerScheduleRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<AvailabilitySlot> findAvailableSlots(UUID serviceId, LocalDate fecha) {
        return findAvailableSlots(serviceId, fecha, null, null);
    }

    @Transactional
    public List<AvailabilitySlot> findAvailableSlots(UUID serviceId, LocalDate fecha, UUID providerId,
                                                      Integer requestedDurationMinutes) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("El servicio solicitado no existe"));
        if (!service.isActivo()
                || (providerId != null && !providerId.equals(service.getProveedorId()))
                || (requestedDurationMinutes != null && requestedDurationMinutes < service.getDuracionMinutos())
                || fecha.isBefore(LocalDate.now())) {
            return List.of();
        }

        int durationMinutes = requestedDurationMinutes == null
                ? service.getDuracionMinutos()
                : requestedDurationMinutes;
        List<AvailabilitySlot> available = new ArrayList<>(
                availabilitySlotRepository.findByServicioIdAndFechaAndDisponibleTrueOrderByHoraInicio(serviceId, fecha)
                        .stream()
                        .filter(slot -> !slot.getHoraInicio().plusMinutes(durationMinutes).isAfter(slot.getHoraFin()))
                        .toList());

        List<ProviderSchedule> schedules = providerScheduleRepository
                .findByProviderIdAndDayOfWeek(service.getProveedorId(), fecha.getDayOfWeek().name());
        for (ProviderSchedule schedule : schedules) {
            LocalTime start = schedule.getStartTime();
            while (!start.plusMinutes(durationMinutes).isAfter(schedule.getEndTime())) {
                LocalTime end = start.plusMinutes(durationMinutes);
                LocalTime slotStart = start;
                LocalTime slotEnd = end;
                boolean overlapsBreak = schedule.getBreaks().stream()
                        .anyMatch(scheduleBreak -> slotStart.isBefore(scheduleBreak.getEnd())
                                && slotEnd.isAfter(scheduleBreak.getStart()));
                if (!overlapsBreak) {
                    AvailabilitySlot slot = findOrCreateSlot(service, fecha, slotStart, slotEnd);
                    if (slot.isDisponible() && available.stream().noneMatch(existing ->
                            existing.getHoraInicio().equals(slotStart))) {
                        available.add(slot);
                    }
                }
                start = end;
            }
        }

        return available.stream().sorted(Comparator.comparing(AvailabilitySlot::getHoraInicio)).toList();
    }

    private AvailabilitySlot findOrCreateSlot(ServiceEntity service, LocalDate date, LocalTime start, LocalTime end) {
        List<AvailabilitySlot> existing = availabilitySlotRepository
                .findByServicioIdAndFechaOrderByHoraInicio(service.getId(), date);
        for (AvailabilitySlot slot : existing) {
            if (slot.getHoraInicio().equals(start) && slot.getHoraFin().equals(end)) {
                return slot;
            }
        }

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setServicio(service);
        slot.setFecha(date);
        slot.setHoraInicio(start);
        slot.setHoraFin(end);
        slot.setDisponible(true);
        return availabilitySlotRepository.save(slot);
    }
}
