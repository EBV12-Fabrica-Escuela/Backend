package com.ebv12.backend.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "provider_schedules")
public class ProviderSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private String dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "provider_schedule_breaks", joinColumns = @JoinColumn(name = "schedule_id"))
    @OrderColumn(name = "break_order")
    private List<ScheduleBreak> breaks = new ArrayList<>();

    protected ProviderSchedule() {
    }

    public ProviderSchedule(UUID providerId, String dayOfWeek, LocalTime startTime, LocalTime endTime,
                           List<ScheduleBreak> breaks) {
        this.providerId = providerId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.breaks = new ArrayList<>(breaks);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public List<ScheduleBreak> getBreaks() {
        return List.copyOf(breaks);
    }

    @Embeddable
    public static class ScheduleBreak {
        @Column(name = "break_start", nullable = false)
        private LocalTime start;

        @Column(name = "break_end", nullable = false)
        private LocalTime end;

        protected ScheduleBreak() {
        }

        public ScheduleBreak(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        public LocalTime getStart() {
            return start;
        }

        public LocalTime getEnd() {
            return end;
        }
    }
}
