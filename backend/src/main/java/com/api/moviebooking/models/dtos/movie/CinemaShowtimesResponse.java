package com.api.moviebooking.models.dtos.movie;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaShowtimesResponse {
    
    private UUID cinemaId;
    private String cinemaName;
    private String address;
    private List<ShowtimeInfo> showtimes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShowtimeInfo {
        private UUID showtimeId;
        private String startTime; // ISO format: 2025-11-10T19:30:00
        private String format; // 2D, 3D, IMAX, 4DX
        private String roomName;
    }
}
