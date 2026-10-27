package com.ebv12.backend.service;

import com.ebv12.backend.api.dto.ConfigureScheduleRequest;
import com.ebv12.backend.domain.ProviderSchedule;
import com.ebv12.backend.repository.ProviderScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProviderScheduleService {

    private final ProviderScheduleRepository providerScheduleRepository;

    public ProviderScheduleService(ProviderScheduleRepository providerScheduleRepository) {
        this.providerScheduleRepository = providerScheduleRepository;
    }

    @Transactional
    public List<ProviderSchedule> configure(UUID providerId, ConfigureScheduleRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }

        List<ProviderSchedule.ScheduleBreak> breaks = new ArrayList<>();
        if (request.breaks() != null) {
            request.breaks().forEach(scheduleBreak -> {
                if (!scheduleBreak.start().isBefore(scheduleBreak.end())
                        || scheduleBreak.start().isBefore(request.startTime())
                        || scheduleBreak.end().isAfter(request.endTime())) {
                    throw new IllegalArgumentException("El descanso debe estar dentro del horario de atención");
                }
                breaks.add(new ProviderSchedule.ScheduleBreak(scheduleBreak.start(), scheduleBreak.end()));
            });
        }

        providerScheduleRepository.deleteByProviderId(providerId);
        List<ProviderSchedule> schedules = request.workingDays().stream()
                .map(day -> normalizeDay(day))
                .distinct()
                .map(day -> new ProviderSchedule(providerId, day, request.startTime(), request.endTime(), breaks))
                .toList();
        return providerScheduleRepository.saveAll(schedules);
    }

    private String normalizeDay(String day) {
        try {
            return DayOfWeek.valueOf(day.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Día de atención inválido: " + day);
        }
    }
}
