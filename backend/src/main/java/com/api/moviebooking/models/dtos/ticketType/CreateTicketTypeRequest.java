package com.api.moviebooking.models.dtos.ticketType;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketTypeRequest {

    @NotBlank(message = "Ticket type ID is required")
    @Pattern(regexp = "^[a-z_]+$", message = "Ticket type ID must be lowercase with underscores only")
    private String ticketTypeId;

    @NotBlank(message = "Label is required")
    private String label;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Active status is required")
    private Boolean active;

    @NotNull(message = "Sort order is required")
    @Min(value = 0, message = "Sort order must be non-negative")
    private Integer sortOrder;
}
