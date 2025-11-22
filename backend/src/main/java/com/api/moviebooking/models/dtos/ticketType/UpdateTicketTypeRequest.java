package com.api.moviebooking.models.dtos.ticketType;

import java.math.BigDecimal;

import com.api.moviebooking.models.enums.ModifierType;

import jakarta.validation.constraints.DecimalMin;
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

    private ModifierType modifierType;

    @DecimalMin(value = "-100.0", message = "Modifier value must be greater than or equal to -100")
    private BigDecimal modifierValue;

    private Boolean active;

    @Min(value = 0, message = "Sort order must be non-negative")
    private Integer sortOrder;
}
