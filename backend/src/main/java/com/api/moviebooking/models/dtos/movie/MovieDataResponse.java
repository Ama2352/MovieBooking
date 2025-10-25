package com.api.moviebooking.models.dtos.movie;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MovieDataResponse {

    private UUID id;
    private String title;
    private String genre;
    private String description;
    private Integer duration;
    private Integer minimumAge;
    private String director;
    private String actors;
    private String posterUrl;
    private String posterCloudinaryId;
    private String trailerUrl;
    private String status;
    private String language;
}
