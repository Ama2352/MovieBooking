package com.api.moviebooking.models.dtos.payment;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConfirmPaymentRequest {

    @NotBlank(message = "Payment method is required")
    @EnumValidator(enumClass = PaymentMethod.class, message = "Invalid payment method")
    private String paymentMethod;

    /**
     * This can be PayPal order ID (a temporary ID managed by paypal, paypal return
     * transaction ID after capture)
     * or VNPay transaction reference (managed by us, using payment ID as
     * transaction reference)
     */
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
}
