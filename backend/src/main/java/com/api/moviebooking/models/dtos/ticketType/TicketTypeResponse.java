package com.api.moviebooking.models.dtos.ticketType;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTypeResponse {

    private UUID id;
    private String ticketTypeId;
    private String label;
    private BigDecimal price;
    private Boolean active;
    private Integer sortOrder;
}
