package com.api.moviebooking.models.dtos.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.api.moviebooking.models.enums.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

        private UUID bookingId;
        private UUID showtimeId;
        private String movieTitle;
        private LocalDateTime showtimeStartTime;
        private String cinemaName;
        private String roomName;
        private List<SeatDetail> seats;
        private BigDecimal totalPrice;
        private BookingStatus status;
        private LocalDateTime bookedAt;
        private String qrCode;
        private String message;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class SeatDetail {
                private String rowLabel;
                private int seatNumber;
                private String seatType;
                private BigDecimal price;
        }
}
