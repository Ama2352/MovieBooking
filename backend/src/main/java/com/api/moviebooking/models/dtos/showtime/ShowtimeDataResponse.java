package com.api.moviebooking.models.dtos.showtime;

import java.time.LocalDateTime;
import java.util.UUID;

import com.api.moviebooking.models.dtos.movie.MovieDataResponse;
import com.api.moviebooking.models.dtos.room.RoomDataResponse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShowtimeDataResponse {

    private UUID id;
    private RoomDataResponse room;
    private MovieDataResponse movie;
    private String format;
    private LocalDateTime startTime;
}
