package com.api.moviebooking.models.dtos.cinema;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddCinemaRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @NotBlank
    private String hotline;
}
