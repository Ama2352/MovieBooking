package com.api.moviebooking.models.dtos.checkout;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutPaymentResponse {

    private UUID bookingId;
    private UUID paymentId;
    private String paymentMethod;
    private String redirectUrl;
    private String message;
}
