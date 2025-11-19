package com.api.moviebooking.models.dtos.ticketType;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketTypeRequest {

    private String label;

    private UUID priceBaseId;

    private Boolean active;

    @Min(value = 0, message = "Sort order must be non-negative")
    private Integer sortOrder;
}
