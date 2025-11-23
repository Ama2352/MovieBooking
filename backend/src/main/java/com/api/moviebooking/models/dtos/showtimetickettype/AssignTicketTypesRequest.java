package com.api.moviebooking.models.dtos.showtimetickettype;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignTicketTypesRequest {

    @NotEmpty(message = "Ticket type IDs cannot be empty")
    private List<UUID> ticketTypeIds;
}
