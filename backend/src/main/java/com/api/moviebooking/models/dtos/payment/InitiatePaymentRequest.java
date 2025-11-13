package com.api.moviebooking.models.dtos.payment;

import java.math.BigDecimal;
import java.util.UUID;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatePaymentRequest {

    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    @NotBlank(message = "Payment method is required")
    @EnumValidator(enumClass = PaymentMethod.class, message = "Invalid payment method")
    private String paymentMethod; // PAYPAL, VNPAY

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String ipAddress; // For VNPAY validation purpose
}
