package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.helpers.utils.MappingUtils;
import com.api.moviebooking.models.dtos.cinema.AddCinemaRequest;
import com.api.moviebooking.models.dtos.cinema.CinemaDataResponse;
import com.api.moviebooking.models.entities.Cinema;

@Mapper(componentModel = "spring", uses = MappingUtils.class)
public interface CinemaMapper {

    @Mapping(target = "id", ignore = true)
    Cinema toEntity(AddCinemaRequest request);

    CinemaDataResponse toDataResponse(Cinema cinema);
}
