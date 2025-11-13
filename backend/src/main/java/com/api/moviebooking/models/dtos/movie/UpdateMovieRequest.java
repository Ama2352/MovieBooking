package com.api.moviebooking.models.dtos.movie;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.MovieStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateMovieRequest {

    private String title;

    private String genre;

    private String description;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;

    @Min(value = 0, message = "Minimum age must be at least 0")
    private Integer minimumAge;

    private String director;

    private String actors;

    private String posterUrl;

    private String posterCloudinaryId;

    private String trailerUrl;

    @Schema(example = "SHOWING")
    @EnumValidator(enumClass = MovieStatus.class, message = "Status must be SHOWING,UPCOMING")
    private String status;

    private String language;
}
