package com.api.moviebooking.models.dtos.seat;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSeatsRequest {

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Number of rows is required")
    @Min(value = 1, message = "Must have at least 1 row")
    private Integer rows;

    @NotNull(message = "Seats per row is required")
    @Min(value = 1, message = "Must have at least 1 seat per row")
    private Integer seatsPerRow;

    // List of row labels that should be VIP (e.g., ["E", "F"])
    private List<String> vipRows;

    // List of row labels that should be COUPLE (e.g., ["G"])
    private List<String> coupleRows;
}
