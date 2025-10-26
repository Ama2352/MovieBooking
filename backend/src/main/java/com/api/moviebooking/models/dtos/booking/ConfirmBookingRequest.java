package com.api.moviebooking.models.dtos.booking;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBookingRequest {

    @NotNull(message = "Lock ID is required")
    private UUID lockId;

    private String paymentMethod; // For future payment integration
}
