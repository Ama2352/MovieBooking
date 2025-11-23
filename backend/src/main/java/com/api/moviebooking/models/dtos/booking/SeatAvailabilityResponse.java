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

    /**
     * Optional: If the requesting session has active locks, include them
     */
    private SessionLockInfo sessionLockInfo;
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionLockInfo {
        private UUID lockId;
        private List<UUID> myLockedSeats; // Seats locked by this session
        private Integer remainingSeconds;
    }
}
