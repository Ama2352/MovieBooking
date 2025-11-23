package com.api.moviebooking.models.dtos.booking;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockSeatsRequest {

    @NotNull(message = "Showtime ID is required")
    private UUID showtimeId;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(min = 1, max = 10, message = "You can book between 1 and 10 seats")
    @Valid
    private List<SeatWithTicketType> seats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatWithTicketType {

        @NotNull(message = "Showtime seat ID is required")
        private UUID showtimeSeatId;

        @NotNull(message = "Ticket type ID is required")
        private UUID ticketTypeId;
    }
}
