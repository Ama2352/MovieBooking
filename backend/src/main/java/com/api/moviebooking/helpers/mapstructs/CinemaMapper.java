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
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "snacks", ignore = true)
    Cinema toEntity(AddCinemaRequest request);

    @Mapping(target = "cinemaId", source = "id")
    CinemaDataResponse toDataResponse(Cinema cinema);
}
