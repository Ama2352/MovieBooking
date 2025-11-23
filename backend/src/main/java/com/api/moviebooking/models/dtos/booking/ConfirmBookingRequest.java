package com.api.moviebooking.models.dtos.booking;

import java.util.List;
import java.util.Map;
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

    @NotNull(message = "User ID is required")
    private UUID userId;

    private String promotionCode; // Optional promotion code for discount

    // Optional snack combo selection
    private List<SnackComboItem> snackCombos; // List of snack combo items

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnackComboItem {
        private UUID snackId;
        private Integer quantity;
    }
}
