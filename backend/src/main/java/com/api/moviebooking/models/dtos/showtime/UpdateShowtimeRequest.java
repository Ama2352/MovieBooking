package com.api.moviebooking.models.dtos.showtime;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateShowtimeRequest {

    private UUID roomId;

    private UUID movieId;

    private String format;

    private LocalDateTime startTime;
}
