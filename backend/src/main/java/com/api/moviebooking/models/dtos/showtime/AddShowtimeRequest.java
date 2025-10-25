package com.api.moviebooking.models.dtos.showtime;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddShowtimeRequest {

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Movie ID is required")
    private UUID movieId;

    @NotBlank(message = "Format is required")
    private String format;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;
}
