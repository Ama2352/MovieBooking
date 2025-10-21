package com.api.moviebooking.models.dtos.cinema;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddCinemaRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @NotBlank
    private String hotline;
}
