package com.api.moviebooking.models.dtos.checkout;

import java.util.List;
import java.util.UUID;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.dtos.booking.ConfirmBookingRequest.SnackComboItem;
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
public class CheckoutPaymentRequest {

    @NotNull(message = "Lock ID is required")
    private UUID lockId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    // Optional promotion code for discount
    private String promotionCode;

    // Optional snack combo selection
    private List<SnackComboItem> snackCombos; // List of snack combo items

    @NotBlank(message = "Payment method is required")
    @EnumValidator(enumClass = PaymentMethod.class, message = "Invalid payment method")
    private String paymentMethod;

}
