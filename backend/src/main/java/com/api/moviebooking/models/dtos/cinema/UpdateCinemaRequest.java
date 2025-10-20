package com.api.moviebooking.models.dtos.cinema;

import lombok.Getter;

@Getter
public class UpdateCinemaRequest {
    private String name;
    private String address;
    private String hotline;
}
