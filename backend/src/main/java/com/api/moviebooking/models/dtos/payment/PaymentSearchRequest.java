package com.api.moviebooking.models.dtos.payment;

import java.time.LocalDateTime;
import java.util.UUID;

import com.api.moviebooking.models.enums.PaymentMethod;
import com.api.moviebooking.models.enums.PaymentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentSearchRequest {
    private UUID bookingId;
    private UUID userId;
    private String transactionId;
    private PaymentStatus status;
    private PaymentMethod method;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
