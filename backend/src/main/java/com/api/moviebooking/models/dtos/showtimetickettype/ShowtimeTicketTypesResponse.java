package com.api.moviebooking.models.dtos.showtimetickettype;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeTicketTypesResponse {

    private UUID showtimeId;
    private List<UUID> assignedTicketTypeIds;
}
