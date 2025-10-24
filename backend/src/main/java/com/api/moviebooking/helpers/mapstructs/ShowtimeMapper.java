package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.helpers.utils.MappingUtils;
import com.api.moviebooking.models.dtos.showtime.AddShowtimeRequest;
import com.api.moviebooking.models.dtos.showtime.ShowtimeDataResponse;
import com.api.moviebooking.models.entities.Showtime;

@Mapper(componentModel = "spring", uses = {MappingUtils.class, MovieMapper.class, RoomMapper.class})
public interface ShowtimeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "movie", ignore = true)
    Showtime toEntity(AddShowtimeRequest request);

    ShowtimeDataResponse toDataResponse(Showtime showtime);
}
