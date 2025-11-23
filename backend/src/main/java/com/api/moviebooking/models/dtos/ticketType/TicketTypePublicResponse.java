package com.api.moviebooking.models.dtos.ticketType;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Public response DTO for ticket types (guest/user view)
 * Only includes essential fields for ticket selection
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTypePublicResponse {

    private UUID ticketTypeId;
    private String code; // adult, student, senior, member, double
    private String label; // Display label: NGƯỜI LỚN, HSSV/U22-GV, etc.
    private BigDecimal price; // Calculated price (for guest view with showtime context)
}
