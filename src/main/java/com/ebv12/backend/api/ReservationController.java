package com.ebv12.backend.api;

import com.ebv12.backend.api.dto.ApiDataResponse;
import com.ebv12.backend.api.dto.CreateReservationRequest;
import com.ebv12.backend.api.dto.ReservationResponse;
import com.ebv12.backend.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiDataResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody CreateReservationRequest request
    ) {
        ReservationResponse response = ReservationResponse.from(reservationService.createReservation(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(response));
    }
}
