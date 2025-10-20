package com.api.moviebooking.models.dtos.cinema;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AddCinemaRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @NotBlank
    private String hotline;
}
