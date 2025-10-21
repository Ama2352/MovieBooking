package com.api.moviebooking.models.dtos.cinema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateCinemaRequest {
    private String name;
    private String address;
    private String hotline;
}
