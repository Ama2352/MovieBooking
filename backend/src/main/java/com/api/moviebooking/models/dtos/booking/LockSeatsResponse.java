package com.api.moviebooking.models.dtos.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class LockSeatsResponse {

    private UUID lockId;
    private UUID showtimeId;
    private String lockOwnerId; // userId or sessionId
    private String lockOwnerType; // USER or GUEST_SESSION
    private List<LockedSeatInfo> lockedSeats;
    private BigDecimal totalPrice;
    private LocalDateTime expiresAt;
    private Integer lockDurationMinutes;
    private String message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LockedSeatInfo {
        private UUID showtimeSeatId;
        private String seatRow;
        private Integer seatNumber;
        private String seatType;
        private UUID ticketTypeId;
        private String ticketTypeLabel;
        private BigDecimal price;
    }
}
