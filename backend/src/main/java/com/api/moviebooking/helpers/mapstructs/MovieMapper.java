package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.helpers.utils.MappingUtils;
import com.api.moviebooking.models.dtos.movie.AddMovieRequest;
import com.api.moviebooking.models.dtos.movie.MovieDataResponse;
import com.api.moviebooking.models.entities.Movie;

@Mapper(componentModel = "spring", uses = MappingUtils.class)
public interface MovieMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "showtimes", ignore = true)
    @Mapping(target = "status", source = "status")
    Movie toEntity(AddMovieRequest request);

    @Mapping(target = "status", source = "status")
    MovieDataResponse toDataResponse(Movie movie);
}
