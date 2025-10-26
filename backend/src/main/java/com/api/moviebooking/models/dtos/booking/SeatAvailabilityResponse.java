package com.api.moviebooking.models.dtos.booking;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAvailabilityResponse {

    private UUID showtimeId;
    private List<UUID> availableSeats;
    private List<UUID> lockedSeats;
    private List<UUID> bookedSeats;
    private String message;
}
