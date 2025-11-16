package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.helpers.utils.MappingUtils;
import com.api.moviebooking.models.dtos.snack.AddSnackRequest;
import com.api.moviebooking.models.dtos.snack.SnackDataResponse;
import com.api.moviebooking.models.entities.Snack;

@Mapper(componentModel = "spring", uses = MappingUtils.class)
public interface SnackMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cinema", ignore = true)
    @Mapping(target = "bookingSnacks", ignore = true)
    Snack toEntity(AddSnackRequest request);

    @Mapping(target = "cinemaId", source = "cinema.id")
    @Mapping(target = "snackId", source = "id")
    SnackDataResponse toDataResponse(Snack snack);
}
