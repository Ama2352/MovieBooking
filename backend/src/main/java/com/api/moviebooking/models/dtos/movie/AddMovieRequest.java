package com.api.moviebooking.models.dtos.movie;

import com.api.moviebooking.helpers.annotations.EnumValidator;
import com.api.moviebooking.models.enums.MovieStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddMovieRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Genre is required")
    private String genre;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;

    @NotNull(message = "Minimum age is required")
    @Min(value = 0, message = "Minimum age must be at least 0")
    private Integer minimumAge;

    @NotBlank(message = "Director is required")
    private String director;

    @NotBlank(message = "Actors are required")
    private String actors;

    private String posterUrl;

    private String posterCloudinaryId;

    @NotBlank(message = "Trailer URL is required")
    private String trailerUrl;

    @NotNull(message = "Status is required")
    @Schema(example = "SHOWING")
    @EnumValidator(enumClass = MovieStatus.class, message = "Status must be SHOWING,UPCOMING")
    private String status;

    @NotBlank(message = "Language is required")
    private String language;
}
