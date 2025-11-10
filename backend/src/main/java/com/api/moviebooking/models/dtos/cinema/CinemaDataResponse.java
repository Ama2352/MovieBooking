package com.api.moviebooking.models.dtos.cinema;

import lombok.Data;

@Data
public class CinemaDataResponse {
    private String cinemaId;
    private String name;
    private String address;
    private String hotline;
}
