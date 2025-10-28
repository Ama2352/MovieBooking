package com.api.moviebooking.models.dtos.cinema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCinemaRequest {
    private String name;
    private String address;
    private String hotline;
}
