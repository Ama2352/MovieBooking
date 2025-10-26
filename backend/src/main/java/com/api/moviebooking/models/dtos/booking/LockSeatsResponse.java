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
        private String lockKey;
        private UUID showtimeId;
        private List<SeatInfo> lockedSeats;
        private BigDecimal totalPrice;
        private LocalDateTime expiresAt;
        private long remainingSeconds;
        private String message;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class SeatInfo {
                private UUID seatId;
                private String rowLabel;
                private int seatNumber;
                private String seatType;
                private BigDecimal price;
        }
}
