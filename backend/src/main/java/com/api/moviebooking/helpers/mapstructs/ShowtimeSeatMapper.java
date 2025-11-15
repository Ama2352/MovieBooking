package com.api.moviebooking.helpers.mapstructs;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.moviebooking.models.dtos.showtimeSeat.ShowtimeSeatDataResponse;
import com.api.moviebooking.models.entities.ShowtimeSeat;

@Mapper(componentModel = "spring")
public interface ShowtimeSeatMapper {

    @Mapping(target = "showtimeId", source = "showtime.id")
    @Mapping(target = "seatId", source = "seat.id")
    @Mapping(target = "rowLabel", source = "seat.rowLabel")
    @Mapping(target = "seatNumber", source = "seat.seatNumber")
    @Mapping(target = "seatType", source = "seat.seatType")
    ShowtimeSeatDataResponse toDataResponse(ShowtimeSeat showtimeSeat);
}
