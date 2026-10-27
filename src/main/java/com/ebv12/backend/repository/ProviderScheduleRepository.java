package com.ebv12.backend.repository;

import com.ebv12.backend.domain.ProviderSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProviderScheduleRepository extends JpaRepository<ProviderSchedule, UUID> {

    List<ProviderSchedule> findByProviderId(UUID providerId);

    List<ProviderSchedule> findByProviderIdAndDayOfWeek(UUID providerId, String dayOfWeek);

    void deleteByProviderId(UUID providerId);
}
