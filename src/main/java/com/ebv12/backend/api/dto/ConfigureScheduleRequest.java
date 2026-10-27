package com.ebv12.backend.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record ConfigureScheduleRequest(
        @NotNull(message = "es obligatorio") List<String> workingDays,
        @NotNull(message = "es obligatorio") LocalTime startTime,
        @NotNull(message = "es obligatorio") LocalTime endTime,
        List<ScheduleBreakRequest> breaks
) {
    public record ScheduleBreakRequest(
            @NotNull(message = "es obligatorio") LocalTime start,
            @NotNull(message = "es obligatorio") LocalTime end
    ) {
    }
}
