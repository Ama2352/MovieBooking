package com.api.moviebooking.models.dtos.booking;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of discount calculation containing breakdown of all applied discounts.
 * Used by both price preview and booking confirmation to ensure consistent
 * discount logic.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountResult {

    /**
     * Total discount amount (membership + promotion combined)
     */
    private BigDecimal totalDiscount;

    /**
     * Human-readable reason for the discount (e.g., "Membership Gold (-10%),
     * Promotion: WINTER2024")
     */
    private String discountReason;

    /**
     * Breakdown: discount from membership tier
     */
    private BigDecimal membershipDiscount;

    /**
     * Breakdown: discount from promotion code
     */
    private BigDecimal promotionDiscount;

    /**
     * Creates an empty discount result with zero values
     */
    public static DiscountResult empty() {
        return DiscountResult.builder()
                .totalDiscount(BigDecimal.ZERO)
                .membershipDiscount(BigDecimal.ZERO)
                .promotionDiscount(BigDecimal.ZERO)
                .discountReason(null)
                .build();
    }
}
