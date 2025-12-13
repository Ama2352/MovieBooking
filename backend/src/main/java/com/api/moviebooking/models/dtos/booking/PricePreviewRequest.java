package com.api.moviebooking.models.dtos.booking;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for price preview endpoint.
 * Uses lockId to get seat/ticket information from an existing SeatLock,
 * ensuring consistency with the confirm booking flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricePreviewRequest {

    @NotNull(message = "Lock ID is required")
    private UUID lockId;

    private String promotionCode;

    private List<SnackItem> snacks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SnackItem {
        private UUID snackId;
        private Integer quantity;
    }
}
