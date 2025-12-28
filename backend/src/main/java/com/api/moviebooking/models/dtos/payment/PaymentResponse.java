package com.api.moviebooking.models.dtos.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.api.moviebooking.models.dtos.booking.BookingResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    // Payment Info
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal gatewayAmount;
    private String gatewayCurrency;
    private BigDecimal exchangeRate;
    private String status;
    private String method;

    // Booking Info (embedded for guest convenience)
    private UUID bookingId;
    private UUID showtimeId;
    private String movieTitle;
    private LocalDateTime showtimeStartTime;
    private String cinemaName;
    private String roomName;
    private List<BookingResponse.SeatDetail> seats;
    private BigDecimal totalPrice;
    private String discountReason;
    private BigDecimal discountValue;
    private BigDecimal finalPrice;
    private String bookingStatus;
    private LocalDateTime bookedAt;
    private String qrCode;
    private String qrPayload;
    private LocalDateTime paymentExpiresAt;
    private String posterUrl;
}
