package com.api.moviebooking.models.dtos.payment;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String paymentId;
    private String bookingId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String method;
    private String bookingStatus;
    private String qrPayload;
}
